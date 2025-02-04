package com.hartwig.hmftools.gripss.rm;

import static com.hartwig.hmftools.common.utils.FileWriterUtils.createBufferedReader;
import static com.hartwig.hmftools.gripss.GripssConfig.GR_LOGGER;
import static com.hartwig.hmftools.gripss.rm.AlignmentData.fromInsertSequenceAlignments;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.genome.chromosome.HumanChromosome;
import com.hartwig.hmftools.common.genome.refgenome.RefGenomeVersion;
import com.hartwig.hmftools.common.utils.sv.BaseRegion;

import org.apache.commons.cli.Options;

public class RepeatMaskAnnotations
{
    private final Map<String,List<RepeatMaskData>> mChrDataMap;

    public static final String REPEAT_MASK_FILE = "repeat_mask_file";
    public static final String REPEAT_MASK_FILE_DESC = "Repeat mask definitions file";

    private static final int MIN_OVERLAP = 20;
    private static final double MIN_COVERAGE_PERC = 0.1;
    private static final double POLY_A_T_PERC = 0.9;

    private static final RepeatMaskData POLY_T_DATA = new RepeatMaskData(
            0, new BaseRegion(0, 1), 0, ' ', "T(n)", "Simple_repeat");

    private static final RepeatMaskData POLY_A_DATA = new RepeatMaskData(
            0, new BaseRegion(0, 1), 0, ' ', "A(n)", "Simple_repeat");

    public RepeatMaskAnnotations()
    {
        mChrDataMap = Maps.newHashMap();
    }

    public static void addCmdLineArgs(Options options)
    {
        options.addOption(REPEAT_MASK_FILE, true, REPEAT_MASK_FILE_DESC);
    }

    public boolean hasData() { return !mChrDataMap.isEmpty(); }

    public RepeatMaskAnnotation annotate(final String insertSequence, final String alignmentsStr)
    {
        List<AlignmentData> alignments = fromInsertSequenceAlignments(alignmentsStr);

        if(alignments == null || alignments.isEmpty())
            return null;

        double insSeqLength = insertSequence.length();

        // first check for a poly-A or T
        for(AlignmentData alignment : alignments)
        {
            String matchedBases = alignment.extractMatchedBases(insertSequence);
            double polyAtPerc = calcPolyATPercent(matchedBases);

            if(polyAtPerc >= POLY_A_T_PERC)
            {
                double coverage = matchedBases.length() / insSeqLength;
                long aCount = matchedBases.chars().filter(x -> x == 'A').count();
                RepeatMaskData rmData = aCount > matchedBases.length() / 2 ? POLY_A_DATA : POLY_T_DATA;
                return new RepeatMaskAnnotation(rmData, coverage, alignment);
            }
        }

        RepeatMaskData topRm = null;
        AlignmentData topAlignment = null;
        double topCoverage = 0;

        for(AlignmentData alignment : alignments)
        {
            List<RepeatMaskData> rmMatches = findMatches(alignment.Chromosome, alignment.Region);

            if(rmMatches.isEmpty())
                continue;

            for(RepeatMaskData rmData : rmMatches)
            {
                int overlap = rmData.overlappingBases(alignment);

                if(overlap < MIN_OVERLAP)
                    continue;

                double coverage = overlap / insSeqLength;

                if(coverage < MIN_COVERAGE_PERC)
                    continue;

                if(coverage > topCoverage)
                {
                    topCoverage = coverage;
                    topRm = rmData;
                    topAlignment = alignment;
                }
            }
        }

        return topRm != null ? new RepeatMaskAnnotation(topRm, topCoverage, topAlignment) : null;
    }

    private static double calcPolyATPercent(final String sequence)
    {
        int count = 0;

        for(int i = 0; i < sequence.length(); ++i)
        {
            if(sequence.charAt(i) == 'T' || sequence.charAt(i) == 'A')
                ++count;
        }

        return count / (double)sequence.length();
    }

