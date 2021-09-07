package com.hartwig.hmftools.neo.cohort;

import static com.hartwig.hmftools.common.neo.NeoEpitopeFile.ITEM_DELIM;
import static com.hartwig.hmftools.common.utils.ConfigUtils.LOG_DEBUG;
import static com.hartwig.hmftools.common.utils.ConfigUtils.setLogLevel;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.OUTPUT_DIR;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.closeBufferedWriter;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.createBufferedWriter;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.parseOutputDir;
import static com.hartwig.hmftools.neo.NeoCommon.NE_LOGGER;
import static com.hartwig.hmftools.neo.NeoCommon.loadSampleIdsFile;
import static com.hartwig.hmftools.neo.cohort.DataLoader.loadNeoEpitopes;
import static com.hartwig.hmftools.neo.cohort.DataLoader.loadPredictionData;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jetbrains.annotations.NotNull;

public class PeptidePredictionAnalyser
{
    private final String mOutputDir;
    private final String mNeoDataDir;
    private final String mPredictionsDataDir;
    private final List<String> mSampleIds;
    private final List<String> mSpecificAlleles;

    private BufferedWriter mPeptideWriter;

    private final int mPeptideAffinityThreshold;

    public static final String SAMPLE_ID_FILE = "sample_id_file";
    public static final String NEO_DATA_DIR = "neo_data_dir";
    public static final String PREDICTION_DATA_DIR = "prediction_data_dir";

    private static final String PEPTIDE_AFF_THRESHOLD = "pep_aff_threshold";
    private static final String SPECIFIC_ALLELES = "specific_alleles";

    public PeptidePredictionAnalyser(final CommandLine cmd)
    {
        mSampleIds = Lists.newArrayList();
        loadSampleIdsFile(cmd.getOptionValue(SAMPLE_ID_FILE), mSampleIds);

        mNeoDataDir = cmd.getOptionValue(NEO_DATA_DIR);
        mPredictionsDataDir = cmd.getOptionValue(PREDICTION_DATA_DIR);

        mPeptideAffinityThreshold = Integer.parseInt(cmd.getOptionValue(PEPTIDE_AFF_THRESHOLD, "0"));
        mSpecificAlleles = Lists.newArrayList();

        if(cmd.hasOption(SPECIFIC_ALLELES))
        {
            Arrays.stream(cmd.getOptionValue(SPECIFIC_ALLELES).split(ITEM_DELIM, -1)).forEach(x -> mSpecificAlleles.add(x));
            NE_LOGGER.info("filtering for {} alleles: {}", mSpecificAlleles.size(), mSpecificAlleles);
        }

        mOutputDir = parseOutputDir(cmd);
        mPeptideWriter = null;
        initialisePeptideWriter();
    }

    public void run()
    {
        if(mSampleIds.isEmpty())
            return;

        NE_LOGGER.info("processing {} samples", mSampleIds.size());

        // check required inputs and config
        int processed = 0;

        for(final String sampleId : mSampleIds)
        {
            processSample(sampleId);
            ++processed;

            if(processed > 0 && (processed % 100) == 0)
            {
                NE_LOGGER.info("processed {} samples", processed);
            }
        }

        closeBufferedWriter(mPeptideWriter);
    }

    private void processSample(final String sampleId)
    {
        List<BindingPredictionData> predictions = loadPredictionData(sampleId, mPredictionsDataDir);
        Map<Integer,NeoEpitopeData> neoDataMap = loadNeoEpitopes(sampleId, mNeoDataDir);

        // organise into map by peptide, avoiding repeated peptides
        for(int i = 0; i < predictions.size(); ++i)
        {
            BindingPredictionData predData = predictions.get(i);

            if(!mSpecificAlleles.isEmpty() && !mSpecificAlleles.contains(predData.Allele))
                continue;

            if(predData.affinity() > mPeptideAffinityThreshold)
                continue;

            NeoEpitopeData neoData = neoDataMap.get(predData.NeId);

            if(neoData == null)
            {
                NE_LOGGER.warn("sample({}) neId({}) neo-data not found", sampleId, predData.NeId);
                continue;
            }

            writePeptideData(sampleId, predData, neoData);
        }
    }

    private void initialisePeptideWriter()
    {
        try
        {
            final String outputFileName = String.format("%sNEO_PREDICTIONS_%d.csv", mOutputDir, mPeptideAffinityThreshold);

            mPeptideWriter = createBufferedWriter(outputFileName, false);
            mPeptideWriter.write("SampleId,NeId,VarType,VarInfo,Genes");
            mPeptideWriter.write(",Peptide,Allele,Affinity,Presentation");
            mPeptideWriter.newLine();
        }
        catch (IOException e)
        {
            NE_LOGGER.error("failed to create peptide writer: {}", e.toString());
        }
    }

    private void writePeptideData(final String sampleId, final BindingPredictionData prediction, final NeoEpitopeData neoData)
    {
        try
        {
            mPeptideWriter.write(String.format("%s,%d,%s,%s,%s",
                    sampleId, neoData.Id, neoData.VariantType, neoData.VariantInfo, neoData.GeneName));

            mPeptideWriter.write(String.format(",%s,%s,%.1f,%.4f",
                    prediction.Peptide, prediction.Allele, prediction.affinity(), prediction.presentation()));

            mPeptideWriter.newLine();
        }
        catch (IOException e)
        {
            NE_LOGGER.error("failed to write neoepitope prediction data: {}", e.toString());
        }
    }

    public static void addCmdLineArgs(Options options)
    {
        options.addOption(SAMPLE_ID_FILE, true, "SampleId file");
        options.addOption(NEO_DATA_DIR, true, "Directory for sample neo-epitope files");
        options.addOption(PREDICTION_DATA_DIR, true, "Directory for sample prediction result files");
        options.addOption(OUTPUT_DIR, true, "Output directory");
        options.addOption(LOG_DEBUG, false, "Log verbose");
        options.addOption(PEPTIDE_AFF_THRESHOLD, true, "Only write peptides with affinity less than this if > 0");
        options.addOption(SPECIFIC_ALLELES, true, "Specific alleles to filter for, separated by ';'");
    }

    public static void main(@NotNull final String[] args) throws ParseException
    {
        final Options options = new Options();

        PeptidePredictionAnalyser.addCmdLineArgs(options);

        final CommandLine cmd = createCommandLine(args, options);

        setLogLevel(cmd);

        PeptidePredictionAnalyser samplePeptidePredictions = new PeptidePredictionAnalyser(cmd);
        samplePeptidePredictions.run();

        NE_LOGGER.info("cohort peptide predictions complete");
    }

    @NotNull
    public static CommandLine createCommandLine(@NotNull final String[] args, @NotNull final Options options) throws ParseException
    {
        final CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

}
