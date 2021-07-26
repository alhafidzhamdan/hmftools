package com.hartwig.hmftools.neo.bind;

import static java.lang.Math.max;
import static java.lang.Math.round;

import static com.hartwig.hmftools.common.neo.NeoEpitopeFile.DELIMITER;
import static com.hartwig.hmftools.common.utils.ConfigUtils.setLogLevel;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.closeBufferedWriter;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.createBufferedWriter;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.createFieldsIndexMap;
import static com.hartwig.hmftools.neo.NeoCommon.NE_LOGGER;
import static com.hartwig.hmftools.neo.bind.ScoringData.initMatrixWriter;
import static com.hartwig.hmftools.neo.bind.ScoringData.initPairDataWriter;
import static com.hartwig.hmftools.neo.bind.ScoringData.initFrequencyWriter;
import static com.hartwig.hmftools.neo.utils.AminoAcidFrequency.AMINO_ACID_FREQ_FILE;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.neo.utils.AminoAcidFrequency;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jetbrains.annotations.NotNull;

public class BindTrainer
{
    private final BinderConfig mConfig;

    private final Map<String,List<BindData>> mAlleleBindData;
    private final AminoAcidFrequency mAminoAcidFrequency;
    private int mMaxPeptideLength;

    public BindTrainer(final CommandLine cmd)
    {
        mConfig = new BinderConfig(cmd);
        mAlleleBindData = Maps.newHashMap();
        mMaxPeptideLength = 0;

        mAminoAcidFrequency = new AminoAcidFrequency(cmd);
        mAminoAcidFrequency.loadFrequencies();
    }

    public void run()
    {
        NE_LOGGER.info("running NeoBinder on {} alleles", mConfig.SpecificAlleles.isEmpty() ? "all" : mConfig.SpecificAlleles.size());

        if(!loadTrainingData(mConfig.TrainingDataFile))
        {
            System.exit(1);
        }

        if(mConfig.WriteScoreMatrix && mAminoAcidFrequency.getAminoAcidFrequencies().isEmpty())
        {
            NE_LOGGER.warn("no amino acid frequencies loaded");
        }

        processingBindingData();

        NE_LOGGER.info("NeoBinder complete");
    }

    private void processingBindingData()
    {
        BufferedWriter matrixWriter = mConfig.WriteScoreMatrix ?
                initMatrixWriter(mConfig.formFilename("score_matrix"), mMaxPeptideLength) : null;

        BufferedWriter freqWriter = mConfig.WriteFrequencyData ?
                initFrequencyWriter(mConfig.formFilename("single_freq_score")) : null;

        BufferedWriter pairWriter = mConfig.CalcPairs ? initPairDataWriter(mConfig.formFilename("pair_score_prob")) : null;

        List<BindScoreMatrix> matrixList = Lists.newArrayList();

        for(Map.Entry<String,List<BindData>> entry : mAlleleBindData.entrySet())
        {
            String allele = entry.getKey();

            Map<Integer,ScoringData> matrixMap = Maps.newHashMap();
            int currentLength = -1;
            ScoringData currentData = null;

            for(BindData bindData : entry.getValue())
            {
                int peptideLength = bindData.Peptide.length();

                if(currentLength != peptideLength)
                {
                    currentLength = peptideLength;
                    currentData = matrixMap.get(peptideLength);

                    if(currentData == null)
                    {
                        currentData = new ScoringData(allele, peptideLength, mConfig.Constants);
                        matrixMap.put(peptideLength, currentData);
                    }
                }

                currentData.processBindData(bindData, mConfig.CalcPairs);
            }

            for(ScoringData scoringData : matrixMap.values())
            {
                // write results
                scoringData.logStats();

                BindScoreMatrix matrix = scoringData.createMatrix(mAminoAcidFrequency);
                matrixList.add(matrix);

                if(mConfig.WriteScoreMatrix)
                    scoringData.writeMatrixData(matrixWriter, matrix, mMaxPeptideLength);

                if(mConfig.WriteFrequencyData)
                    scoringData.writeFrequencyData(freqWriter);

                if(mConfig.CalcPairs)
                    scoringData.writePairData(allele, pairWriter);
            }
        }

        RandomPeptideDistribution randomDistribution = new RandomPeptideDistribution(mConfig);

        if(!randomDistribution.hasData())
        {
            randomDistribution.buildDistribution(matrixList);
        }

        // rank the training data peptides using the newly created data and the random peptide distributions
        try
        {
            BufferedWriter writer = createBufferedWriter(mConfig.formFilename("training_scores"), false);

            writer.write("Allele,Peptide,Score,Rank,Affinity,PredictedAffinity");
            writer.newLine();

            for(Map.Entry<String, List<BindData>> entry : mAlleleBindData.entrySet())
            {
                String allele = entry.getKey();

                BindScoreMatrix matrix = matrixList.stream().filter(x -> x.Allele.equals(allele)).findFirst().orElse(null);

                for(BindData bindData : entry.getValue())
                {
                    if(bindData.OtherInfo.equals("Random"))
                        continue;

                    double peptideScore = matrix.calcScore(bindData.Peptide);
                    double scoreRank = randomDistribution.getScoreRank(allele, peptideScore);

                    writer.write(String.format("%s,%s,%.4f,%.4f,%.1f,%.1f",
                            allele, bindData.Peptide, peptideScore, scoreRank, bindData.Affinity, bindData.PredictedAffinity));
                    writer.newLine();
                }
            }

            writer.close();
        }
        catch(IOException e)
        {
            NE_LOGGER.error("failed to write peptide scores file: {}", e.toString());
        }

        closeBufferedWriter(matrixWriter);
        closeBufferedWriter(freqWriter);
        closeBufferedWriter(pairWriter);
    }

