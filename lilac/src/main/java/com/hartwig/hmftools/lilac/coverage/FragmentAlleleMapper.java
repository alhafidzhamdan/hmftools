package com.hartwig.hmftools.lilac.coverage;

import static com.hartwig.hmftools.lilac.LilacConfig.LL_LOGGER;
import static com.hartwig.hmftools.lilac.LilacConstants.GENE_A;
import static com.hartwig.hmftools.lilac.LilacConstants.HLA_Y_FRAGMENT_THRESHOLD;
import static com.hartwig.hmftools.lilac.LilacConstants.getAminoAcidExonBoundaries;
import static com.hartwig.hmftools.lilac.LilacConstants.getNucleotideExonBoundaries;
import static com.hartwig.hmftools.lilac.LilacConstants.longGeneName;
import static com.hartwig.hmftools.lilac.fragment.FragmentScope.HLA_Y;
import static com.hartwig.hmftools.lilac.fragment.FragmentScope.NO_HET_LOCI;
import static com.hartwig.hmftools.lilac.fragment.FragmentScope.UNMATCHED_AMINO_ACID;
import static com.hartwig.hmftools.lilac.fragment.FragmentScope.WILD_ONLY;
import static com.hartwig.hmftools.lilac.seq.HlaSequence.WILD_STR;
import static com.hartwig.hmftools.lilac.seq.SequenceMatchType.FULL;
import static com.hartwig.hmftools.lilac.seq.SequenceMatchType.NO_LOCI;
import static com.hartwig.hmftools.lilac.seq.SequenceMatchType.WILD;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.utils.PerformanceCounter;
import com.hartwig.hmftools.lilac.fragment.Fragment;
import com.hartwig.hmftools.lilac.hla.HlaAllele;
import com.hartwig.hmftools.lilac.seq.HlaSequenceLoci;
import com.hartwig.hmftools.lilac.seq.SequenceMatchType;

public class FragmentAlleleMapper
{
    private final Map<String,Map<Integer,List<String>>> mGeneAminoAcidHetLociMap;
    private final Map<String,List<Integer>> mRefNucleotideHetLoci;
    private final List<Set<String>> mRefNucleotides;

    private final PerformanceCounter mPerfCounterFrag;
    private final PerformanceCounter mPerfCounterAcid;
    private final PerformanceCounter mPerfCounterNuc;

    public FragmentAlleleMapper(
            final Map<String, Map<Integer,List<String>>> geneAminoAcidHetLociMap,
            final Map<String,List<Integer>> refNucleotideHetLoci, final List<Set<String>> refNucleotides)
    {
        mGeneAminoAcidHetLociMap = geneAminoAcidHetLociMap;
        mRefNucleotideHetLoci = refNucleotideHetLoci;
        mRefNucleotides = refNucleotides;

        mPerfCounterFrag = new PerformanceCounter("Frags");
        mPerfCounterNuc = new PerformanceCounter("Nuc");
        mPerfCounterAcid = new PerformanceCounter("AminoAcid");
    }

    public void logPerfData()
    {
        mPerfCounterFrag.logStats();
        mPerfCounterNuc.logStats();
        mPerfCounterAcid.logStats();
    }

    public List<FragmentAlleles> createFragmentAlleles(
            final List<Fragment> refCoverageFragments, final List<HlaSequenceLoci> candidateAminoAcidSequences,
            final List<HlaSequenceLoci> candidateNucleotideSequences)
    {
        LL_LOGGER.info("building frag-alleles from aminoAcids(frags={} candSeq={}) nucFrags(hetLoci={} candSeq={} nucs={})",
                refCoverageFragments.size(), candidateAminoAcidSequences.size(),
                mRefNucleotideHetLoci.size(), candidateNucleotideSequences.size(), mRefNucleotides.size());

        List<FragmentAlleles> results = Lists.newArrayList();

        for(Fragment fragment : refCoverageFragments)
        {
            mPerfCounterFrag.start();
            FragmentAlleles fragmentAlleles = mapFragmentToAlleles(fragment, candidateAminoAcidSequences, candidateNucleotideSequences);
            mPerfCounterFrag.stop();

            // drop wild-only alleles since their support can't be clearly established
            if(!fragmentAlleles.getFull().isEmpty())
            {
                results.add(fragmentAlleles);
            }
            else
            {
                if(!fragmentAlleles.getWild().isEmpty())
                {
                    fragment.setScope(WILD_ONLY);
                }
                else
                {
                    // was this fragment homozygous in the context of all genes
                    boolean hasHetLoci = false;

                    for(Map.Entry<String,Map<Integer,List<String>>> geneEntry : mGeneAminoAcidHetLociMap.entrySet())
                    {
                        if(!fragment.getGenes().contains(longGeneName(geneEntry.getKey())))
                            continue;

                        if(fragment.getAminoAcidLoci().stream().anyMatch(x -> geneEntry.getValue().containsKey(x)))
                        {
                            hasHetLoci = true;
                            break;
                        }
                    }

                    if(!hasHetLoci)
                    {
                        fragment.setScope(NO_HET_LOCI);
                    }
                    else
                    {
                        fragment.setScope(UNMATCHED_AMINO_ACID);
                    }
                }
            }
        }

        return results;
    }

