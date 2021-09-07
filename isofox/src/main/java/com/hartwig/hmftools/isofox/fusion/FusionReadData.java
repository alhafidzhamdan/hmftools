package com.hartwig.hmftools.isofox.fusion;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import static com.hartwig.hmftools.common.fusion.FusionCommon.FS_DOWN;
import static com.hartwig.hmftools.common.fusion.FusionCommon.FS_PAIR;
import static com.hartwig.hmftools.common.fusion.FusionCommon.FS_UP;
import static com.hartwig.hmftools.common.utils.sv.SvCommonUtils.NEG_ORIENT;
import static com.hartwig.hmftools.common.utils.sv.SvCommonUtils.POS_ORIENT;
import static com.hartwig.hmftools.common.fusion.FusionCommon.switchStream;
import static com.hartwig.hmftools.common.utils.Strings.appendStr;
import static com.hartwig.hmftools.common.utils.Strings.appendStrList;
import static com.hartwig.hmftools.common.utils.Strings.reverseString;
import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.SE_END;
import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.SE_START;
import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.switchIndex;
import static com.hartwig.hmftools.common.utils.sv.BaseRegion.positionWithin;
import static com.hartwig.hmftools.isofox.IsofoxConfig.ISF_LOGGER;
import static com.hartwig.hmftools.isofox.common.RegionMatchType.INTRON;
import static com.hartwig.hmftools.isofox.common.RnaUtils.deriveCommonRegions;
import static com.hartwig.hmftools.isofox.common.RnaUtils.impliedSvType;
import static com.hartwig.hmftools.isofox.common.TransExonRef.hasTranscriptExonMatch;
import static com.hartwig.hmftools.isofox.fusion.FusionConstants.JUNCTION_BASE_LENGTH;
import static com.hartwig.hmftools.isofox.fusion.FusionConstants.REALIGN_MAX_SOFT_CLIP_BASE_LENGTH;
import static com.hartwig.hmftools.isofox.fusion.FusionConstants.REALIGN_MIN_SOFT_CLIP_BASE_LENGTH;
import static com.hartwig.hmftools.isofox.fusion.FusionConstants.SOFT_CLIP_JUNC_BUFFER;
import static com.hartwig.hmftools.isofox.fusion.FusionFragmentType.MATCHED_JUNCTION;
import static com.hartwig.hmftools.isofox.fusion.FusionFragmentType.DISCORDANT;
import static com.hartwig.hmftools.isofox.fusion.FusionFragmentType.REALIGNED;
import static com.hartwig.hmftools.isofox.results.ResultsWriter.DELIMITER;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.gene.GeneData;

import com.hartwig.hmftools.common.genome.refgenome.RefGenomeInterface;
import com.hartwig.hmftools.common.sv.StructuralVariantType;
import com.hartwig.hmftools.isofox.common.ReadRecord;
import com.hartwig.hmftools.isofox.common.TransExonRef;

public class FusionReadData
{
    private final int mId;
    private final String mLocationId;
    private final FusionFragment mFragment; // the one which establishes this fusion and whose junction data is used

    private final Map<FusionFragmentType,List<FusionFragment>> mFragments;
    private final Map<FusionFragmentType,Integer> mFragmentCounts;

    private boolean mIncompleteData;

    private final Set<Integer> mRelatedProximateFusions;
    private final Set<Integer> mRelatedSplicedFusions;

    // not stored by stream
    private final String[] mChromosomes;
    private final int[] mGeneCollections;
    private final int[] mJunctionPositions;
    private final byte[] mJunctionOrientations;
    private final String[] mJunctionBases; // the 10 bases leading up to the junction
    private final String[] mAdjacentJunctionBases; // the 10 bases continuing on from the junction
    private final String[] mJunctionSpliceBases; // the 2 donor/acceptor bases

    private final List<TransExonRef>[] mTransExonRefs;
    private final int[] mReadDepth;

    // the following data is stored by stream, not start/end
    private final GeneData[] mFusionGenes; // up and downstream genes if identified
    private final int[] mStreamIndices; // mapping of up & down stream to position data which is in SV terms
    private final int[] mMaxSplitLengths;
    private final int[] mJunctionHomology;