    private boolean loadTrainingData(final String filename)
    {
        try
        {
            final List<String> lines = Files.readAllLines(new File(filename).toPath());

            final Map<String,Integer> fieldsIndexMap = createFieldsIndexMap(lines.get(0), DELIMITER);
            lines.remove(0);

            int alleleIndex = fieldsIndexMap.get("Allele");
            int peptideIndex = fieldsIndexMap.get("Peptide");
            int affinityIndex = fieldsIndexMap.get("Affinity");
            int predictedIndex = fieldsIndexMap.get("PredAffinity");
            int otherInfoIndex = fieldsIndexMap.get("OtherInfo");

            String currentAllele = "";
            List<BindData> currentBindList = null;

            for(String line : lines)
            {
                final String[] items = line.split(DELIMITER, -1);

                String allele = items[alleleIndex];

                if(!mConfig.SpecificAlleles.isEmpty() && !mConfig.SpecificAlleles.contains(allele))
                {
                    if(mConfig.SpecificAlleles.size() == mAlleleBindData.size())
                        break;

                    continue;
                }

                BindData bindData = BindData.fromCsv(line, alleleIndex, peptideIndex, affinityIndex, predictedIndex, otherInfoIndex);

                if(bindData.Peptide.contains("X"))
                    continue;

                if(!allele.equals(currentAllele))
                {
                    currentAllele = allele;
                    currentBindList = Lists.newArrayList();
                    mAlleleBindData.put(allele, currentBindList);
                }

                currentBindList.add(bindData);

                mMaxPeptideLength = max(mMaxPeptideLength, bindData.Peptide.length());
            }

            NE_LOGGER.info("loaded {} alleles with {} training data items from file({}) maxPeptideLength({})",
                    mAlleleBindData.size(), mAlleleBindData.values().stream().mapToInt(x -> x.size()).sum(), filename, mMaxPeptideLength);
        }
        catch(IOException e)
        {
            NE_LOGGER.error("failed to read training binding data file: {}", e.toString());
            return false;
        }

        return true;
    }

    public static void main(@NotNull final String[] args) throws ParseException
    {
        final Options options = new Options();

        BinderConfig.addCmdLineArgs(options);
        options.addOption(AMINO_ACID_FREQ_FILE, true, "Amino acid frequency from proteome");

        final CommandLine cmd = createCommandLine(args, options);

        setLogLevel(cmd);

        BindTrainer neoBinder = new BindTrainer(cmd);
        neoBinder.run();
    }

    @NotNull
    public static CommandLine createCommandLine(@NotNull final String[] args, @NotNull final Options options) throws ParseException
    {
        final CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

}