    private FragmentAlleles mapFragmentToAlleles(
            final Fragment fragment, final List<HlaSequenceLoci> aminoAcidSequences, final List<HlaSequenceLoci> nucleotideSequences)
    {
        // look first for nucleotide support at the exon boundaries, then for amino acid support - full or wild
        Map<String,List<Integer>> fragNucleotideLociMap = Maps.newHashMap();

        mRefNucleotideHetLoci.entrySet().forEach(x -> fragNucleotideLociMap.put(
                x.getKey(), fragment.getNucleotideLoci().stream().filter(y -> x.getValue().contains(y)).collect(Collectors.toList())));

        mPerfCounterNuc.start();
        Map<HlaAllele, SequenceMatchType> nucleotideAlleleMatches = findNucleotideMatches(fragment, nucleotideSequences);
        mPerfCounterNuc.stop();

        List<HlaAllele> fullNucleotideMatch = nucleotideAlleleMatches.entrySet().stream()
                .filter(x -> x.getValue() == FULL).map(x -> x.getKey()).collect(Collectors.toList());

        List<HlaAllele> wildNucleotideMatch = nucleotideAlleleMatches.entrySet().stream()
                .filter(x -> x.getValue() == WILD)
                .map(x -> x.getKey()).collect(Collectors.toList());

        mPerfCounterAcid.start();
        Map<HlaAllele, SequenceMatchType> aminoAcidAlleleMatches = findAminoAcidMatches(fragment, aminoAcidSequences);
        mPerfCounterAcid.stop();

        List<HlaAllele> fullAminoAcidMatch = aminoAcidAlleleMatches.entrySet().stream()
                .filter(x -> x.getValue() == FULL).map(x -> x.getKey()).collect(Collectors.toList());

        List<HlaAllele> wildAminoAcidMatch = aminoAcidAlleleMatches.entrySet().stream()
                .filter(x -> x.getValue() == WILD).map(x -> x.getKey()).collect(Collectors.toList());

        // List<HlaAllele> homLociAminoAcidMatch = Lists.newArrayList();

        List<HlaAllele> homLociAminoAcidMatch = aminoAcidAlleleMatches.entrySet().stream()
                .filter(x -> x.getValue() == NO_LOCI).map(x -> x.getKey()).collect(Collectors.toList());

        if(fullNucleotideMatch.isEmpty() && wildNucleotideMatch.isEmpty())
        {
            // do not allow wild-only (ie no full) if there are homozygous matches
            if(fullAminoAcidMatch.isEmpty() && !wildAminoAcidMatch.isEmpty() && !homLociAminoAcidMatch.isEmpty())
                return new FragmentAlleles(fragment, Lists.newArrayList(), Lists.newArrayList());

            return new FragmentAlleles(fragment, fullAminoAcidMatch, wildAminoAcidMatch);
        }

        // otherwise look for matching nuc and amino-acid full matches
        List<HlaAllele> consistentFull = fullNucleotideMatch.stream()
                .filter(x -> fullAminoAcidMatch.contains(x) || homLociAminoAcidMatch.contains(x)).collect(Collectors.toList());

        // otherwise down-grade the full matches to wild
        fullAminoAcidMatch.stream()
                .filter(x -> !wildAminoAcidMatch.contains(x))
                .filter(x -> wildNucleotideMatch.contains(x))
                .forEach(x -> wildAminoAcidMatch.add(x));

        return new FragmentAlleles(fragment, consistentFull, wildAminoAcidMatch);
    }

