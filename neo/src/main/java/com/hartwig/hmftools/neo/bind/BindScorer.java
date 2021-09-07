package com.hartwig.hmftools.neo.bind;

import static com.hartwig.hmftools.common.utils.ConfigUtils.setLogLevel;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.createBufferedWriter;
import static com.hartwig.hmftools.neo.NeoCommon.NE_LOGGER;
import static com.hartwig.hmftools.neo.bind.BindData.loadBindData;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.stats.AucCalc;
import com.hartwig.hmftools.common.stats.AucData;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.NotNull;

public class BindScorer
{
    private final ScoreConfig mConfig;

    private final Map<String,Map<Integer,List<BindData>>> mAllelePeptideData;
    private final Map<String,Map<Integer,BindScoreMatrix>> mAlleleBindMatrices;
    private final FlankScores mFlankScores;

    private final RandomPeptideDistribution mRandomDistribution;
    private final BindingLikelihood mBindingLikelihood;
    private final RecognitionSimilarity mRecognitionSimilarity;

    private final Map<String,Integer> mBindDataOtherColumns;

    public BindScorer(final ScoreConfig config)
    {
        mConfig = config;

        mAllelePeptideData = Maps.newHashMap();
        mBindDataOtherColumns = Maps.newLinkedHashMap();

        mAlleleBindMatrices = Maps.newHashMap();

        mRandomDistribution = new RandomPeptideDistribution(config.RandomPeptides);
        mBindingLikelihood = new BindingLikelihood();
        mFlankScores = new FlankScores();
        mRecognitionSimilarity = new RecognitionSimilarity();
    }

    public BindScorer(
            final ScoreConfig config, final Map<String,Map<Integer,List<BindData>>> allelePeptideData,
            final Map<String,Map<Integer,BindScoreMatrix>> alleleBindMatrices, final RandomPeptideDistribution randomDistribution,
            final FlankScores flankScores)
    {
        mConfig = config;

        mAllelePeptideData = allelePeptideData;
        mBindDataOtherColumns = Maps.newLinkedHashMap();

        mAlleleBindMatrices = alleleBindMatrices;
        mRandomDistribution = randomDistribution;
        mBindingLikelihood = null;
        mFlankScores = flankScores;
        mRecognitionSimilarity = null;
    }

    public void run()
    {
        NE_LOGGER.info("running BindScorer");

        if(!loadScoringData())
        {
            NE_LOGGER.error("invalid reference data");
            return;
        }

        if(!loadBindData(mConfig.ValidationDataFile, Lists.newArrayList(), mAllelePeptideData, mBindDataOtherColumns))
        {
            NE_LOGGER.error("invalid validation data");
            return;
        }

        runScoring();

        NE_LOGGER.info("scoring complete");
    }

    public static final double INVALID_CALC = -1;

    public double calcScore(final String allele, final String peptide)
    {
        Map<Integer,BindScoreMatrix> pepLenMatrixMap = mAlleleBindMatrices.get(allele);
        if(pepLenMatrixMap == null)
            return INVALID_CALC;

        BindScoreMatrix matrix = pepLenMatrixMap.get(peptide.length());

        if(matrix == null)
            return INVALID_CALC;

        return matrix.calcScore(peptide);
    }

    public double calcScoreRank(final String allele, final String peptide, double score)
    {
        return mRandomDistribution.getScoreRank(allele, peptide.length(), score);
    }

    public double calcLikelihood(final String allele, final String peptide, double rank)
    {
        if(mBindingLikelihood == null || !mBindingLikelihood.hasData())
            return INVALID_CALC;

        return mBindingLikelihood.getBindingLikelihood(allele, peptide, rank);
    }

    public void runScoring()
    {
        NE_LOGGER.info("running scoring");

        for(Map.Entry<String,Map<Integer,List<BindData>>> alleleEntry : mAllelePeptideData.entrySet())
        {
            final String allele = alleleEntry.getKey();
            final Map<Integer,List<BindData>> pepLenBindDataMap = alleleEntry.getValue();

            Map<Integer,BindScoreMatrix> pepLenMatrixMap = mAlleleBindMatrices.get(allele);

            if(pepLenMatrixMap == null)
            {
                NE_LOGGER.warn("allele({}) has no matrix scoring data", allele);
                continue;
            }

            TprCalc alleleTprCalc = new TprCalc();

            for(Map.Entry<Integer,List<BindData>> pepLenEntry : pepLenBindDataMap.entrySet())
            {
                final List<BindData> bindDataList = pepLenEntry.getValue();

                for(BindData bindData : bindDataList)
                {
                    BindScoreMatrix matrix = pepLenMatrixMap.get(bindData.peptideLength());

                    if(matrix == null)
                        continue;

                    calcScoreData(bindData, matrix, mFlankScores, mRandomDistribution, mBindingLikelihood);

                    alleleTprCalc.addRank(bindData.likelihoodRank());
                }
            }
        }

        writeAlleleSummary();
        writePeptideScores();
    }