    public FusionReadData(int id, final FusionFragment fragment)
    {
        mId = id;
        mFragment = fragment;

        mChromosomes = new String[] { fragment.chromosomes()[SE_START], fragment.chromosomes()[SE_END] };
        mGeneCollections = new int[] { fragment.geneCollections()[SE_START], fragment.geneCollections()[SE_END] };
        mJunctionPositions = new int[] { fragment.junctionPositions()[SE_START], fragment.junctionPositions()[SE_END] };
        mJunctionOrientations = new byte[]{ fragment.junctionOrientations()[SE_START], fragment.junctionOrientations()[SE_END] };

        mJunctionBases = new String[] {"", ""};
        mJunctionSpliceBases = new String[] {"", ""};
        mAdjacentJunctionBases = new String[] {"", ""};

        mFragments = Maps.newHashMap();
        mFragmentCounts = Maps.newHashMap();

        mLocationId = fragment.locationPair();

        mRelatedSplicedFusions = Sets.newHashSet();
        mRelatedProximateFusions = Sets.newHashSet();

        mFusionGenes = new GeneData[] { null, null};
        mStreamIndices = new int[] { SE_START, SE_END };

        mReadDepth = new int[] {0, 0};

        // extract depth from reads
        for(int se = SE_START; se <= SE_END; ++se)
        {
            for(ReadRecord read : mFragment.reads())
            {
                if(read.getJunctionDepth().containsKey(mJunctionPositions[se]))
                {
                    mReadDepth[se] = read.getJunctionDepth().get(mJunctionPositions[se]);
                    break;
                }
            }
        }

        mMaxSplitLengths = new int[] {0, 0};
        mJunctionHomology = new int[] {0, 0};

        mIncompleteData = false;

        mTransExonRefs = new List[FS_PAIR];
        mTransExonRefs[SE_START] = Lists.newArrayList();
        mTransExonRefs[SE_END] = Lists.newArrayList();

        addFusionFragment(fragment, true);
    }

    public int id() { return mId; }
    public String locationId() { return mLocationId; }
    public final String[] chromosomes() { return mChromosomes; }
    public final int[] geneCollections() { return mGeneCollections; }
    public final int[] junctionPositions() { return mJunctionPositions; }
    public final byte[] junctionOrientations() { return mJunctionOrientations; }
    public final String[] junctionBases() { return mJunctionBases; }
    public final String[] adjacentJunctionBases() { return mAdjacentJunctionBases; }
    public final String[] junctionSpliceBases() { return mJunctionSpliceBases; }
    public final int[] junctionHomology() { return mJunctionHomology; }

    public boolean hasIncompleteData() { return mIncompleteData; }
    public void setIncompleteData() { mIncompleteData = true; }

    public List<TransExonRef> getTransExonRefsByPos(int se) { return mTransExonRefs[se]; }

    public List<TransExonRef> getTransExonRefsByStream(int fs)
    {
        if(hasViableGenes())
            return mTransExonRefs[mStreamIndices[fs]];

        return mTransExonRefs[fs];
    }

    public final Map<FusionFragmentType,List<FusionFragment>> getFragments() { return mFragments; }

    public final List<FusionFragment> getFragments(FusionFragmentType type)
    {
        return mFragments.containsKey(type) ? mFragments.get(type) : Lists.newArrayList();
    }

    public int getFragmentTypeCount(FusionFragmentType type)
    {
        return mFragmentCounts.containsKey(type) ? mFragmentCounts.get(type) : 0;
    }

    public void addFragmentTypeCount(FusionFragmentType type, int count)
    {
        Integer existingCount = mFragmentCounts.get(type);
        if(existingCount == null)
            mFragmentCounts.put(type, count);
        else
            mFragmentCounts.put(type, existingCount + count);
    }

    public void addFusionFragment(final FusionFragment fragment, boolean cacheFragment)
    {
        try
        {
            if(fragment.type() == MATCHED_JUNCTION)
                updateMaxSplitMappedLength(fragment);
        }
        catch(Exception e)
        {
            ISF_LOGGER.error("{}", e.toString());
        }

        addFragmentTypeCount(fragment.type(), 1);

        if(cacheFragment)
        {
            fragment.assignedFusions().add(this);

            List<FusionFragment> fragments = mFragments.get(fragment.type());

            if (fragments == null)
                mFragments.put(fragment.type(), Lists.newArrayList(fragment));
            else
                fragments.add(fragment);
        }
    }

