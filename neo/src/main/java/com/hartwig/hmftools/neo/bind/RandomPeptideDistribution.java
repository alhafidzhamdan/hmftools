package com.hartwig.hmftools.neo.bind;

import static com.hartwig.hmftools.common.utils.FileWriterUtils.createBufferedWriter;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.createFieldsIndexMap;
import static com.hartwig.hmftools.neo.NeoCommon.NE_LOGGER;
import static com.hartwig.hmftools.neo.bind.BindCommon.DELIM;
import static com.hartwig.hmftools.neo.bind.BindConstants.INVALID_SCORE;
import static com.hartwig.hmftools.neo.bind.RandomPeptideData.loadRandomPeptides;
import static com.hartwig.hmftools.neo.bind.TrainConfig.FILE_ID_LIKELIHOOD_RAND_DIST;
import static com.hartwig.hmftools.neo.bind.TrainConfig.FILE_ID_RAND_DIST;
import static com.hartwig.hmftools.neo.bind.TrainConfig.formTrainingFilename;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.utils.TaskExecutor;
import com.hartwig.hmftools.common.utils.Doubles;

public class RandomPeptideDistribution
{
    private final RandomPeptideConfig mConfig;
    private boolean mDataLoaded;

    private final Map<Integer,List<RandomPeptideData>> mRandomPeptideMap; // by length and with flanking data

    private final Map<String,Map<Integer,List<ScoreDistributionData>>> mAlleleScoreDistributions; // allele to peptide length to distribution
    private final Map<String,List<ScoreDistributionData>> mAlleleLikelihoodDistributions; // allele to distribution of likelihoods

    public RandomPeptideDistribution(final RandomPeptideConfig config)
    {
        mConfig = config;

        mAlleleScoreDistributions = Maps.newHashMap();
        mAlleleLikelihoodDistributions = Maps.newHashMap();
        mRandomPeptideMap = Maps.newHashMap();
        mDataLoaded = false;
    }

    public boolean loadData()
    {
        mDataLoaded = loadDistribution() && loadLikelihoodDistribution();
        return mDataLoaded;
    }

    public boolean hasData() { return mDataLoaded; }

    public Map<String,Map<Integer,List<ScoreDistributionData>>> getAlleleScoresMap() { return mAlleleScoreDistributions; }

    public double getScoreRank(final String allele, final int peptideLength, double score)
    {
        return getScoreRank(mAlleleScoreDistributions, allele, peptideLength, score);
    }

    public static double getScoreRank(
            final Map<String,Map<Integer,List<ScoreDistributionData>>> scoresMap, final String allele, final int peptideLength, double score)
    {
        Map<Integer,List<ScoreDistributionData>> peptideLengthMap = scoresMap.get(allele);

        if(peptideLengthMap == null)
            return INVALID_SCORE;

        List<ScoreDistributionData> scores = peptideLengthMap.get(peptideLength);

        if(scores == null || scores.size() < 2)
            return INVALID_SCORE;

        return getRank(scores, score);
    }

    public double getLikelihoodRank(final String allele, double likelihood)
    {
        List<ScoreDistributionData> likelihoodDist = mAlleleLikelihoodDistributions.get(allele);

        if(likelihoodDist == null || likelihoodDist.size() < 2)
            return INVALID_SCORE;

        return getRank(likelihoodDist, likelihood);
    }

    private static double getRank(final List<ScoreDistributionData> distribution, double score)
    {
        boolean isAscending = distribution.get(0).Score < distribution.get(1).Score;

        if((isAscending && score < distribution.get(0).Score) || (!isAscending && score > distribution.get(0).Score))
            return 0; // zero-th percentile if the score is better than any in the random distribution

        for(int i = 0; i < distribution.size(); ++i)
        {
            ScoreDistributionData scoreData = distribution.get(i);

            if(Doubles.equal(score, scoreData.Score))
                return scoreData.ScoreBucket;

            ScoreDistributionData nextScoreData = i < distribution.size() - 1 ? distribution.get(i + 1) : null;

            if(nextScoreData != null && Doubles.equal(score, nextScoreData.Score))
                return nextScoreData.ScoreBucket;

            if((isAscending && score > scoreData.Score) || (!isAscending && score < scoreData.Score))
            {
                if(nextScoreData == null)
                    break;

                if((isAscending && score < nextScoreData.Score) || (!isAscending && score > nextScoreData.Score))
                {
                    // interpolate between the distribution to set the rank
                    if(isAscending)
                    {
                        double upperPerc = (score - scoreData.Score) / (nextScoreData.Score - scoreData.Score);
                        return upperPerc * nextScoreData.ScoreBucket + (1 - upperPerc) * scoreData.ScoreBucket;
                    }
                    else
                    {
                        double upperPerc = (score - nextScoreData.Score) / (scoreData.Score - nextScoreData.Score);
                        return upperPerc * scoreData.ScoreBucket + (1 - upperPerc) * nextScoreData.ScoreBucket;
                    }
                }
            }
        }

        return 1;
    }