    public List<RepeatMaskData> findMatches(final String chromosome, final BaseRegion region)
    {
        List<RepeatMaskData> regions = mChrDataMap.get(chromosome);

        if(regions == null)
            return Lists.newArrayList();

        if(regions.size() < 100)
            return regions.stream().filter(x -> x.Region.overlaps(region)).collect(Collectors.toList());

        // use a binary search since the number of entries is typically > 100K per chromosome
        int currentIndex = regions.size() / 2;
        int lowerIndex = 0;
        int upperIndex = regions.size() - 1;

        List<RepeatMaskData> matchedRegions = Lists.newArrayList();

        while(true)
        {
            RepeatMaskData rmData = regions.get(currentIndex);

            if(region.end() < rmData.Region.start())
            {
                if(lowerIndex + 1 == currentIndex)
                    break;

                upperIndex = currentIndex;
                currentIndex = (lowerIndex + upperIndex) / 2;
            }
            else if(region.start() > rmData.Region.end())
            {
                // search higher
                if(currentIndex + 1 == upperIndex)
                    break;

                lowerIndex = currentIndex;
                currentIndex = (lowerIndex + upperIndex) / 2;
            }
            else if(rmData.Region.overlaps(region))
            {
                matchedRegions.add(rmData);
                break;
            }
        }

        // check up and down for further overlaps
        for(int i = 0; i <= 1; ++i)
        {
            boolean searchUp = (i == 0);

            int index = searchUp ? currentIndex + 1 : currentIndex - 1;

            while(index >= 0 && index < regions.size())
            {
                RepeatMaskData rmData = regions.get(index);

                if(!rmData.Region.overlaps(region))
                    break;

                matchedRegions.add(rmData);

                if(searchUp)
                    ++index;
                else
                    --index;
            }
        }

        return matchedRegions;
    }

    public boolean load(final String filename, final RefGenomeVersion refGenomeVersion)
    {
        if(filename == null)
            return true;

        try
        {
            BufferedReader fileReader = createBufferedReader(filename);

            String line = null;
            String currentChr = "";
            List<RepeatMaskData> entries = null;
            int index = 0;

            // first 3 lines contain the header, then expect columns as:
            // SW     perc perc perc  query      position in query           matching       repeat              position in  repeat
            // score  div. del. ins.  sequence    begin     end    (left)    repeat         class/family         begin  end (left)   ID
            // 0      1    2    3    4           5       6     7           8  9              10                       11 12     13       14
            // 1504   1.3  0.4  1.3  chr1        10001   10468 (249240153) +  (CCCTAA)n      Simple_repeat            1  463    (0)      1
            fileReader.readLine();
            fileReader.readLine();
            fileReader.readLine();

            while((line = fileReader.readLine()) != null)
            {
                final String[] values = line.trim().split("\\s{1,}", -1);

                String chromosome = refGenomeVersion.versionedChromosome(values[4]);

                if(!HumanChromosome.contains(chromosome))
                    continue;

                if(!chromosome.equals(currentChr))
                {
                    currentChr = chromosome;
                    entries = Lists.newArrayList();
                    mChrDataMap.put(chromosome, entries);
                }

                try
                {
                    // note BED start position adjustment
                    BaseRegion region = new BaseRegion(Integer.parseInt(values[5]) + 1, Integer.parseInt(values[6]));
                    int id = Integer.parseInt(values[14]);
                    int swScore = Integer.parseInt(values[0]);
                    char orientation = values[8].charAt(0);
                    String classType = values[10];
                    String repeat = values[9];

                    entries.add(new RepeatMaskData(id, region, swScore, orientation, repeat, classType));
                }
                catch(Exception e)
                {
                    GR_LOGGER.error("invalid RM file entry: index({}) line({})", line, index);
                    return false;
                }

                ++index;
            }

            GR_LOGGER.info("loaded {} repeat-mask entries from file({})",
                    mChrDataMap.values().stream().mapToInt(x -> x.size()).sum(), filename);
        }
        catch(IOException e)
        {
            GR_LOGGER.error("failed to load repeat-mask data from file({}): {}", filename, e.toString());
            return false;
        }

        return true;
    }
}
