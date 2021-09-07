package com.hartwig.hmftools.sage.read;

import static com.hartwig.hmftools.sage.SageCommon.SG_LOGGER;

import com.hartwig.hmftools.common.variant.hotspot.VariantHotspot;
import com.hartwig.hmftools.sage.config.QualityConfig;
import com.hartwig.hmftools.sage.config.SageConfig;
import com.hartwig.hmftools.sage.quality.QualityRecalibrationMap;
import com.hartwig.hmftools.sage.realign.Realigned;
import com.hartwig.hmftools.sage.realign.RealignedContext;
import com.hartwig.hmftools.sage.realign.RealignedType;
import com.hartwig.hmftools.sage.samtools.NumberEvents;
import com.hartwig.hmftools.sage.variant.VariantTier;

import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.CigarElement;
import htsjdk.samtools.SAMRecord;

public class ReadContextCounter implements VariantHotspot
{
    public final String Sample;
    public final VariantHotspot Variant;
    public final ReadContext ReadContext;
    public final RawContextFactory RawFactory;
    public final QualityRecalibrationMap QualityRecalibrationMap;
    public final VariantTier Tier;
    public final boolean Realign;
    public final int MaxCoverage;
    public final int MinNumberOfEvents;

    private final ExpandedBasesFactory mExpandedBasesFactory;

    private int mFull;
    private int mPartial;
    private int mCore;
    private int mAlt;
    private int mRealigned;
    private int mReference;
    private int mCoverage;

    private int mLengthened;
    private int mShortened;

    private int mFullQuality;
    private int mPartialQuality;
    private int mCoreQuality;
    private int mAltQuality;
    private int mRealignedQuality;
    private int mReferenceQuality;
    private int mTotalQuality;

    private double mJitterPenalty;

    private int mImproperPair;

    private int mRawDepth;
    private int mRawAltSupport;
    private int mRawRefSupport;
    private int mRawAltBaseQuality;
    private int mRawRefBaseQuality;

    public ReadContextCounter(@NotNull final String sample, @NotNull final VariantHotspot variant, @NotNull final ReadContext readContext,
            final QualityRecalibrationMap recalibrationMap, final VariantTier tier, final int maxCoverage, final int minNumberOfEvents,
            final int maxSkippedReferenceRegions, boolean realign)
    {
        Sample = sample;
        Tier = tier;
        Variant = variant;
        ReadContext = readContext;
        RawFactory = new RawContextFactory(variant);
        Realign = realign;
        MaxCoverage = maxCoverage;
        QualityRecalibrationMap = recalibrationMap;
        MinNumberOfEvents = minNumberOfEvents;
        mExpandedBasesFactory = new ExpandedBasesFactory(maxSkippedReferenceRegions, maxSkippedReferenceRegions);
    }

    @NotNull
    @Override
    public String chromosome()
    {
        return Variant.chromosome();
    }

    @Override
    public long position()
    {
        return Variant.position();
    }

    @NotNull
    @Override
    public String ref()
    {
        return Variant.ref();
    }

    @NotNull
    @Override
    public String alt()
    {
        return Variant.alt();
    }

    public int altSupport()
    {
        return mFull + mPartial + mCore + mAlt + mRealigned;
    }

    public int refSupport()
    {
        return mReference;
    }

    public int coverage()
    {
        return mCoverage;
    }

    public int depth()
    {
        return mCoverage;
    }

    public double vaf()
    {
        return af(altSupport());
    }

    public double refAllelicFrequency()
    {
        return af(refSupport());
    }

    private double af(double support)
    {
        return mCoverage == 0 ? 0d : support / mCoverage;
    }

    public int tumorQuality()
    {
        int tumorQuality = mFullQuality + mPartialQuality;
        return Math.max(0, tumorQuality - (int) mJitterPenalty);
    }

    public int[] counts()
    {
        return new int[] { mFull, mPartial, mCore, mRealigned, mAlt, mReference, mCoverage };
    }

    public int[] jitter()
    {
        return new int[] { mShortened, mLengthened, qualityJitterPenalty() };
    }

    public int[] quality()
    {
        return new int[] { mFullQuality, mPartialQuality, mCoreQuality, mRealignedQuality, mAltQuality, mReferenceQuality, mTotalQuality };
    }

