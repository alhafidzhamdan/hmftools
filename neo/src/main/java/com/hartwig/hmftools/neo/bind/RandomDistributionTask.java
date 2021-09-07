package com.hartwig.hmftools.neo.bind;

import static java.lang.Math.round;

import static com.hartwig.hmftools.neo.NeoCommon.NE_LOGGER;
import static com.hartwig.hmftools.neo.bind.BindConstants.PAN_PEPTIDE_LENGTH;
import static com.hartwig.hmftools.neo.bind.BindConstants.PAN_PEPTIDE_MAX_LENGTH;
import static com.hartwig.hmftools.neo.bind.RandomPeptideDistribution.getScoreRank;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.utils.Doubles;
import com.hartwig.hmftools.common.utils.VectorUtils;

public class RandomDistributionTask implements Callable
{
    private final String mAllele;

    private final Map<Integer,List<RandomPeptideData>> mRandomPeptideMap; // by length and with flanking data
    private final FlankScores mFlankScores;
    private final Map<Integer,BindScoreMatrix> mPeptideLengthMatrixMap;

    private final int mTaskType;

    // outputs
    private final Map<Integer,List<ScoreDistributionData>> mPeptideLengthScoresMap; // peptide length to distribution
    private final List<ScoreDistributionData> mLikelihoodsDistribution; // distribution of likelihoods
    private final Map<String,Map<Integer,List<ScoreDistributionData>>> mAlleleScoresMap;
    private final BindingLikelihood mBindingLikelihood;
    private final List<double[]> mDiscreteScoreData;

    private static final int SCORE_SIZE = 0;
    private static final int SCORE_BRACKET = 1;

    public static final int TASK_TYPE_SCORE_RANK = 0;
    public static final int TASK_TYPE_LIKELIHOOD_RANK = 1;

    public RandomDistributionTask(
            final String allele, final Map<Integer,BindScoreMatrix> peptideLengthMatrixMap,
            final Map<Integer,List<RandomPeptideData>> randomPeptideMap, final FlankScores flankScores)
    {
        this(TASK_TYPE_SCORE_RANK, allele, peptideLengthMatrixMap, randomPeptideMap, flankScores, null, null);
    }

    public RandomDistributionTask(
            final String allele, final Map<Integer,BindScoreMatrix> peptideLengthMatrixMap,
            final Map<Integer,List<RandomPeptideData>> randomPeptideMap, final FlankScores flankScores,
            final Map<String,Map<Integer,List<ScoreDistributionData>>> scoresMap, final BindingLikelihood bindingLikelihood)
    {
        this(TASK_TYPE_LIKELIHOOD_RANK, allele, peptideLengthMatrixMap, randomPeptideMap, flankScores, scoresMap, bindingLikelihood);
    }

    private RandomDistributionTask(
            final int taskType, final String allele, final Map<Integer,BindScoreMatrix> peptideLengthMatrixMap,
            final Map<Integer,List<RandomPeptideData>> randomPeptideMap, final FlankScores flankScores,
            final Map<String,Map<Integer,List<ScoreDistributionData>>> scoresMap, final BindingLikelihood bindingLikelihood)
    {
        mTaskType = taskType;
        mAllele = allele;
        mPeptideLengthMatrixMap = peptideLengthMatrixMap;
        mRandomPeptideMap = randomPeptideMap;
        mFlankScores = flankScores;
        mAlleleScoresMap = scoresMap;
        mBindingLikelihood = bindingLikelihood;

        mPeptideLengthScoresMap = Maps.newHashMap();
        mLikelihoodsDistribution = Lists.newArrayList();

        mDiscreteScoreData = generateDistributionBuckets();
    }

    public static List<double[]> generateDistributionBuckets()
    {
        List<double[]> discreteScoreData = Lists.newArrayList();;
        discreteScoreData.add(new double[] {0.0001, 0.01});
        discreteScoreData.add(new double[] {0.001, 0.1});
        discreteScoreData.add(new double[] {0.01, 0.25});
        discreteScoreData.add(new double[] {0.05, 1.0});
        return discreteScoreData;
    }

    public final String allele() { return mAllele; }

    public Map<Integer,List<ScoreDistributionData>> getPeptideLengthScoresMap() { return mPeptideLengthScoresMap; }
    public List<ScoreDistributionData> getLikelihoodDistributions() { return mLikelihoodsDistribution; }

    @Override
    public Long call()
    {
        if(mTaskType == TASK_TYPE_SCORE_RANK)
            buildScoreDistribution();
        else
            buildLikelihoodDistribution();

        return (long)0;
    }

