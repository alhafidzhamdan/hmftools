package com.hartwig.hmftools.isofox.unmapped;

import static com.hartwig.hmftools.common.rna.RnaCommon.FLD_CHROMOSOME;
import static com.hartwig.hmftools.common.rna.RnaCommon.FLD_GENE_NAME;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.createFieldsIndexMap;
import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.SE_END;
import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.SE_START;
import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.START_STR;
import static com.hartwig.hmftools.isofox.IsofoxConfig.ISF_LOGGER;
import static com.hartwig.hmftools.isofox.cohort.AnalysisType.UNMAPPED_READS;
import static com.hartwig.hmftools.isofox.cohort.CohortConfig.formSampleFilenames;
import static com.hartwig.hmftools.isofox.results.ResultsWriter.DELIMITER;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.utils.sv.ChrBaseRegion;
import com.hartwig.hmftools.isofox.cohort.CohortConfig;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public class UnmappedReadsAnalyser
{
    private final CohortConfig mConfig;

    // other config
    private final int mMinSampleThreshold;
    private final int mMinFragments;

    // map of chromosomes to a map of splice-boundary keys to a map of samples to a list of unmapped reads
    private final Map<String,Map<String,Map<String,List<UnmappedRead>>>> mUnmappedReads;

    private static final String UMR_MIN_SAMPLES = "umr_min_samples";
    private static final String UMR_MIN_FRAGS = "umr_min_frags";

    public UnmappedReadsAnalyser(final CohortConfig config, final CommandLine cmd)
    {
        mConfig = config;
        mUnmappedReads = Maps.newHashMap();

        mMinSampleThreshold = Integer.parseInt(cmd.getOptionValue(UMR_MIN_SAMPLES, "0"));
        mMinFragments = Integer.parseInt(cmd.getOptionValue(UMR_MIN_FRAGS, "0"));
    }

    public static void addCmdLineOptions(final Options options)
    {
        options.addOption(UMR_MIN_SAMPLES, true, "Min number of samples to report an unmapped read");
        options.addOption(UMR_MIN_FRAGS, true, "Min frag count ...");
    }

    public void processSampleFiles()
    {
        final List<Path> filenames = Lists.newArrayList();

        if(!formSampleFilenames(mConfig, UNMAPPED_READS, filenames))
            return;

        int totalProcessed = 0;
        int nextLog = 100000;

        // load each sample's alt SJs and consolidate into a single list
        for(int i = 0; i < mConfig.SampleData.SampleIds.size(); ++i)
        {
            final String sampleId = mConfig.SampleData.SampleIds.get(i);
            final Path umrFile = filenames.get(i);

            loadFile(sampleId, umrFile);

            int totalUmrCount = mUnmappedReads.values().stream().mapToInt(x -> x.values().stream().mapToInt(y -> y.size()).sum()).sum();

            if(totalUmrCount >= nextLog)
            {
                ISF_LOGGER.debug("cached unmapped-read count({})", totalUmrCount);
                nextLog += 100000;
            }
        }

        ISF_LOGGER.info("loaded {} unmapped-read records", totalProcessed);
    }

    private void loadFile(final String sampleId, final Path filename)
    {
        try
        {
            final List<String> lines = Files.readAllLines(filename);

            Map<String,Integer> fieldsIndexMap = createFieldsIndexMap(lines.get(0), DELIMITER);

            lines.remove(0);

            // int geneId = fieldsIndexMap.get(FLD_GENE_ID);
            int geneNameIndex = fieldsIndexMap.get(FLD_GENE_NAME);
            int chrIndex = fieldsIndexMap.get(FLD_CHROMOSOME);
            int posStartIndex = fieldsIndexMap.get("PosStart");
            int readIndex = fieldsIndexMap.get("ReadId");
            int posEndIndex = fieldsIndexMap.get("PosEnd");
            int spliceTypeIndex = fieldsIndexMap.get("SpliceType");
            int orientIndex = fieldsIndexMap.get("Orientation");
            int scLengthIndex = fieldsIndexMap.get("SoftClipLength");
            int scSideIndex = fieldsIndexMap.get("SoftClipSide");
            int abqIndex = fieldsIndexMap.get("AvgBaseQual");
            int transIndex = fieldsIndexMap.get("TransName");
            int exonRankIndex = fieldsIndexMap.get("ExonRank");
            int exonBoundaryIndex = fieldsIndexMap.get("ExonBoundary");
            int exonDistIndex = fieldsIndexMap.get("ExonDistance");
            int scBasesIndex = fieldsIndexMap.get("SoftClipBases");

            for(String data : lines)
            {
                final String[] values = data.split(DELIMITER);

                UnmappedRead umRead = new UnmappedRead(
                        values[readIndex],
                        new ChrBaseRegion(values[chrIndex], Integer.parseInt(values[posStartIndex]), Integer.parseInt(values[posEndIndex])),
                        Byte.parseByte(values[orientIndex]), Integer.parseInt(values[scLengthIndex]),
                        values[scSideIndex].equals(START_STR) ? SE_START : SE_END,
                        Double.parseDouble(values[abqIndex]), values[geneNameIndex], values[transIndex],
                        Integer.parseInt(values[exonRankIndex]), Integer.parseInt(values[exonBoundaryIndex]),
                        Integer.parseInt(values[exonDistIndex]), values[spliceTypeIndex], values[scBasesIndex]);

                addUnmappedRead(sampleId, umRead);
            }

            ISF_LOGGER.debug("sample({}) loaded {} unmapped-read records", sampleId, lines.size());
        }
        catch(IOException e)
        {
            ISF_LOGGER.error("failed to load unmapped-read file({}): {}", filename.toString(), e.toString());
        }
    }

    private void addUnmappedRead(final String sampleId, final UnmappedRead umRead)
    {
        Map<String,Map<String,List<UnmappedRead>>> chrUmrs = mUnmappedReads.get(umRead.ReadRegion.Chromosome);

        if(chrUmrs == null)
        {
            chrUmrs = Maps.newHashMap();
            mUnmappedReads.put(umRead.ReadRegion.Chromosome, chrUmrs);
        }

        String umrKey = umRead.formKey();
        Map<String,List<UnmappedRead>> umrKeyList = chrUmrs.get(umrKey);

        if(umrKeyList == null)
        {
            umrKeyList = Maps.newHashMap();
            chrUmrs.put(umRead.GeneName, umrKeyList);
        }

        List<UnmappedRead> sampleReads = umrKeyList.get(sampleId);

        if(sampleReads == null)
        {
            sampleReads = Lists.newArrayList();
            umrKeyList.put(sampleId, sampleReads);
        }

        sampleReads.add(umRead);
    }

}
