package com.hartwig.hmftools.purple.copynumber;

import static com.hartwig.hmftools.purple.PurpleCommon.PPL_LOGGER;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.function.IntUnaryOperator;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.purple.copynumber.CopyNumberMethod;
import com.hartwig.hmftools.common.purple.region.FittedRegion;
import com.hartwig.hmftools.common.purple.region.GermlineStatus;
import com.hartwig.hmftools.common.purple.segment.SegmentSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

class ExtendDiploid
{
    private enum Direction
    {
        LEFT(index -> index - 1),
        RIGHT(index -> index + 1);

        private final IntUnaryOperator indexOperator;

        Direction(final IntUnaryOperator indexOperator)
        {
            this.indexOperator = indexOperator;
        }

        int moveIndex(int index)
        {
            return indexOperator.applyAsInt(index);
        }
    }

    private static final int MIN_BAF_COUNT_TO_WEIGH_WITH_BAF = 50;
    private static final DecimalFormat FORMAT = new DecimalFormat("0.00");

    private final int mMinTumorCount;
    private final int mMinTumorCountAtCentromere;
    private final CopyNumberTolerance mTolerance;

    ExtendDiploid(@NotNull final CopyNumberTolerance tolerance, final int minTumorCount, final int minTumorCountAtCentromere)
    {
        mMinTumorCount = minTumorCount;
        mMinTumorCountAtCentromere = minTumorCountAtCentromere;
        mTolerance = tolerance;
    }

    @NotNull
    List<CombinedRegion> extendDiploid(@NotNull final Collection<FittedRegion> fittedRegions)
    {
        final boolean bafWeighted = fittedRegions.stream().anyMatch(x -> x.bafCount() >= MIN_BAF_COUNT_TO_WEIGH_WITH_BAF);

        final List<CombinedRegion> regions = Lists.newLinkedList();

        for(FittedRegion fittedRegion : fittedRegions)
        {
            regions.add(new CombinedRegionImpl(bafWeighted, fittedRegion));
        }

        int highestConfidenceIndex = nextIndex(regions);
        while(highestConfidenceIndex > -1)
        {
            final CombinedRegion highestConfidence = regions.get(highestConfidenceIndex);
            highestConfidence.setCopyNumberMethod(CopyNumberMethod.BAF_WEIGHTED);

            PPL_LOGGER.trace("Selected region {}", toString(highestConfidence.region()));
            extendRight(regions, highestConfidenceIndex);
            extendLeft(regions, highestConfidenceIndex);

            PPL_LOGGER.trace("Completed region {}", toString(highestConfidence.region()));
            highestConfidenceIndex = nextIndex(regions);
        }

        return regions;
    }

    private void extendRight(@NotNull final List<CombinedRegion> regions, int targetIndex)
    {
        assert (targetIndex < regions.size());
        int neighbourIndex = targetIndex + 1;

        while(neighbourIndex < regions.size())
        {
            if(!merge(regions, Direction.RIGHT, targetIndex))
            {
                return;
            }
            regions.remove(neighbourIndex);
        }
    }

    private void extendLeft(@NotNull final List<CombinedRegion> regions, final int targetIndex)
    {
        assert (targetIndex < regions.size());
        int neighbourIndex = targetIndex - 1;

        while(neighbourIndex >= 0)
        {
            if(!merge(regions, Direction.LEFT, neighbourIndex + 1))
            {
                return;
            }
            regions.remove(neighbourIndex);
            neighbourIndex--;
        }
    }

    private boolean merge(@NotNull final List<CombinedRegion> regions, @NotNull final Direction direction, int targetIndex)
    {
        final CombinedRegion target = regions.get(targetIndex);
        final FittedRegion neighbour = regions.get(direction.moveIndex(targetIndex)).region();

        if(Extend.doNotExtend(target, neighbour))
        {
            return false;
        }

        int minTumorCount =
                nextBigBreakIsCentromereOrTelomere(regions, direction, targetIndex) ? mMinTumorCountAtCentromere : this.mMinTumorCount;

        final boolean isNeighbourDubious = isDubious(minTumorCount, neighbour);
        if(isNeighbourDubious)
        {
            if(inTolerance(target.region(), neighbour))
            {
                target.extendWithWeightedAverage(neighbour);
                return true;
            }
            else if(pushThroughDubiousRegion(minTumorCount, regions, direction, targetIndex))
            {
                target.extend(neighbour);
                return true;
            }
            else
            {
                return false;
            }
        }

        final boolean isNeighbourValid = isValid(minTumorCount, neighbour);
        if(!isNeighbourValid)
        {
            target.extend(neighbour);
            return true;
        }
        else if(inTolerance(target.region(), neighbour))
        {
            target.extendWithWeightedAverage(neighbour);
            return true;
        }

        return false;
    }