    private Map<HlaAllele, SequenceMatchType> findNucleotideMatches(
            final Fragment fragment, final List<HlaSequenceLoci> nucleotideSequences)
    {
        Map<String,List<Integer>> fragGeneLociMap = Maps.newHashMap();
        Map<String,List<String>> fragGeneSequenceMap = Maps.newHashMap();

        Map<HlaAllele, SequenceMatchType> alleleMatches = Maps.newHashMap();

        // also attempt to retrieve amino acids from low-qual nucleotides
        Map<Integer,String> missedNucleotides = Maps.newHashMap();

        for(Map.Entry<String,List<Integer>> entry : mRefNucleotideHetLoci.entrySet())
        {
            String gene = entry.getKey();
            List<Integer> refNucleotideLoci = entry.getValue();

            List<Integer> fragmentMatchedLoci = fragment.getNucleotideLoci().stream()
                    .filter(y -> refNucleotideLoci.contains(y)).collect(Collectors.toList());

            // also check for support from low-qual reads at this same location
            List<Integer> missedNucleotideLoci = refNucleotideLoci.stream()
                    .filter(x -> !fragment.getNucleotideLoci().contains(x)).collect(Collectors.toList());

            for(Integer missedLocus : missedNucleotideLoci)
            {
                if(missedLocus >= mRefNucleotides.size())
                    continue;

                String lowQualNucleotide = fragment.getLowQualNucleotide(missedLocus);

                if(lowQualNucleotide.isEmpty())
                    continue;

                Set<String> candidateNucleotides = mRefNucleotides.get(missedLocus);

                if(candidateNucleotides.contains(lowQualNucleotide))
                {
                    fragmentMatchedLoci.add(missedLocus);
                    missedNucleotides.put(missedLocus, lowQualNucleotide);
                }
            }

            Collections.sort(fragmentMatchedLoci);

            final List<String> fragmentNucleotides = Lists.newArrayListWithExpectedSize(fragmentMatchedLoci.size());

            for(Integer locus : fragmentMatchedLoci)
            {
                if(missedNucleotides.containsKey(locus))
                    fragmentNucleotides.add(missedNucleotides.get(locus));
                else
                    fragmentNucleotides.add(fragment.nucleotide(locus));
            }

            fragGeneLociMap.put(gene, fragmentMatchedLoci);
            fragGeneSequenceMap.put(gene, fragmentNucleotides);
        }

        if(fragGeneLociMap.values().stream().allMatch(x -> x.isEmpty()))
            return alleleMatches;

        for(HlaSequenceLoci sequence : nucleotideSequences)
        {
            HlaAllele allele = sequence.Allele;

            HlaAllele proteinAllele = allele.asFourDigit();

            SequenceMatchType existingMatch = alleleMatches.get(proteinAllele);

            if(existingMatch != null && existingMatch == FULL)
                continue;

            if(!fragment.getGenes().contains(allele.geneName()))
                continue;

            List<Integer> fragNucleotideLoci = fragGeneLociMap.get(allele.Gene);
            if(fragNucleotideLoci.isEmpty())
                continue;

            // filter out wildcard bases at these exon boundaries
            List<String> fragmentNucleotides = fragGeneSequenceMap.get(allele.Gene);

            if(sequence.hasExonBoundaryWildcards())
            {
                // mustn't change the original
                fragmentNucleotides = fragmentNucleotides.stream().collect(Collectors.toList());
                fragNucleotideLoci = fragNucleotideLoci.stream().collect(Collectors.toList());

                // ignore any wildcard loci at an exon boundary
                List<Integer> nucleotideExonBoundaries = getNucleotideExonBoundaries(sequence.Allele.Gene);

                int index = 0;
                while(index < fragNucleotideLoci.size())
                {
                    int locus = fragNucleotideLoci.get(index);
                    boolean wildcardExonBoundary = nucleotideExonBoundaries.contains(locus)
                            && locus < sequence.length() && sequence.sequence(locus).equals(WILD_STR);

                    if(!wildcardExonBoundary)
                    {
                        ++index;
                    }
                    else
                    {
                        fragNucleotideLoci.remove(index);
                        fragmentNucleotides.remove(index);
                    }
                }
            }

            SequenceMatchType matchType;
            if(fragNucleotideLoci.isEmpty())
            {
                // if all bases being considered a wild, the treat it as full in nucleotide space and rely on the amino acid match type
                matchType = FULL;
            }
            else
            {
                matchType = sequence.determineMatchType(fragmentNucleotides, fragNucleotideLoci);
            }

            if(matchType == SequenceMatchType.MISMATCH)
                continue;

            if(existingMatch == null || matchType.isBetter(existingMatch))
            {
                alleleMatches.put(proteinAllele, matchType);
            }
        }

        return alleleMatches;
    }

