package com.hartwig.hmftools.purple.copynumber;

import com.google.common.annotations.VisibleForTesting;
import com.hartwig.hmftools.common.purple.PurityAdjuster;
import com.hartwig.hmftools.common.utils.Doubles;
import com.hartwig.hmftools.purple.region.ObservedRegion;

import org.jetbrains.annotations.NotNull;

public class AlleleTolerance implements CopyNumberTolerance
{
    private static final double MIN_OBSERVED_BAF_CHANGE = 0.03;
    private static final double MAX_DEVIATION_ADJUSTMENT = 0.20;
    private static final double MIN_ABSOLUTE_COPY_NUMBER_TOLERANCE = 0.3;
    private static final double MIN_RELATIVE_COPY_NUMBER_TOLERANCE = 0.12;

    @NotNull
    private final PurityAdjuster mPurityAdjuster;

    public AlleleTolerance(@NotNull final PurityAdjuster purityAdjuster)
    {
        mPurityAdjuster = purityAdjuster;
    }

    @Override
    public boolean inTolerance(final ObservedRegion first, final ObservedRegion second)
    {
        double purityAdjustment = purityAdjustment(mPurityAdjuster);

        int minBafCount = Math.min(first.bafCount(), second.bafCount());
        if(minBafCount > 0)
        {
            double observedBafDeviation = Math.abs(first.observedBAF() - second.observedBAF());

            double maxCopyNumber = Math.max(first.tumorCopyNumber(), second.tumorCopyNumber());
            double maxMinorAllelePloidyDeviation =
                    purityAdjustment * tolerance(MIN_ABSOLUTE_COPY_NUMBER_TOLERANCE, 0.5 * maxCopyNumber, minBafCount);
            double minorAllelePloidyDeviation = Math.abs(first.minorAlleleCopyNumber() - second.minorAlleleCopyNumber());
            if(Doubles.greaterThan(minorAllelePloidyDeviation, maxMinorAllelePloidyDeviation) && Doubles.greaterThan(observedBafDeviation,
                    MIN_OBSERVED_BAF_CHANGE))
            {
                return false;
            }
        }

        int minWindowDepthCount = Math.min(first.depthWindowCount(), second.depthWindowCount());
        double absTolerance = purityAdjustment * tolerance(MIN_ABSOLUTE_COPY_NUMBER_TOLERANCE, 2, minWindowDepthCount);
        double relTolerance = purityAdjustment * tolerance(MIN_RELATIVE_COPY_NUMBER_TOLERANCE, 0.8, minWindowDepthCount);

        boolean copyNumberInTolerance =
                inAbsoluteTolerance(absTolerance, first.tumorCopyNumber(), second.tumorCopyNumber()) || inRelativeTolerance(relTolerance,
                        first.tumorCopyNumber(),
                        second.tumorCopyNumber());

        boolean refNormalisedCopyNumberInTolerance =
                inAbsoluteTolerance(absTolerance, first.refNormalisedCopyNumber(), second.refNormalisedCopyNumber()) || inRelativeTolerance(
                        relTolerance,
                        first.refNormalisedCopyNumber(),
                        second.refNormalisedCopyNumber());

        return copyNumberInTolerance || refNormalisedCopyNumberInTolerance;
    }

    private static double purityAdjustment(final PurityAdjuster purityAdjuster)
    {
        return Math.max(1, MAX_DEVIATION_ADJUSTMENT / purityAdjuster.purity());
    }

    private static double tolerance(double minTolerance, double additional, int samples)
    {
        return minTolerance + additional / Math.sqrt(samples);
    }

    private static boolean inAbsoluteTolerance(double tolerance, double firstCopyNumber, double secondCopyNumber)
    {
        final double absCopyNumberDifference = Math.abs(firstCopyNumber - secondCopyNumber);
        return Doubles.lessOrEqual(absCopyNumberDifference, tolerance);
    }

    private static boolean inRelativeTolerance(double tolerance, double firstCopyNumber, double secondCopyNumber)
    {
        final double relCopyNumberDifference = relativeCopyNumberChange(firstCopyNumber, secondCopyNumber);
        return Doubles.lessOrEqual(relCopyNumberDifference, tolerance);
    }

    @VisibleForTesting
    static double relativeCopyNumberChange(double firstCopyNumber, double secondCopyNumber)
    {
        final double absCopyNumberDifference =
                Math.abs(Math.max(firstCopyNumber, secondCopyNumber) - Math.min(firstCopyNumber, secondCopyNumber));
        if(Doubles.isZero(absCopyNumberDifference))
        {
            return 0;
        }

        if(Doubles.isZero(firstCopyNumber) || Doubles.isZero(secondCopyNumber))
        {
            return 1;
        }

        return absCopyNumberDifference / Math.abs(Math.min(firstCopyNumber, secondCopyNumber));
    }

}
