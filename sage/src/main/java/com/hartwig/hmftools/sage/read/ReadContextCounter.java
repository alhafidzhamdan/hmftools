package com.hartwig.hmftools.sage.read;

import com.hartwig.hmftools.common.genome.chromosome.MitochondrialChromosome;
import com.hartwig.hmftools.common.variant.hotspot.VariantHotspot;
import com.hartwig.hmftools.sage.config.QualityConfig;
import com.hartwig.hmftools.sage.config.SageConfig;
import com.hartwig.hmftools.sage.realign.Realigned;
import com.hartwig.hmftools.sage.realign.RealignedContext;
import com.hartwig.hmftools.sage.realign.RealignedType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.CigarElement;
import htsjdk.samtools.SAMRecord;

public class ReadContextCounter implements VariantHotspot {
    private static final Logger LOGGER = LogManager.getLogger(ReadContextCounter.class);

    private final String sample;
    private final VariantHotspot variant;
    private final ReadContext readContext;
    private final RawContextFactory rawFactory;
    private final boolean realign;
    private final boolean mitochondrial;

    private int full;
    private int partial;
    private int core;
    private int reference;
    private int realigned;
    private int lengthened;
    private int shortened;
    private int coverage;

    private int fullQuality;
    private int partialQuality;
    private int coreQuality;
    private int realignedQuality;
    private int referenceQuality;
    private int totalQuality;

    private double jitterPenalty;

    private int improperPair;

    private int rawDepth;
    private int rawAltSupport;
    private int rawRefSupport;
    private int rawAltBaseQuality;
    private int rawRefBaseQuality;

    public ReadContextCounter(@NotNull final String sample, @NotNull final VariantHotspot variant, @NotNull final ReadContext readContext,
            boolean realign) {
        this.sample = sample;
        assert (readContext.isComplete());
        this.variant = variant;
        this.readContext = readContext;
        this.rawFactory = new RawContextFactory(variant);
        this.realign = realign;
        this.mitochondrial = MitochondrialChromosome.contains(variant.chromosome());
    }

    @NotNull
    public String sample() {
        return sample;
    }

    public VariantHotspot variant() {
        return variant;
    }

    @NotNull
    @Override
    public String chromosome() {
        return variant.chromosome();
    }

    @Override
    public long position() {
        return variant.position();
    }

    @NotNull
    @Override
    public String ref() {
        return variant.ref();
    }

    @NotNull
    @Override
    public String alt() {
        return variant.alt();
    }

    public int altSupport() {
        return full + partial + core + realigned;
    }

    public int refSupport() {
        return reference;
    }

    public int coverage() {
        return coverage;
    }

    public int depth() {
        return coverage;
    }

    public double vaf() {
        return af(altSupport());
    }

    public double refAllelicFrequency() {
        return af(refSupport());
    }

    private double af(double support) {
        return coverage == 0 ? 0d : support / coverage;
    }

    public int tumorQuality() {
        int tumorQuality = fullQuality + partialQuality;
        return Math.max(0, tumorQuality - (int) jitterPenalty);
    }

    public int[] counts() {
        return new int[] { full, partial, core, realigned, reference, coverage };
    }

    public int[] jitter() {
        return new int[] { shortened, lengthened, qualityJitterPenalty() };
    }

    public int[] quality() {
        return new int[] { fullQuality, partialQuality, coreQuality, realignedQuality, referenceQuality, totalQuality };
    }

    public int improperPair() {
        return improperPair;
    }

    public int rawDepth() {
        return rawDepth;
    }

    public int rawAltSupport() {
        return rawAltSupport;
    }

    public int rawRefSupport() {
        return rawRefSupport;
    }

    public int rawAltBaseQuality() {
        return rawAltBaseQuality;
    }

    public int rawRefBaseQuality() {
        return rawRefBaseQuality;
    }

    public double rawVaf() {
        return rawDepth == 0 ? 0 : ((double) rawAltSupport) / rawDepth;
    }

    @NotNull
    public ReadContext readContext() {
        return readContext;
    }

    @Override
    public String toString() {
        return readContext.toString();
    }