    private Map<HlaAllele, SequenceMatchType> findAminoAcidMatches(final Fragment fragment, final List<HlaSequenceLoci> aminoAcidSequences)
    {
        Map<HlaAllele,SequenceMatchType> alleleMatches = Maps.newHashMap();

        Map<String,List<Integer>> fragGeneLociMap = Maps.newHashMap(); // per-gene map of heterozygous locations for this fragment
        Map<String,List<String>> fragGeneSequenceMap = Maps.newHashMap(); // per-gene map of fragment sequences at these het loci

        for(Map.Entry<String,Map<Integer,List<String>>> geneEntry : mGeneAminoAcidHetLociMap.entrySet())
        {
            Map<Integer,List<String>> hetLociSeqMap = geneEntry.getValue();

            List<Integer> fragAminoAcidLoci = fragment.getAminoAcidLoci().stream()
                    .filter(x -> hetLociSeqMap.containsKey(x)).collect(Collectors.toList());

            if(fragAminoAcidLoci.isEmpty())
                continue;

            // also attempt to retrieve amino acids from low-qual nucleotides
            Map<Integer,String> missedAminoAcids = Maps.newHashMap();

            List<Integer> missedAminoAcidLoci = hetLociSeqMap.keySet().stream()
                    .filter(x -> !fragAminoAcidLoci.contains(x)).collect(Collectors.toList());

            for(Integer missedLocus : missedAminoAcidLoci)
            {
                String lowQualAminoAcid = fragment.getLowQualAminoAcid(missedLocus);

                if(lowQualAminoAcid.isEmpty())
                    continue;

                List<String> candidateAminoAcids = hetLociSeqMap.get(missedLocus);

                if(candidateAminoAcids.contains(lowQualAminoAcid))
                {
                    fragAminoAcidLoci.add(missedLocus);
                    missedAminoAcids.put(missedLocus, lowQualAminoAcid);
                }
            }

            Collections.sort(fragAminoAcidLoci);

            final List<String> fragmentAminoAcids = Lists.newArrayListWithExpectedSize(fragAminoAcidLoci.size());

            for(Integer locus : fragAminoAcidLoci)
            {
                if(missedAminoAcids.containsKey(locus))
                    fragmentAminoAcids.add(missedAminoAcids.get(locus));
                else
                    fragmentAminoAcids.add(fragment.aminoAcid(locus));
            }

            fragGeneLociMap.put(geneEntry.getKey(), fragAminoAcidLoci);
            fragGeneSequenceMap.put(geneEntry.getKey(), fragmentAminoAcids);
        }

        for(HlaSequenceLoci sequence : aminoAcidSequences)
        {
            HlaAllele allele = sequence.Allele;

            if(!fragment.getGenes().contains(allele.geneName()))
            {
                alleleMatches.put(allele, NO_LOCI);
                continue;
            }

            List<Integer> fragAminoAcidLoci = fragGeneLociMap.get(allele.Gene);

            if(fragAminoAcidLoci == null) // not supported by this gene or homozygous in all locations
            {
                alleleMatches.put(allele, NO_LOCI);
                continue;
            }

            List<String> fragmentAminoAcids = fragGeneSequenceMap.get(allele.Gene);

            if(sequence.hasExonBoundaryWildcards())
            {
                // mustn't change the original
                fragmentAminoAcids = fragmentAminoAcids.stream().collect(Collectors.toList());
                fragAminoAcidLoci = fragAminoAcidLoci.stream().collect(Collectors.toList());

                // ignore any wildcard loci at an exon boundary
                List<Integer> aminoAcidExonBoundaries = getAminoAcidExonBoundaries(sequence.Allele.Gene);

                int index = 0;
                while(index < fragAminoAcidLoci.size())
                {
                    int locus = fragAminoAcidLoci.get(index);
                    boolean wildcardExonBoundary = aminoAcidExonBoundaries.contains(locus)
                            && locus < sequence.length() && sequence.sequence(locus).equals(WILD_STR);

                    if(!wildcardExonBoundary)
                    {
                        ++index;
                    }
                    else
                    {
                        fragAminoAcidLoci.remove(index);
                        fragmentAminoAcids.remove(index);
                    }
                }
            }

            SequenceMatchType matchType = sequence.determineMatchType(fragmentAminoAcids, fragAminoAcidLoci);
            if(matchType == SequenceMatchType.MISMATCH)
                continue;

            alleleMatches.put(allele, matchType);
        }

        return alleleMatches;
    }