    public boolean isKnownSpliced() { return getInitialFragment().isSpliced(); }
    public boolean isUnspliced() { return getInitialFragment().isUnspliced() && getInitialFragment().type() == MATCHED_JUNCTION; }

    public boolean hasViableGenes() { return mFusionGenes[FS_UP] != null && mFusionGenes[FS_DOWN] != null; }

    public void setJunctionBases(final RefGenomeInterface refGenome)
    {
        if(mFragment.type() != MATCHED_JUNCTION)
            return;

        try
        {
            for (int se = SE_START; se <= SE_END; ++se)
            {
                int junctionBase = mJunctionPositions[se];

                if (junctionOrientations()[se] == POS_ORIENT)
                {
                    String junctionBases = refGenome.getBaseString(
                            mChromosomes[se], junctionBase - JUNCTION_BASE_LENGTH + 1, junctionBase + JUNCTION_BASE_LENGTH);

                    mJunctionBases[se] = junctionBases.substring(0, JUNCTION_BASE_LENGTH);
                    mAdjacentJunctionBases[se] = junctionBases.substring(JUNCTION_BASE_LENGTH);
                    mJunctionSpliceBases[se] = junctionBases.substring(JUNCTION_BASE_LENGTH, JUNCTION_BASE_LENGTH + 2);
                }
                else
                {
                    String junctionBases = refGenome.getBaseString(
                            mChromosomes[se], junctionBase - JUNCTION_BASE_LENGTH, junctionBase + JUNCTION_BASE_LENGTH - 1);

                    mJunctionBases[se] = junctionBases.substring(JUNCTION_BASE_LENGTH);
                    mJunctionSpliceBases[se] = junctionBases.substring(JUNCTION_BASE_LENGTH - 2, JUNCTION_BASE_LENGTH);
                    mAdjacentJunctionBases[se] = junctionBases.substring(0, JUNCTION_BASE_LENGTH);
                }
            }
        }
        catch(Exception e)
        {
            // junction may be in an invalid region, just ignore these
        }

        setHomologyOffsets();
    }

    private void setHomologyOffsets()
    {
        // test moving the junction point back and forth from the current positions

        // in the int-pair array the first element is the number of bases the junction could move
        boolean startHasPosOrient = mJunctionOrientations[SE_START] == POS_ORIENT;

        for(int se = SE_START; se <= SE_END; ++se)
        {
            int psIndex = switchIndex(se);

            // first of all the junction start bases will be compared to the adjacent end bases to see if they were transferred from
            // the junction bases to the adjacent bases, would there be a match

            // for the start, compare the start bases with the other junction's post-junction bases
            for(int i = 1; i < JUNCTION_BASE_LENGTH; ++i)
            {
                if(mJunctionBases[se].length() < i || mAdjacentJunctionBases[psIndex].length() < i)
                    break;

                String junctionStr = mJunctionOrientations[se] == POS_ORIENT ?
                        mJunctionBases[se].substring(mJunctionBases[se].length() - i) :
                        mJunctionBases[se].substring(0, i);

                String adjacentStr = mJunctionOrientations[psIndex] == POS_ORIENT ?
                        mAdjacentJunctionBases[psIndex].substring(0, i) :
                        mAdjacentJunctionBases[psIndex].substring(mAdjacentJunctionBases[psIndex].length() - i);

                if(i > 1 && mJunctionOrientations[SE_START] == mJunctionOrientations[SE_END])
                {
                    reverseString(adjacentStr);
                }

                if(!junctionStr.equals(adjacentStr))
                    break;

                boolean testingPosRetreating = (se == SE_START && startHasPosOrient) || (se == SE_END && !startHasPosOrient);

                if(testingPosRetreating)
                    mJunctionHomology[SE_START] = -i;
                else
                    mJunctionHomology[SE_END] = i;
            }
        }
    }