    private void buildScoreDistribution()
    {
        if(mRandomPeptideMap.isEmpty())
            return;

        // score each against each allele and build up a percentiles for each
        NE_LOGGER.debug("building distribution for allele({})", mAllele);

        for(BindScoreMatrix matrix : mPeptideLengthMatrixMap.values())
        {
            List<RandomPeptideData> randomPeptides = mRandomPeptideMap.get(matrix.PeptideLength);

            if(randomPeptides == null || randomPeptides.isEmpty())
            {
                NE_LOGGER.error("missing random peptide data for length({})", matrix.PeptideLength);
                return;
            }

            List<Double> peptideScores = Lists.newArrayListWithExpectedSize(randomPeptides.size());

            int count = 0;

            for(RandomPeptideData peptideData : randomPeptides)
            {
                double score = BindScorer.calcScore(matrix, mFlankScores, peptideData.Peptide, peptideData.UpFlank, peptideData.DownFlank);

                VectorUtils.optimisedAdd(peptideScores, score, false);

                ++count;

                if(count > 0 && (count % 500000) == 0)
                {
                    NE_LOGGER.debug("added {} sorted random peptide scores", count);
                }
            }

            List<ScoreDistributionData> scoresDistributions = generateDistribution(matrix.Allele, matrix.PeptideLength, peptideScores);
            mPeptideLengthScoresMap.put(matrix.PeptideLength, scoresDistributions);
        }
    }

    public void buildLikelihoodDistribution()
    {
        if(mRandomPeptideMap.isEmpty())
            return;

        NE_LOGGER.debug("building likelihood distribution for allele({})", mAllele);

        List<Double> likelihoodScores = Lists.newArrayList();

        for(BindScoreMatrix matrix : mPeptideLengthMatrixMap.values())
        {
            // for now hard-code exclusion of length 12 since other tools are 8-11
            if(matrix.PeptideLength > PAN_PEPTIDE_MAX_LENGTH)
                continue;

            int count = 0;

            List<RandomPeptideData> randomPeptides = mRandomPeptideMap.get(matrix.PeptideLength);

            if(randomPeptides == null || randomPeptides.isEmpty())
                return;

            for(RandomPeptideData peptideData : randomPeptides)
            {
                double score = BindScorer.calcScore(matrix, mFlankScores, peptideData.Peptide, peptideData.UpFlank, peptideData.DownFlank);
                double rank = getScoreRank(mAlleleScoresMap, mAllele, matrix.PeptideLength, score);
                double likelihood = mBindingLikelihood.getBindingLikelihood(mAllele, peptideData.Peptide, rank);

                VectorUtils.optimisedAdd(likelihoodScores, likelihood, false);

                ++count;

                if(count > 0 && (count % 500000) == 0)
                {
                    NE_LOGGER.debug("added {} sorted random peptide likelihood scores", count);
                }
            }
        }

        mLikelihoodsDistribution.addAll(generateDistribution(mAllele, PAN_PEPTIDE_LENGTH, likelihoodScores));
    }

    private List<ScoreDistributionData> generateDistribution(final String allele, final int peptideLength, final List<Double> peptideScores)
    {
        return generateDistribution(allele, peptideLength, peptideScores, mDiscreteScoreData);
    }

    public static List<ScoreDistributionData> generateDistribution(
            final String allele, final int peptideLength, final List<Double> peptideScores, final List<double[]> discreteScoreData)
    {
        // write the distribution as 0.0001 up to 0.01, 0.001 up to 0.01, then 0.01 up to 100%
        // store the lowest/worst score at each point
        int totalScores = peptideScores.size();
        int discreteIndex = 0;
        double currentSize = discreteScoreData.get(discreteIndex)[SCORE_SIZE];
        double currentBracket = discreteScoreData.get(discreteIndex)[SCORE_BRACKET];
        int requiredScores = (int) round(totalScores * currentSize);

        int currentScoreCount = 0;
        double currentSizeTotal = 0;
        int cumulativeScores = 0;

        List<ScoreDistributionData> scoresDistributions = Lists.newArrayList();

        for(Double score : peptideScores)
        {
            ++currentScoreCount;
            ++cumulativeScores;

            if(currentScoreCount >= requiredScores)
            {
                scoresDistributions.add(new ScoreDistributionData(
                        allele, peptideLength, currentSizeTotal, score, currentScoreCount, cumulativeScores));

                currentScoreCount = 0;
                currentSizeTotal += currentSize;

                if(Doubles.equal(currentSizeTotal, currentBracket))
                {
                    ++discreteIndex;

                    if(discreteIndex < discreteScoreData.size())
                    {
                        currentSize = discreteScoreData.get(discreteIndex)[SCORE_SIZE];
                        currentBracket = discreteScoreData.get(discreteIndex)[SCORE_BRACKET];
                        requiredScores = (int) round(totalScores * currentSize);
                    }
                }
            }
        }

        return scoresDistributions;
    }
}
