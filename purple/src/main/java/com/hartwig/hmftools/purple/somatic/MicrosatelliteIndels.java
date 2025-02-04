package com.hartwig.hmftools.purple.somatic;

import static com.hartwig.hmftools.common.variant.enrich.SomaticRefContextEnrichment.REPEAT_COUNT_FLAG;
import static com.hartwig.hmftools.common.variant.enrich.SomaticRefContextEnrichment.REPEAT_SEQUENCE_FLAG;
import static com.hartwig.hmftools.purple.PurpleCommon.PPL_LOGGER;

import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.hartwig.hmftools.purple.config.TargetRegionsData;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;

public class MicrosatelliteIndels
{
    private final TargetRegionsData mTargetRegions;
    private int mIndelCount;

    private static final int MIN_SEQUENCE_LENGTH_FOR_LONG_REPEATS = 2;
    private static final int MAX_SEQUENCE_LENGTH_FOR_LONG_REPEATS = 4;
    private static final int MIN_REPEAT_COUNT_FOR_LONG_REPEATS = 4;
    private static final int MIN_REPEAT_COUNT_FOR_SHORT_REPEATS = 5;

    private static final int MAX_REF_ALT_LENGTH = 50;
    private static final double MIN_TARGET_REGIONS_AF = 0.15;

    public MicrosatelliteIndels(final TargetRegionsData referenceData)
    {
        mTargetRegions = referenceData;
        mIndelCount = 0;
    }

    public int msiIndelCount()
    {
        return mIndelCount;
    }

    public double msiIndelsPerMb()
    {
        return mTargetRegions.calcMsiIndels(mIndelCount);
    }

    public void processVariant(final SomaticVariant variant)
    {
        final VariantContext context = variant.context();

        if(!isValidIndel(context))
            return;

        if(mTargetRegions.hasTargetRegions())
        {
            if(!mTargetRegions.isTargetRegionsMsiIndel(context.getContig(), context.getStart()))
                return;

            if(variant.alleleFrequency() < MIN_TARGET_REGIONS_AF)
                return;
        }

        int repeatCount = context.getAttributeAsInt(REPEAT_COUNT_FLAG, 0);
        int repeatSequenceLength = context.getAttributeAsString(REPEAT_SEQUENCE_FLAG, Strings.EMPTY).length();

        if(repeatContextIsRelevant(repeatCount, repeatSequenceLength))
        {
            mIndelCount++;
        }
    }

    private static boolean isValidIndel(final VariantContext context)
    {
        int altLength = alt(context).length();
        int refLength = context.getReference().getBaseString().length();

        return context.isIndel() && refLength < MAX_REF_ALT_LENGTH && altLength < MAX_REF_ALT_LENGTH;
    }

    private static boolean repeatContextIsRelevant(int repeatCount, int repeatSequenceLength)
    {
        final boolean longRepeatRelevant =
                repeatSequenceLength >= MIN_SEQUENCE_LENGTH_FOR_LONG_REPEATS && repeatSequenceLength <= MAX_SEQUENCE_LENGTH_FOR_LONG_REPEATS
                        && repeatCount >= MIN_REPEAT_COUNT_FOR_LONG_REPEATS;
        final boolean shortRepeatRelevant = repeatSequenceLength == 1 && repeatCount >= MIN_REPEAT_COUNT_FOR_SHORT_REPEATS;
        return longRepeatRelevant | shortRepeatRelevant;
    }

    @VisibleForTesting
    static boolean repeatContextIsRelevant(int repeatCount, String sequence)
    {
        return repeatContextIsRelevant(repeatCount, sequence.length());
    }

    @NotNull
    private static String alt(final VariantContext context)
    {
        return String.join(",", context.getAlternateAlleles().stream().map(Allele::toString).collect(Collectors.toList()));
    }
}