    public boolean matchWithinHomology(final FusionReadData other)
    {
        // must be offset by the same number of bases
        if(mJunctionPositions[SE_START] - other.junctionPositions()[SE_START] != mJunctionPositions[SE_END] - other.junctionPositions()[SE_END])
            return false;

        for(int se = SE_START; se <= SE_END; ++se)
        {
            if(mJunctionOrientations[se] != other.junctionOrientations()[se])
                return false;

            if(mJunctionPositions[se] == other.junctionPositions()[se])
                continue;

            if(mJunctionPositions[se] < other.junctionPositions()[se])
            {
                if(mJunctionPositions[se] + mJunctionHomology[SE_END] < other.junctionPositions()[se])
                    return false;
            }
            else
            {
                if(mJunctionPositions[se] + mJunctionHomology[SE_START] > other.junctionPositions()[se])
                    return false;
            }
        }

        return true;
    }

    private static final int CLOSE_MATCH_BASES = 10;
    private static final double CLOSE_MATCH_FRAG_RATIO = 0.2;

    public boolean isCloseMatch(final FusionReadData other)
    {
        for(int se = SE_START; se <= SE_END; ++se)
        {
            if(abs(mJunctionPositions[se] - other.junctionPositions()[se]) > CLOSE_MATCH_BASES)
               return false;
        }

        double fragmentsRatio = getFragmentTypeCount(MATCHED_JUNCTION) / (double)other.getFragmentTypeCount(MATCHED_JUNCTION);
        return fragmentsRatio >= 1/CLOSE_MATCH_FRAG_RATIO || fragmentsRatio <= CLOSE_MATCH_FRAG_RATIO;
    }

    public void setStreamData(final List<GeneData> upstreamGenes, final List<GeneData> downstreamGenes, boolean startIsUpstream)
    {
        if(!upstreamGenes.isEmpty())
        {
            mStreamIndices[FS_UP] = startIsUpstream ? SE_START : SE_END;

            if(downstreamGenes.isEmpty())
                mStreamIndices[FS_DOWN] = switchStream(mStreamIndices[FS_UP]);

            mFusionGenes[FS_UP] = upstreamGenes.get(0);
        }

        if(!downstreamGenes.isEmpty())
        {
            mStreamIndices[FS_DOWN] = startIsUpstream ? SE_END : SE_START;

            if(upstreamGenes.isEmpty())
                mStreamIndices[FS_UP] = switchStream(mStreamIndices[FS_DOWN]);

            mFusionGenes[FS_DOWN] = downstreamGenes.get(0);
        }
    }

    public byte[] getGeneStrands()
    {
        if(!hasViableGenes())
            return null;

        if(mStreamIndices[FS_UP] == SE_START)
            return new byte[] { mFusionGenes[SE_START].Strand, mFusionGenes[SE_END].Strand };
        else
            return new byte[] { mFusionGenes[SE_END].Strand, mFusionGenes[SE_START].Strand };
    }

    public final Set<Integer> getRelatedFusions() { return mRelatedSplicedFusions; }

    public void addRelatedFusion(int id, boolean isSpliced)
    {
        if(isSpliced)
            mRelatedSplicedFusions.add(id);
        else
            mRelatedProximateFusions.add(id);
    }

    public StructuralVariantType getImpliedSvType()
    {
        return impliedSvType(mChromosomes, mJunctionOrientations);
    }

    public FusionFragment getInitialFragment() { return mFragment; }

    public void cacheTranscriptData()
    {
        for (int se = SE_START; se <= SE_END; ++se)
        {
            mTransExonRefs[se].addAll(mFragment.getTransExonRefs()[se]);
        }
    }