    public static double calcScore(
            final BindScoreMatrix matrix, final FlankScores flankScores, final String peptide, final String upFlank, final String downFlank)
    {
        double score = matrix.calcScore(peptide);

        if(flankScores.hasData())
        {
            double flankScore = flankScores.calcScore(upFlank, downFlank);
            score += flankScore;
        }

        return score;
    }

    public void calcScoreData(final BindData bindData)
    {
        if(!mAlleleBindMatrices.containsKey(bindData.Allele))
            return;

        BindScoreMatrix matrix = mAlleleBindMatrices.get(bindData.Allele).get(bindData.peptideLength());

        if(matrix == null)
            return;

        calcScoreData(bindData, matrix, mFlankScores, mRandomDistribution, mBindingLikelihood);
    }

    public static void calcScoreData(
            final BindData bindData, final BindScoreMatrix matrix, final FlankScores flankScores,
            final RandomPeptideDistribution randomDistribution, final BindingLikelihood bindingLikelihood)
    {
        double score = matrix.calcScore(bindData.Peptide);

        double flankScore = 0;
        if(flankScores.hasData() && bindData.hasFlanks())
        {
            flankScore = flankScores.calcScore(bindData.UpFlank, bindData.DownFlank);
            score += flankScore;
        }

        double rankPercentile = randomDistribution.getScoreRank(bindData.Allele, bindData.peptideLength(), score);

        double likelihood = bindingLikelihood != null && bindingLikelihood.hasData() ?
                bindingLikelihood.getBindingLikelihood(bindData.Allele, bindData.Peptide, rankPercentile) : 0;

        double likelihoodRank = randomDistribution.getLikelihoodRank(bindData.Allele, likelihood);

        bindData.setScoreData(score, flankScore, rankPercentile, likelihood, likelihoodRank);
    }

    private void writePeptideScores()
    {
        if(!mConfig.WritePeptideScores)
            return;

        String outputFile = mConfig.formOutputFilename("peptide_scores");
        NE_LOGGER.info("writing peptide scores to {}", outputFile);

        try
        {
            BufferedWriter writer = createBufferedWriter(outputFile, false);
            writer.write("Allele,Peptide,Source,Score,Rank,Likelihood,LikelihoodRank");

            boolean writeFlanks = mFlankScores.hasData() && mAllelePeptideData.values().stream().anyMatch(x -> x.values().stream()
                    .filter(y -> !y.isEmpty()).anyMatch(y -> y.get(0).hasFlanks()));

            if(writeFlanks)
                writer.write(",FlankScore,UpFlank,DownFlank");

            boolean calcRecognitionSim = mRecognitionSimilarity != null && mRecognitionSimilarity.hasData();

            if(calcRecognitionSim)
                writer.write(",RecogSim");

            boolean hasOtherData = !mBindDataOtherColumns.isEmpty();

            if(hasOtherData)
            {
                for(String column : mBindDataOtherColumns.keySet())
                {
                    writer.write(String.format(",%s", column));
                }
            }

            writer.newLine();

            for(Map.Entry<String,Map<Integer,List<BindData>>> alleleEntry : mAllelePeptideData.entrySet())
            {
                final String allele = alleleEntry.getKey();
                final Map<Integer,List<BindData>> pepLenBindDataMap = alleleEntry.getValue();

                for(List<BindData> bindDataList : pepLenBindDataMap.values())
                {
                    for(BindData bindData : bindDataList)
                    {
                        writer.write(String.format("%s,%s,%s,%.4f,%.6f,%.6f,%.6f",
                                allele, bindData.Peptide, bindData.Source, bindData.score(),
                                bindData.rankPercentile(), bindData.likelihood(), bindData.likelihoodRank()));

                        if(writeFlanks)
                        {
                            writer.write(String.format(",%.4f,%s,%s", bindData.flankScore(), bindData.UpFlank, bindData.DownFlank));
                        }

                        if(calcRecognitionSim)
                        {
                            writer.write(String.format(",%.1f", mRecognitionSimilarity.calcSimilarity(bindData.Allele, bindData.Peptide)));
                        }

                        if(hasOtherData)
                        {
                            for(Integer columnIndex : mBindDataOtherColumns.values())
                            {
                                writer.write(String.format(",%s", bindData.getOtherData().get(columnIndex)));
                            }
                        }

                        writer.newLine();
                    }
                }
            }

            writer.close();
        }
        catch(IOException e)
        {
            NE_LOGGER.error("failed to write peptide scores file: {}", e.toString());
        }
    }