    private boolean isValid(int minTumorCount, @NotNull final FittedRegion region)
    {
        return region.status() == GermlineStatus.DIPLOID && (region.support().isSV() || region.depthWindowCount() >= minTumorCount);
    }

    private boolean isDubious(int minTumorCount, @NotNull final FittedRegion region)
    {
        return region.status() == GermlineStatus.DIPLOID && !region.support().isSV() && region.depthWindowCount() < minTumorCount;
    }

    private boolean nextBigBreakIsCentromereOrTelomere(@NotNull final List<CombinedRegion> regions, @NotNull final Direction direction,
            int targetIndex)
    {
        for(int i = direction.moveIndex(targetIndex); i >= 0 && i < regions.size(); i = direction.moveIndex(i))
        {
            final FittedRegion neighbour = regions.get(i).region();
            if(neighbour.support() == SegmentSupport.CENTROMERE)
            {
                return true;
            }
            if(neighbour.support().isSV())
            {
                return false;
            }
        }

        return true;
    }

    private boolean pushThroughDubiousRegion(int minTumorCount, @NotNull final List<CombinedRegion> regions,
            @NotNull final Direction direction, int targetIndex)
    {
        int dubiousCount = 0;
        final CombinedRegion target = regions.get(targetIndex);
        for(int i = direction.moveIndex(targetIndex); i >= 0 && i < regions.size(); i = direction.moveIndex(i))
        {
            final FittedRegion neighbour = regions.get(i).region();

            // Coming from left to right, EXCLUDE neighbour from decision on break.
            if(neighbour.start() > target.start())
            {
                if(neighbour.support() == SegmentSupport.CENTROMERE)
                {
                    return dubiousCount < minTumorCount;
                }
                if(neighbour.support().isSV())
                {
                    return false;
                }
            }

            boolean isDubious = isDubious(minTumorCount, neighbour);
            boolean inTolerance = inTolerance(target.region(), neighbour);

            if(isDubious && !inTolerance)
            {

                dubiousCount += neighbour.depthWindowCount();
                if(dubiousCount >= minTumorCount)
                {
                    return false;
                }
            }

            if(isValid(minTumorCount, neighbour))
            {
                return inTolerance;
            }

            // Coming from right to left, INCLUDE neighbour from decision on break.
            if(neighbour.start() < target.start())
            {
                if(neighbour.support() == SegmentSupport.CENTROMERE)
                {
                    return dubiousCount < minTumorCount;
                }
                if(neighbour.support().isSV())
                {
                    return false;
                }
            }
        }

        return dubiousCount < minTumorCount;
    }

    private boolean inTolerance(@NotNull final FittedRegion left, @NotNull final FittedRegion right)
    {
        return mTolerance.inTolerance(left, right);
    }

    private static int nextIndex(@NotNull final List<CombinedRegion> regions)
    {
        int indexOfLargestBaf = -1;
        int indexOfLargestLength = -1;
        int indexOfTumorRatioCount = -1;

        int largestBAFCount = 0;
        long largestDepthWindowCount = 0;

        for(int i = 0; i < regions.size(); i++)
        {
            final CombinedRegion combined = regions.get(i);
            final FittedRegion region = combined.region();
            if(!combined.isProcessed() && region.status().equals(GermlineStatus.DIPLOID))
            {

                if(region.bafCount() > largestBAFCount)
                {
                    largestBAFCount = region.bafCount();
                    indexOfLargestBaf = i;
                }

                if(region.depthWindowCount() > largestDepthWindowCount)
                {
                    largestDepthWindowCount = region.depthWindowCount();
                    indexOfTumorRatioCount = i;
                }
            }
        }

        return indexOfLargestBaf > -1 ? indexOfLargestBaf : (indexOfTumorRatioCount > -1 ? indexOfTumorRatioCount : indexOfLargestLength);
    }

    @NotNull
    private static String toString(@NotNull FittedRegion region)
    {
        return MoreObjects.toStringHelper("FittedRegion")
                .omitNullValues()
                .add("chromosome", region.chromosome())
                .add("start", region.start())
                .add("end", region.end())
                .add("status", region.status())
                .add("support", region.support())
                .add("copyNumber", FORMAT.format(region.tumorCopyNumber()))
                .toString();
    }
}