    public boolean canAddDiscordantFragment(final FusionFragment fragment, int maxFragmentDistance)
    {
        // a discordant read spans both genes and cannot be outside the standard long fragment length either in intronic terms
        // or by exons if exonic

        // a realigned fragment must touch one of the fusion junctions with soft-clipping

        // the 2 reads' bounds need to fall within 2 or less exons away
        // apply max fragment distance criteria

        int impliedFragmentLength = fragment.reads().get(0).Length * 2;

        for(int se = SE_START; se <= SE_END; ++se)
        {
            final List<TransExonRef> fragmentRefs = fragment.getTransExonRefs()[se];
            final List<TransExonRef> fusionRefs = getTransExonRefsByPos(se);

            // must match the orientations of the fusion junction
            if(fragment.orientations()[se] != mJunctionOrientations[se])
                return false;

            boolean isUpstream = (mStreamIndices[FS_UP] == se);

            int permittedExonDiff;

            if(isUpstream)
                permittedExonDiff = -2;
            else if(fragment.regionMatchTypes()[se] == INTRON)
                permittedExonDiff = 1;
            else
                permittedExonDiff = 2;

            if(!hasTranscriptExonMatch(fusionRefs, fragmentRefs, permittedExonDiff))
                return false;

            final List<ReadRecord> reads = fragment.readsByLocation(se);

            if(reads.isEmpty())
                return false;

            final ReadRecord read = reads.get(0);

            int fragmentPosition = read.getCoordsBoundary(switchIndex(se));

            // cannot be on the wrong side of the junction
            if((mJunctionOrientations[se] == 1) == (fragmentPosition > mJunctionPositions[se]))
            {
                // check if a mis-mapping explains the over-hang
                if(!softClippedReadSupportsJunction(read, se))
                    return false;
            }

            // of the fusion is unspliced or the fragment is intronic, then measure the genomic distance vs permitted fragment length
            if(fragment.regionMatchTypes()[se] == INTRON || mFragment.regionMatchTypes()[se] == INTRON)
            {
                if (fragmentPosition < mJunctionPositions[se])
                    impliedFragmentLength += mJunctionPositions[se] - fragmentPosition;
                else
                    impliedFragmentLength += fragmentPosition - mJunctionPositions[se];
            }
        }

        if(impliedFragmentLength > maxFragmentDistance)
            return false;

        return true;
    }

    public boolean canRelignFragmentToJunction(final FusionFragment fragment)
    {
        boolean hasSupportingRead = false;

        for (int se = SE_START; se <= SE_END; ++se)
        {
            final int seIndex = se;
            List<ReadRecord> reads = fragment.reads().stream()
                    .filter(x -> mChromosomes[seIndex].equals(x.Chromosome))
                    .filter(x -> mGeneCollections[seIndex] == x.getGeneCollectons()[seIndex])
                    .collect(Collectors.toList());

            for (ReadRecord read : reads)
            {
                if (softClippedReadSupportsJunction(read, se))
                {
                    hasSupportingRead = true;
                }

                // check that none of the other reads are on the incorrect side of this fusion junction
                if(mJunctionOrientations[se] == 1 && read.getCoordsBoundary(SE_END) > mJunctionPositions[se] + SOFT_CLIP_JUNC_BUFFER)
                    return false;
                else if(mJunctionOrientations[se] == -1 && read.getCoordsBoundary(SE_START) < mJunctionPositions[se] - SOFT_CLIP_JUNC_BUFFER)
                    return false;
            }
        }

        return hasSupportingRead;
    }

    private boolean softClippedReadSupportsJunction(final ReadRecord read, int juncSeIndex)
    {
        return softClippedReadSupportsJunction(
                read, juncSeIndex, mJunctionPositions[juncSeIndex], mJunctionOrientations[juncSeIndex], mJunctionBases);
    }

    public static boolean softClippedReadSupportsJunction(
            final ReadRecord read, int juncSeIndex, int junctionPosition, byte junctionOrientation, final String[] junctionBases)
    {
        // compare a minimum number of soft-clipped bases to the other side of the exon junction
        // if the read extends past break junction, include these bases in what is compared against the next junction to account for homology
        if(junctionOrientation == POS_ORIENT)
        {
            if(!read.isSoftClipped(SE_END))
                return false;

            int readBoundary = read.getCoordsBoundary(SE_END);

            // the fragment is limited to how far past the junction (into the other fused gene) it can overhang by a mis-map
            if(!positionWithin(readBoundary, junctionPosition, junctionPosition + SOFT_CLIP_JUNC_BUFFER))
                return false;

            // test that soft-clipped bases match the other junction's bases
            int scLength = read.Cigar.getLastCigarElement().getLength();

            if(scLength < REALIGN_MIN_SOFT_CLIP_BASE_LENGTH || scLength > REALIGN_MAX_SOFT_CLIP_BASE_LENGTH)
                return false;

            if(junctionBases == null)
                return true;

            // if the junction is 1 base higher, then take 1 base off the soft-clipped bases
            int posAdjust = readBoundary > junctionPosition ? readBoundary - junctionPosition : 0;

            String extraBases = read.ReadBases.substring(read.Length - scLength - posAdjust, read.Length);

            if(extraBases.length() > JUNCTION_BASE_LENGTH)
                extraBases = extraBases.substring(0, JUNCTION_BASE_LENGTH);

            return junctionBases[switchIndex(juncSeIndex)].startsWith(extraBases);
        }
        else
        {
            if(!read.isSoftClipped(SE_START))
                return false;

            int readBoundary = read.getCoordsBoundary(SE_START);

            if(!positionWithin(readBoundary, junctionPosition - SOFT_CLIP_JUNC_BUFFER, junctionPosition))
                return false;

            int scLength = read.Cigar.getFirstCigarElement().getLength();

            if(scLength < REALIGN_MIN_SOFT_CLIP_BASE_LENGTH || scLength > REALIGN_MAX_SOFT_CLIP_BASE_LENGTH)
                return false;

            if(junctionBases == null)
                return true;

            int posAdjust = readBoundary < junctionPosition ? junctionPosition - readBoundary : 0;

            String extraBases = read.ReadBases.substring(0, scLength + posAdjust);

            if(extraBases.length() > JUNCTION_BASE_LENGTH)
                extraBases = extraBases.substring(extraBases.length() - JUNCTION_BASE_LENGTH, extraBases.length());

            return junctionBases[switchIndex(juncSeIndex)].endsWith(extraBases);
        }
    }