    public void accept(final SAMRecord record, final SageConfig sageConfig) {
        int maxCoverage = mitochondrial ? 1000 * sageConfig.maxReadDepthEvidence() : sageConfig.maxReadDepthEvidence();
        try {
            if (coverage >= maxCoverage || !readContext.isComplete()) {
                return;
            }

            final RawContext rawContext = rawFactory.create(sageConfig.maxSkippedReferenceRegions(), record);
            if (rawContext.isReadIndexInSkipped()) {
                return;
            }

            final int readIndex = rawContext.readIndex();
            final boolean baseDeleted = rawContext.isReadIndexInDelete();

            rawDepth += rawContext.isDepthSupport() ? 1 : 0;
            rawAltSupport += rawContext.isAltSupport() ? 1 : 0;
            rawRefSupport += rawContext.isRefSupport() ? 1 : 0;
            rawAltBaseQuality += rawContext.altQuality();
            rawRefBaseQuality += rawContext.refQuality();

            if (readIndex > -1) {

                boolean covered = readContext.isCentreCovered(readIndex, record.getReadBases());
                if (!covered) {
                    return;
                }

                final QualityConfig qualityConfig = sageConfig.qualityConfig();
                double quality = calculateQualityScore(readIndex, record, qualityConfig);

                coverage++;
                totalQuality += quality;

                // Check if FULL, PARTIAL, OR CORE
                if (!baseDeleted) {
                    final ReadContextMatch match = readContext.matchAtPosition(readIndex, record.getReadBases());
                    if (!match.equals(ReadContextMatch.NONE)) {
                        switch (match) {
                            case FULL:
                                incrementQualityFlags(record);
                                full++;
                                fullQuality += quality;
                                break;
                            case PARTIAL:
                                incrementQualityFlags(record);
                                partial++;
                                partialQuality += quality;
                                break;
                            case CORE:
                                incrementQualityFlags(record);
                                core++;
                                coreQuality += quality;
                                break;
                        }

                        return;
                    }
                }

                // Check if realigned
                final RealignedContext realignment = realignmentContext(realign, readIndex, record);
                if (realignment.type().equals(RealignedType.EXACT)) {
                    realigned++;
                    realignedQuality += quality;
                    return;
                }

                // Check if lengthened, shortened AND/OR reference!
                switch (realignment.type()) {
                    case LENGTHENED:
                        jitterPenalty += qualityConfig.jitterPenalty(realignment.repeatCount());
                        lengthened++;
                        break;
                    case SHORTENED:
                        jitterPenalty += qualityConfig.jitterPenalty(realignment.repeatCount());
                        shortened++;
                        break;
                }

                byte refBase = (byte) this.variant.ref().charAt(0);
                byte readBase = record.getReadBases()[readIndex];

                if (refBase == readBase && !rawContext.isReadIndexInDelete() && !rawContext.isIndelAtPosition()) {
                    reference++;
                    referenceQuality += quality;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error at chromosome: {}, position: {}", chromosome(), position());
            throw e;
        }
    }

    @NotNull
    private RealignedContext realignmentContext(boolean realign, int readIndex, SAMRecord record) {
        if (!realign) {
            return new RealignedContext(RealignedType.NONE, 0);
        }

        int indelLength = indelLength(record);
        return Realigned.realignedAroundIndex(readContext,
                readIndex,
                record.getReadBases(),
                Math.max(indelLength, Realigned.MAX_REPEAT_SIZE));
    }

    private double calculateQualityScore(int readBaseIndex, final SAMRecord record, final QualityConfig qualityConfig) {
        final int distanceFromRef = readContext.distance();

        final int baseQuality = baseQuality(readBaseIndex, record);
        final int distanceFromReadEdge = readDistanceFromEdge(readBaseIndex, record);

        final int mapQuality = record.getMappingQuality();

        int modifiedMapQuality = qualityConfig.modifiedMapQuality(mapQuality, distanceFromRef, record.getProperPairFlag());
        int modifiedBaseQuality = qualityConfig.modifiedBaseQuality(baseQuality, distanceFromReadEdge);

        return Math.max(0, Math.min(modifiedMapQuality, modifiedBaseQuality));
    }

    private int baseQuality(int readBaseIndex, SAMRecord record) {
        return variant.ref().length() == variant.alt().length()
                ? baseQuality(readBaseIndex, record, variant.ref().length())
                : readContext.minCentreQuality(readBaseIndex, record);
    }

    private int baseQuality(int readIndex, SAMRecord record, int length) {
        int maxIndex = Math.min(readIndex + length, record.getBaseQualities().length) - 1;
        int quality = Integer.MAX_VALUE;
        for (int i = readIndex; i <= maxIndex; i++) {
            quality = Math.min(quality, record.getBaseQualities()[i]);
        }
        return quality;
    }

    private int qualityJitterPenalty() {
        return (int) jitterPenalty;
    }

    public boolean incrementCounters(@NotNull final ReadContext other) {
        if (readContext.isFullMatch(other)) {
            full++;
            return true;
        }

        return false;
    }

    private void incrementQualityFlags(@NotNull final SAMRecord record) {
        if (!record.getProperPairFlag()) {
            improperPair++;
        }
    }

    private int indelLength(@NotNull final SAMRecord record) {
        int result = 0;
        for (CigarElement cigarElement : record.getCigar()) {
            switch (cigarElement.getOperator()) {
                case I:
                case D:
                    result += cigarElement.getLength();
            }

        }

        return result;
    }

    private int readDistanceFromEdge(int readIndex, @NotNull final SAMRecord record) {
        int index = readContext.readBasesPositionIndex();
        int leftIndex = readContext.readBasesLeftCentreIndex();
        int rightIndex = readContext.readBasesRightCentreIndex();

        int leftOffset = index - leftIndex;
        int rightOffset = rightIndex - index;

        int adjustedLeftIndex = readIndex - leftOffset;
        int adjustedRightIndex = readIndex + rightOffset;

        return Math.max(0, Math.min(adjustedLeftIndex, record.getReadBases().length - 1 - adjustedRightIndex));
    }

}