    public int improperPair()
    {
        return mImproperPair;
    }

    public int rawDepth()
    {
        return mRawDepth;
    }
    public int rawAltSupport()
    {
        return mRawAltSupport;
    }
    public int rawRefSupport()
    {
        return mRawRefSupport;
    }
    public int rawAltBaseQuality()
    {
        return mRawAltBaseQuality;
    }
    public int rawRefBaseQuality()
    {
        return mRawRefBaseQuality;
    }

    @Override
    public String toString()
    {
        return ReadContext.toString();
    }

    public void accept(final SAMRecord record, final SageConfig sageConfig, final int rawNumberOfEvents)
    {
        try
        {
            if(mCoverage >= MaxCoverage)
            {
                return;
            }

            if(!Tier.equals(VariantTier.HOTSPOT) && record.getMappingQuality() < sageConfig.MinMapQuality)
            {
                return;
            }

            final RawContext rawContext = RawFactory.create(sageConfig.maxSkippedReferenceRegions(), record);
            if(rawContext.isReadIndexInSkipped())
            {
                return;
            }

            final int readIndex = rawContext.readIndex();
            final boolean baseDeleted = rawContext.isReadIndexInDelete();

            mRawDepth += rawContext.isDepthSupport() ? 1 : 0;
            mRawAltSupport += rawContext.isAltSupport() ? 1 : 0;
            mRawRefSupport += rawContext.isRefSupport() ? 1 : 0;
            mRawAltBaseQuality += rawContext.altQuality();
            mRawRefBaseQuality += rawContext.refQuality();

            if(readIndex < 0)
            {
                return;
            }

            boolean covered = ReadContext.isCentreCovered(readIndex, record.getReadBases());
            if(!covered)
            {
                return;
            }

            final QualityConfig qualityConfig = sageConfig.Quality;
            int numberOfEvents =
                    Math.max(MinNumberOfEvents, NumberEvents.numberOfEventsWithMNV(rawNumberOfEvents, Variant.ref(), Variant.alt()));
            double quality = calculateQualityScore(readIndex, record, qualityConfig, numberOfEvents);

            // Check if FULL, PARTIAL, OR CORE
            if(!baseDeleted)
            {
                final boolean wildcardMatchInCore = Variant.isSNV() && ReadContext.microhomology().isEmpty();
                final IndexedBases expandedBases = mExpandedBasesFactory.expand((int) position(), readIndex, record);
                final ReadContextMatch match =
                        ReadContext.matchAtPosition(wildcardMatchInCore, expandedBases.Index, expandedBases.Bases);

                if(!match.equals(ReadContextMatch.NONE))
                {
                    switch(match)
                    {
                        case FULL:
                            incrementQualityFlags(record);
                            mFull++;
                            mFullQuality += quality;
                            break;
                        case PARTIAL:
                            incrementQualityFlags(record);
                            mPartial++;
                            mPartialQuality += quality;
                            break;
                        case CORE:
                            incrementQualityFlags(record);
                            mCore++;
                            mCoreQuality += quality;
                            break;
                    }

                    mCoverage++;
                    mTotalQuality += quality;
                    return;
                }
            }

            // Check if REALIGNED
            final RealignedContext realignment = realignmentContext(Realign, readIndex, record);
            final RealignedType realignmentType = realignment.Type;
            if(realignmentType.equals(RealignedType.EXACT))
            {
                mRealigned++;
                mRealignedQuality += quality;
                mCoverage++;
                mTotalQuality += quality;
                return;
            }

            if(realignmentType.equals(RealignedType.NONE) && rawContext.isReadIndexInSoftClip())
            {
                return;
            }

            mCoverage++;
            mTotalQuality += quality;
            if(rawContext.isRefSupport())
            {
                mReference++;
                mReferenceQuality += quality;
            }
            else if(rawContext.isAltSupport())
            {
                mAlt++;
                mAltQuality++;
            }

            // Jitter Penalty
            switch(realignmentType)
            {
                case LENGTHENED:
                    mJitterPenalty += qualityConfig.jitterPenalty(realignment.RepeatCount);
                    mLengthened++;
                    break;
                case SHORTENED:
                    mJitterPenalty += qualityConfig.jitterPenalty(realignment.RepeatCount);
                    mShortened++;
                    break;
            }

        } catch(Exception e)
        {
            SG_LOGGER.error("Error at chromosome: {}, position: {}", chromosome(), position());
            throw e;
        }
    }