    private void updateMaxSplitMappedLength(final FusionFragment fragment)
    {
        // find the longest section mapped across the junction
        for (int se = SE_START; se <= SE_END; ++se)
        {
            final int seIndex = se;

            final List<ReadRecord> reads = fragment.readsByLocation(se).stream()
                    .filter(x -> positionWithin(mJunctionPositions[seIndex], x.PosStart, x.PosEnd)).collect(Collectors.toList());

            if(reads.isEmpty()) // can occur with the fragments from a fusion merged in due to homology
                continue;

            List<int[]> mappedCoords;

            if(reads.size() == 1)
            {
                mappedCoords = reads.get(0).getMappedRegionCoords(false);
            }
            else
            {
                mappedCoords = deriveCommonRegions(
                        reads.get(0).getMappedRegionCoords(false), reads.get(1).getMappedRegionCoords(false));
            }

            int mappedBases = 0;

            for(int[] coord : mappedCoords)
            {
                if(mJunctionOrientations[se] == NEG_ORIENT)
                {
                    if(coord[SE_END] < mJunctionPositions[se])
                        continue;

                    mappedBases += coord[SE_END] - max(mJunctionPositions[se], coord[SE_START]) + 1;
                }
                else
                {
                    if(coord[SE_START] > mJunctionPositions[se])
                        break;

                    mappedBases += min(mJunctionPositions[se], coord[SE_END]) - coord[SE_START] + 1;
                }
            }

            mMaxSplitLengths[se] = max(mappedBases, mMaxSplitLengths[se]);
        }
    }

    public int[] getReadDepth() { return mReadDepth; }
    public int[] getMaxSplitLengths() { return mMaxSplitLengths; }

    public String getGeneName(int stream)
    {
        return mFusionGenes[stream] != null ? mFusionGenes[stream].GeneName : "";
    }

    public String toString()
    {
        return String.format("%d: chr(%s-%s) junc(%d-%d %d/%d %s) genes(%s-%s) frags(%d)",
                mId, mChromosomes[SE_START], mChromosomes[SE_END], mJunctionPositions[SE_START], mJunctionPositions[SE_END],
                mJunctionOrientations[SE_START], mJunctionOrientations[SE_END], getImpliedSvType(),
                getGeneName(FS_UP), getGeneName(FS_DOWN), mFragmentCounts.values().stream().mapToInt(x -> x).sum());
    }

    public static String csvHeader()
    {
        return "FusionId,Valid,GeneIdUp,GeneNameUp,ChrUp,PosUp,OrientUp,StrandUp,JuncTypeUp"
                + ",GeneIdDown,GeneNameDown,ChrDown,PosDown,OrientDown,StrandDown,JuncTypeDown"
                + ",SVType,NonSupp,TotalFragments,SplitFrags,RealignedFrags,DiscordantFrags,CoverageUp,CoverageDown"
                + ",MaxAnchorLengthUp,MaxAnchorLengthDown,TransDataUp,TransDataDown"
                + ",RelatedSplicedIds,RelatedProxIds,InitReadId,HomologyOffset";
    }