    public void buildDistribution(final Map<String,Map<Integer,BindScoreMatrix>> alleleBindMatrixMap, final FlankScores flankScores)
    {
        Set<Integer> peptideLengths = Sets.newHashSet();

        for(Map<Integer,BindScoreMatrix> pepLenEntry : alleleBindMatrixMap.values())
        {
            peptideLengths.addAll(pepLenEntry.keySet());
            break;
        }

        if(!loadRandomPeptides(mConfig.RandomPeptidesFile, peptideLengths, mRandomPeptideMap) || mRandomPeptideMap.isEmpty())
            return;

        mAlleleScoreDistributions.clear();

        List<RandomDistributionTask> alleleTasks = Lists.newArrayList();

        for(Map.Entry<String,Map<Integer,BindScoreMatrix>> alleleEntry : alleleBindMatrixMap.entrySet())
        {
            final String allele = alleleEntry.getKey();

            if(!mConfig.RequiredOutputAlleles.isEmpty() && !mConfig.RequiredOutputAlleles.contains(allele))
                continue;

            // NE_LOGGER.debug("building distribution for allele({})", allele);

            final Map<Integer, BindScoreMatrix> peptideLengthMatrixMap = alleleEntry.getValue();

            alleleTasks.add(new RandomDistributionTask(allele, peptideLengthMatrixMap, mRandomPeptideMap, flankScores));
        }

        NE_LOGGER.info("building distribution for {} allele(s)", alleleTasks.size());

        if(mConfig.Threads > 1)
        {
            final List<Callable> callableList = alleleTasks.stream().collect(Collectors.toList());
            TaskExecutor.executeTasks(callableList, callableList.size());
        }
        else
        {
            alleleTasks.forEach(x -> x.call());
        }

        alleleTasks.forEach(x -> mAlleleScoreDistributions.put(x.allele(), x.getPeptideLengthScoreDistributions()));

        if(mConfig.WriteRandomDistribution)
            writeDistribution();
    }

    public void buildLikelihoodDistribution(
            final Map<String,Map<Integer,BindScoreMatrix>> alleleBindMatrixMap, final FlankScores flankScores,
            final BindingLikelihood bindingLikelihood, final ExpressionLikelihood expressionLikelihood)
    {
        if(mRandomPeptideMap.isEmpty())
            return;

        mAlleleLikelihoodDistributions.clear();

        List<RandomDistributionTask> alleleTasks = Lists.newArrayList();

        for(Map.Entry<String,Map<Integer,BindScoreMatrix>> alleleEntry : alleleBindMatrixMap.entrySet())
        {
            final String allele = alleleEntry.getKey();

            if(!mConfig.RequiredOutputAlleles.isEmpty() && !mConfig.RequiredOutputAlleles.contains(allele))
                continue;

            final Map<Integer, BindScoreMatrix> peptideLengthMatrixMap = alleleEntry.getValue();

            alleleTasks.add(new RandomDistributionTask(
                    allele, peptideLengthMatrixMap, mRandomPeptideMap, flankScores, mAlleleScoreDistributions,
                    bindingLikelihood, expressionLikelihood));
        }

        NE_LOGGER.info("building likelihood distribution for {} allele(s)", alleleTasks.size());

        if(mConfig.Threads > 1)
        {
            final List<Callable> callableList = alleleTasks.stream().collect(Collectors.toList());
            TaskExecutor.executeTasks(callableList, callableList.size());
        }
        else
        {
            alleleTasks.forEach(x -> x.call());
        }

        alleleTasks.forEach(x -> mAlleleLikelihoodDistributions.put(x.allele(), x.getLikelihoodDistributions()));

        if(mConfig.WriteRandomDistribution)
            writeLikelihoodDistribution();
    }

    public static BufferedWriter initialiseWriter(final String filename)
    {
        try
        {
            BufferedWriter writer = createBufferedWriter(filename, false);

            writer.write("Allele,PeptideLength,ScoreBucket,Score,BucketCount,CumulativeCount");
            writer.newLine();
            return writer;
        }
        catch(IOException e)
        {
            NE_LOGGER.error("failed to write random peptide file: {}", e.toString());
            return null;
        }
    }

    public static void writeDistribution(final BufferedWriter writer, final List<ScoreDistributionData> scoreDistributionData)
    {
        try
        {
            for(ScoreDistributionData scoreDist : scoreDistributionData)
            {
                writer.write(String.format("%s,%d,%f,%.4f,%d,%d",
                        scoreDist.Allele, scoreDist.PeptideLength, scoreDist.ScoreBucket, scoreDist.Score,
                        scoreDist.BucketCount, scoreDist.CumulativeCount));

                writer.newLine();
            }
        }
        catch(IOException e)
        {
            NE_LOGGER.error("failed to write random peptide file: {}", e.toString());
            return;
        }
    }