    private void writeAlleleSummary()
    {
        if(!mConfig.WriteSummaryData)
            return;

        try
        {
            BufferedWriter writer = createBufferedWriter(mConfig.formOutputFilename("allele_summary"), false);
            writer.write("Allele,PeptideLength,BindCount,TPR,AUC");
            writer.newLine();

            for(Map.Entry<String,Map<Integer,List<BindData>>> alleleEntry : mAllelePeptideData.entrySet())
            {
                final String allele = alleleEntry.getKey();

                final Map<Integer,List<BindData>> pepLenBindDataMap = alleleEntry.getValue();

                List<AucData> alleleAucData = Lists.newArrayList();
                TprCalc alleleTprCalc = new TprCalc();

                for(Map.Entry<Integer,List<BindData>> pepLenEntry : pepLenBindDataMap.entrySet())
                {
                    int peptideLength = pepLenEntry.getKey();
                    final List<BindData> bindDataList = pepLenEntry.getValue();

                    TprCalc pepLenTprCalc = new TprCalc();

                    for(BindData bindData : bindDataList)
                    {
                        pepLenTprCalc.addRank(bindData.likelihoodRank());
                        alleleTprCalc.addRank(bindData.likelihoodRank());

                        alleleAucData.add(new AucData(true, bindData.likelihoodRank(), true));
                    }

                    writer.write(String.format("%s,%d,%d,%.4f,0",
                            allele, peptideLength, pepLenTprCalc.entryCount(), pepLenTprCalc.calc()));
                    writer.newLine();
                }

                double aucPerc = AucCalc.calcPercentilesAuc(alleleAucData, Level.TRACE);

                writer.write(String.format("%s,ALL,%d,%.4f,%.4f",
                        allele, alleleTprCalc.entryCount(), alleleTprCalc.calc(), aucPerc));
                writer.newLine();

            }

            writer.close();
        }
        catch(IOException e)
        {
            NE_LOGGER.error("failed to init allele summary writer: {}", e.toString());
        }
    }

    public boolean loadScoringData()
    {
        List<BindScoreMatrix> matrixList = BindScoreMatrix.loadFromCsv(mConfig.PosWeightsFile);

        for(BindScoreMatrix matrix : matrixList)
        {
            Map<Integer,BindScoreMatrix> pepLenMap = mAlleleBindMatrices.get(matrix.Allele);

            if(pepLenMap == null)
            {
                pepLenMap = Maps.newHashMap();
                mAlleleBindMatrices.put(matrix.Allele, pepLenMap);
            }

            pepLenMap.put(matrix.PeptideLength, matrix);
        }

        if(!mRandomDistribution.loadData())
            return false;

        if(mConfig.BindLikelihoodFile != null && !mBindingLikelihood.loadLikelihoods(mConfig.BindLikelihoodFile))
            return false;

        if(mConfig.FlankPosWeightsFile != null && Files.exists(Paths.get(mConfig.FlankPosWeightsFile)))
        {
            if(!mFlankScores.loadPosWeights(mConfig.FlankPosWeightsFile))
                return false;
        }

        if(mConfig.RecognitionDataFile != null && !mRecognitionSimilarity.loadData(mConfig.RecognitionDataFile))
            return false;

        return true;
    }

    public static void main(@NotNull final String[] args) throws ParseException
    {
        final Options options = new Options();

        ScoreConfig.addCmdLineArgs(options);

        final CommandLine cmd = createCommandLine(args, options);

        setLogLevel(cmd);

        BindScorer bindScorer = new BindScorer(new ScoreConfig(cmd));
        bindScorer.run();
    }

    @NotNull
    public static CommandLine createCommandLine(@NotNull final String[] args, @NotNull final Options options) throws ParseException
    {
        final CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }
}