    public boolean checkHlaYSupport(
            final List<HlaSequenceLoci> hlaYSequences, final List<FragmentAlleles> fragAlleles, final List<Fragment> fragments)
    {
        // ignore fragments which don't contain any heterozygous locations
        int uniqueHlaY = 0;

        List<FragmentAlleles> matchedFragmentAlleles = Lists.newArrayList();

        // only test heterozygous locations in A since HLA-Y matches its exon boundaries
        Set<Integer> aminoAcidHetLoci = mGeneAminoAcidHetLociMap.get(GENE_A).keySet();

        for(Fragment fragment : fragments)
        {
            List<Integer> fragAminoAcidLoci = fragment.getAminoAcidLoci().stream()
                    .filter(x -> aminoAcidHetLoci.contains(x)).collect(Collectors.toList());

            if(fragAminoAcidLoci.isEmpty())
                continue;

            List<Integer> fragNucleotideLoci = fragment.getNucleotideLoci();

            boolean matchesY = false;
            FragmentAlleles matchedFrag = null;

            for(HlaSequenceLoci sequence : hlaYSequences)
            {
                String fragNucleotides = fragment.nucleotides(fragNucleotideLoci);

                SequenceMatchType matchType = sequence.determineMatchType(fragNucleotides, fragNucleotideLoci);
                if(matchType == SequenceMatchType.FULL)
                {
                    matchesY = true;

                    matchedFrag = fragAlleles.stream()
                            .filter(x -> x.getFragment().id().equals(fragment.id())).findFirst().orElse(null);

                    /*
                    if(LL_LOGGER.isDebugEnabled())
                    {
                        HlaAllele allele = sequence.Allele;

                        LL_LOGGER.debug("HLA-Y allele({}) fragment({}: {}) range({} -> {}) assignedGenes({})",
                                allele.toString(), fragment.id(), fragment.readInfo(),
                                fragment.getNucleotideLoci().get(0),
                                fragment.getNucleotideLoci().get(fragment.getNucleotideLoci().size() - 1),
                                matchedFrag != null ? matchedFrag.getFragment().getGenes() : "");
                    }
                    */

                    break;
                }
            }

            if(!matchesY)
                continue;

            if(matchedFrag == null)
            {
                ++uniqueHlaY;
                fragment.setScope(HLA_Y, true);
            }
            else
            {
                matchedFragmentAlleles.add(matchedFrag);
            }
        }

        int totalHlaYFrags = uniqueHlaY  + matchedFragmentAlleles.size();
        double threshold = fragments.size() * HLA_Y_FRAGMENT_THRESHOLD;
        boolean exceedsThreshold = uniqueHlaY >= threshold;

        if(totalHlaYFrags > 0)
        {
            LL_LOGGER.info("HLA-Y fragments({} unique={}) shared={}) aboveThreshold({})",
                    totalHlaYFrags, uniqueHlaY, matchedFragmentAlleles.size(), exceedsThreshold);

            if(exceedsThreshold)
            {
                matchedFragmentAlleles.forEach(x -> fragAlleles.remove(x));
                matchedFragmentAlleles.forEach(x -> x.getFragment().setScope(HLA_Y, true));
            }
        }

        return exceedsThreshold;
    }

    public static void applyUniqueStopLossFragments(
            final List<FragmentAlleles> fragmentAlleles, int stopLossFragments, final List<HlaAllele> stopLossAlleles)
    {
        if(stopLossFragments == 0)
            return;

        for(HlaAllele stopLossAllele : stopLossAlleles)
        {
            List<FragmentAlleles> sampleFragments = fragmentAlleles.stream()
                    .filter(x -> x.contains(stopLossAllele))
                    .map(x -> new FragmentAlleles(x.getFragment(), Lists.newArrayList(stopLossAllele), Lists.newArrayList()))
                    .collect(Collectors.toList());

            for(int i = 0; i < stopLossFragments; ++i)
            {
                if(i >= sampleFragments.size())
                    break;

                fragmentAlleles.add(sampleFragments.get(i));
            }
        }
    }



}