    private void writeDistribution()
    {
        NE_LOGGER.info("writing random peptide scoring distribution for {} alleles",
                !mConfig.RequiredOutputAlleles.isEmpty() ? mConfig.RequiredOutputAlleles.size() : "all");

        final String distFilename = formTrainingFilename(mConfig.OutputDir, FILE_ID_RAND_DIST, mConfig.OutputId);

        try
        {
            BufferedWriter writer = initialiseWriter(distFilename);

            for(Map.Entry<String,Map<Integer,List<ScoreDistributionData>>> alleleEntry : mAlleleScoreDistributions.entrySet())
            {
                final Map<Integer,List<ScoreDistributionData>> pepLenDistMap = alleleEntry.getValue();

                for(List<ScoreDistributionData> pepLenScoreDist : pepLenDistMap.values())
                {
                    writeDistribution(writer, pepLenScoreDist);
                }
            }

            writer.close();
        }
        catch(IOException e)
        {
            NE_LOGGER.error("failed to write random peptide file: {}", e.toString());
        }
    }

    private void writeLikelihoodDistribution()
    {
        NE_LOGGER.info("writing random peptide likelihood distribution for {} alleles",
                !mConfig.RequiredOutputAlleles.isEmpty() ? mConfig.RequiredOutputAlleles.size() : "all");

        final String filename = formTrainingFilename(mConfig.OutputDir, FILE_ID_LIKELIHOOD_RAND_DIST, mConfig.OutputId);

        try
        {
            BufferedWriter writer = createBufferedWriter(filename, false);

            writer.write("Allele,Bucket,Likelihood");
            writer.newLine();

            for(Map.Entry<String,List<ScoreDistributionData>> alleleEntry : mAlleleLikelihoodDistributions.entrySet())
            {
                for(ScoreDistributionData scoreDist : alleleEntry.getValue())
                {
                    writer.write(String.format("%s,%f,%.8f",
                            scoreDist.Allele, scoreDist.ScoreBucket, scoreDist.Score));

                    writer.newLine();
                }
            }

            writer.close();
        }
        catch(IOException e)
        {
            NE_LOGGER.error("failed to write random peptide likelihood file: {}", e.toString());
        }
    }

    private boolean loadDistribution()
    {
        if(mConfig.RandomPeptideDistributionFile == null)
            return false;

        try
        {
            final List<String> lines = Files.readAllLines(new File(mConfig.RandomPeptideDistributionFile).toPath());

            final Map<String,Integer> fieldsIndexMap = createFieldsIndexMap(lines.get(0), DELIM);
            lines.remove(0);

            for(String line : lines)
            {
                ScoreDistributionData data = ScoreDistributionData.fromCsv(line, fieldsIndexMap);

                Map<Integer,List<ScoreDistributionData>> peptideLengthMap = mAlleleScoreDistributions.get(data.Allele);

                if(peptideLengthMap == null)
                {
                    peptideLengthMap = Maps.newHashMap();
                    mAlleleScoreDistributions.put(data.Allele, peptideLengthMap);
                }

                List<ScoreDistributionData> dataList = peptideLengthMap.get(data.PeptideLength);

                if(dataList == null)
                {
                    dataList = Lists.newArrayList();
                    peptideLengthMap.put(data.PeptideLength, dataList);
                }

                dataList.add(data);
            }

            NE_LOGGER.info("loaded {} alleles with random peptide score distribution data", mAlleleScoreDistributions.size());
        }
        catch(IOException e)
        {
            NE_LOGGER.error("failed to read random distribution file: {}", e.toString());
            return false;
        }

        return true;
    }

    private boolean loadLikelihoodDistribution()
    {
        if(mConfig.RandomLikelihoodDistributionFile == null)
            return true;

        try
        {
            final List<String> lines = Files.readAllLines(new File(mConfig.RandomLikelihoodDistributionFile).toPath());

            final Map<String,Integer> fieldsIndexMap = createFieldsIndexMap(lines.get(0), DELIM);
            lines.remove(0);

            for(String line : lines)
            {
                ScoreDistributionData data = ScoreDistributionData.fromLikelihoodCsv(line, fieldsIndexMap);

                List<ScoreDistributionData> dataList = mAlleleLikelihoodDistributions.get(data.Allele);

                if(dataList == null)
                {
                    dataList = Lists.newArrayList();
                    mAlleleLikelihoodDistributions.put(data.Allele, dataList);
                }

                dataList.add(data);
            }

            NE_LOGGER.info("loaded {} alleles with random peptide likelihood distribution data", mAlleleScoreDistributions.size());
        }
        catch(IOException e)
        {
            NE_LOGGER.error("failed to read random likelihood distribution file: {}", e.toString());
            return false;
        }

        return true;
    }
}