    @NotNull
    private RealignedContext realignmentContext(boolean realign, int readIndex, SAMRecord record)
    {
        if(!realign)
        {
            return new RealignedContext(RealignedType.NONE, 0);
        }

        int index = ReadContext.readBasesPositionIndex();
        int leftIndex = ReadContext.readBasesLeftCentreIndex();
        int rightIndex = ReadContext.readBasesRightCentreIndex();

        int leftOffset = index - leftIndex;
        int rightOffset = rightIndex - index;

        int indelLength = indelLength(record);
        return Realigned.realignedAroundIndex(ReadContext,
                readIndex,
                record.getReadBases(),
                Math.max(indelLength + Math.max(leftOffset, rightOffset), Realigned.MAX_REPEAT_SIZE));
    }

    private double calculateQualityScore(int readBaseIndex, final SAMRecord record, final QualityConfig qualityConfig, int numberOfEvents)
    {
        final double baseQuality = baseQuality(readBaseIndex, record);
        final int distanceFromReadEdge = readDistanceFromEdge(readBaseIndex, record);

        final int mapQuality = record.getMappingQuality();
        int modifiedMapQuality = qualityConfig.modifiedMapQuality(Variant, mapQuality, numberOfEvents, record.getProperPairFlag());
        double modifiedBaseQuality = qualityConfig.modifiedBaseQuality(baseQuality, distanceFromReadEdge);

        return Math.max(0, Math.min(modifiedMapQuality, modifiedBaseQuality));
    }

    private double baseQuality(int readBaseIndex, SAMRecord record)
    {
        return Variant.ref().length() == Variant.alt().length()
                ? baseQuality(readBaseIndex, record, Variant.ref().length())
                : ReadContext.avgCentreQuality(readBaseIndex, record);
    }

    private double baseQuality(int startReadIndex, @NotNull final SAMRecord record, int length)
    {
        int maxIndex = Math.min(startReadIndex + length, record.getBaseQualities().length) - 1;
        int maxLength = maxIndex - startReadIndex + 1;

        double quality = Integer.MAX_VALUE;
        for(int i = 0; i < maxLength; i++)
        {
            int refPosition = (int) position() + i;
            int readIndex = startReadIndex + i;
            byte rawQuality = record.getBaseQualities()[readIndex];
            byte[] trinucleotideContext = ReadContext.refTrinucleotideContext(refPosition);
            double recalibratedQuality =
                    QualityRecalibrationMap.quality((byte) ref().charAt(i), (byte) alt().charAt(i), trinucleotideContext, rawQuality);
            quality = Math.min(quality, recalibratedQuality);
        }

        return quality;
    }

    private int qualityJitterPenalty()
    {
        return (int) mJitterPenalty;
    }

    private void incrementQualityFlags(@NotNull final SAMRecord record)
    {
        if(!record.getProperPairFlag())
        {
            mImproperPair++;
        }
    }

    private int indelLength(@NotNull final SAMRecord record)
    {
        int result = 0;
        for(CigarElement cigarElement : record.getCigar())
        {
            switch(cigarElement.getOperator())
            {
                case I:
                case D:
                    result += cigarElement.getLength();
            }

        }

        return result;
    }

    private int readDistanceFromEdge(int readIndex, @NotNull final SAMRecord record)
    {
        int index = ReadContext.readBasesPositionIndex();
        int leftIndex = ReadContext.readBasesLeftCentreIndex();
        int rightIndex = ReadContext.readBasesRightCentreIndex();

        int leftOffset = index - leftIndex;
        int rightOffset = rightIndex - index;

        int adjustedLeftIndex = readIndex - leftOffset;
        int adjustedRightIndex = readIndex + rightOffset;

        return Math.max(0, Math.min(adjustedLeftIndex, record.getReadBases().length - 1 - adjustedRightIndex));
    }
}