    public static final String FUSION_ID_PREFIX = "Id_";
    public static final String FUSION_NONE = "NONE";

    public static String fusionId(int id) { return String.format("%s%d", FUSION_ID_PREFIX, id); }

    public String toCsv()
    {
        StringJoiner csvData = new StringJoiner(DELIMITER);

        csvData.add(fusionId(mId));
        csvData.add(String.valueOf(hasViableGenes() && !hasIncompleteData()));

        final FusionFragment sampleFragment = getInitialFragment();

        for(int fs = FS_UP; fs <= FS_DOWN; ++fs)
        {
            final GeneData geneData = mFusionGenes[fs];

            csvData.add(geneData != null ? geneData.GeneId : "");
            csvData.add(geneData != null ? geneData.GeneName : "");

            csvData.add(mChromosomes[mStreamIndices[fs]]);
            csvData.add(String.valueOf(mJunctionPositions[mStreamIndices[fs]]));
            csvData.add(String.valueOf(mJunctionOrientations[mStreamIndices[fs]]));

            csvData.add(geneData != null ? String.valueOf(geneData.Strand) : "0");
            csvData.add(sampleFragment.junctionTypes()[mStreamIndices[fs]].toString());
        }

        csvData.add(getImpliedSvType().toString());
        csvData.add(String.valueOf(!sampleFragment.hasSuppAlignment()));

        int splitFragments = 0;
        int realignedFragments = 0;
        int discordantFragments = 0;

        for(Map.Entry<FusionFragmentType,Integer> entry : mFragmentCounts.entrySet())
        {
            if(entry.getKey() == MATCHED_JUNCTION)
                splitFragments = entry.getValue();
            else if(entry.getKey() == DISCORDANT)
                discordantFragments = entry.getValue();
            else if(entry.getKey() == REALIGNED)
                realignedFragments = entry.getValue();
        }

        int totalFragments = splitFragments + realignedFragments + discordantFragments;

        csvData.add(String.valueOf(totalFragments));
        csvData.add(String.valueOf(splitFragments));
        csvData.add(String.valueOf(realignedFragments));
        csvData.add(String.valueOf(discordantFragments));

        // since depth of 1 may have been discarded from the BaseDepth, correct for this
        csvData.add(String.valueOf(max(mReadDepth[mStreamIndices[FS_UP]], splitFragments)));
        csvData.add(String.valueOf(max(mReadDepth[mStreamIndices[FS_DOWN]], splitFragments)));

        csvData.add(String.valueOf(mMaxSplitLengths[mStreamIndices[FS_UP]]));
        csvData.add(String.valueOf(mMaxSplitLengths[mStreamIndices[FS_DOWN]]));

        for (int fs = FS_UP; fs <= FS_DOWN; ++fs)
        {
            final List<TransExonRef> transExonRefs = getTransExonRefsByStream(fs);
            if(transExonRefs.isEmpty())
            {
                csvData.add(FUSION_NONE);
                continue;
            }

            String transData = "";
            for(final TransExonRef transExonRef : transExonRefs)
            {
                transData = appendStr(transData, String.format("%s-%d", transExonRef.TransName, transExonRef.ExonRank), ';');
            }

            csvData.add(transData);
        }

        if(!mRelatedSplicedFusions.isEmpty())
        {
            List<String> relatedFusions = mRelatedSplicedFusions.stream().map(x -> fusionId(x)).collect(Collectors.toList());
            csvData.add(appendStrList(relatedFusions, ';'));
        }
        else
        {
            csvData.add(FUSION_NONE);
        }

        if(!mRelatedProximateFusions.isEmpty())
        {
            List<String> relatedFusions = mRelatedProximateFusions.stream().map(x -> fusionId(x)).collect(Collectors.toList());
            csvData.add(appendStrList(relatedFusions, ';'));
        }
        else
        {
            csvData.add(FUSION_NONE);
        }

        // csvData.add(ISF_LOGGER.isDebugEnabled() ? getInitialFragment().readId() : "-");
        csvData.add(getInitialFragment().readId()); // very handy for now
        csvData.add(String.format("%d-%d", mJunctionHomology[SE_START], mJunctionHomology[SE_END]));

        return csvData.toString();
    }

}
