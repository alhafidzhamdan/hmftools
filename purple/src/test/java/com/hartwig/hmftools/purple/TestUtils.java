package com.hartwig.hmftools.purple;

import static com.hartwig.hmftools.common.variant.Hotspot.HOTSPOT;
import static com.hartwig.hmftools.common.variant.Hotspot.HOTSPOT_FLAG;
import static com.hartwig.hmftools.common.variant.Hotspot.NEAR_HOTSPOT;
import static com.hartwig.hmftools.common.variant.Hotspot.NEAR_HOTSPOT_FLAG;
import static com.hartwig.hmftools.common.variant.VariantHeader.PURPLE_BIALLELIC_FLAG;
import static com.hartwig.hmftools.common.variant.VariantHeader.PURPLE_VARIANT_CN_INFO;
import static com.hartwig.hmftools.common.variant.VariantHeader.PURPLE_VARIANT_PLOIDY_INFO;
import static com.hartwig.hmftools.common.variant.enrich.SomaticRefContextEnrichment.REPEAT_COUNT_FLAG;
import static com.hartwig.hmftools.common.variant.impact.VariantImpactSerialiser.writeImpactDetails;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.test.SomaticVariantTestBuilderFactory;
import com.hartwig.hmftools.common.utils.Doubles;
import com.hartwig.hmftools.common.variant.CodingEffect;
import com.hartwig.hmftools.common.variant.Hotspot;
import com.hartwig.hmftools.common.variant.VariantConsequence;
import com.hartwig.hmftools.common.variant.VariantType;
import com.hartwig.hmftools.common.variant.impact.VariantImpact;
import com.hartwig.hmftools.purple.somatic.SomaticData;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.GenotypeBuilder;
import htsjdk.variant.variantcontext.GenotypesContext;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;

public final class TestUtils
{
    private static final String SAMPLE_ID = "SAMPLE_ID";
    private static final String TEST_GENE_01 = "GENE_01";

    public static SomaticData createVariant(
            final VariantType type, final CodingEffect codingEffect, int repeatCount, Hotspot hotspot, double vaf)
    {
        VariantContext context = createDefaultContext(type);

        context.getCommonInfo().putAttribute(REPEAT_COUNT_FLAG, repeatCount);

        if(hotspot == HOTSPOT)
            context.getCommonInfo().putAttribute(HOTSPOT_FLAG, true);
        else if(hotspot == NEAR_HOTSPOT)
            context.getCommonInfo().putAttribute(NEAR_HOTSPOT_FLAG, true);

        VariantImpact impact = new VariantImpact(
                TEST_GENE_01, "TRANS_01", VariantConsequence.MISSENSE_VARIANT.parentTerm(), codingEffect,
                "HgvsCoding", "HgvsPrtotein", false, "",
                codingEffect, 1);

        boolean biallelic = Doubles.greaterOrEqual(2 * vaf, 1.5);

        if(biallelic)
            context.getCommonInfo().putAttribute(PURPLE_BIALLELIC_FLAG, true);

        double variantCopyNumber = 2 * vaf;
        double adjustedCopyNumber = 2;

        context.getCommonInfo().putAttribute(PURPLE_VARIANT_CN_INFO, variantCopyNumber);

        //context.getCommonInfo().putAttribute(PURPLE_VARIANT_CN_INFO, );
        //context.getCommonInfo().putAttribute(PURPLE_VARIANT_CN_INFO, );

        writeImpactDetails(context, impact);

        return new SomaticData(context, SAMPLE_ID);
    }

    public static VariantContext createDefaultContext(final VariantType type)
    {
        VariantContextBuilder builder = new VariantContextBuilder();

        List<Allele> alleles = Lists.newArrayList();

        String ref;
        String alt;

        if(type == VariantType.SNP)
        {
            ref = "A";
            alt = "G";
        }
        else if(type == VariantType.MNP)
        {
            ref = "AA";
            alt = "GG";
        }
        else
        {
            ref = "AAA";
            alt = "A";
        }

        alleles.add(Allele.create(ref, true));
        alleles.add(Allele.create(alt, false));

        Map<String,Object> commonAttributes = Maps.newHashMap();
        Map<String,Object> refAttributes = Maps.newHashMap();
        Map<String,Object> tumorAttributes = Maps.newHashMap();

        int[] ad = {1, 1};
        Genotype gtNormal = new GenotypeBuilder().attributes(refAttributes).name("REF_ID").DP(-1).AD(ad).noPL().GQ(-1).make();
        Genotype gtTumor = new GenotypeBuilder().attributes(tumorAttributes).name(SAMPLE_ID).DP(-1).AD(ad).noPL().GQ(-1).make();

        GenotypesContext genotypesContext = GenotypesContext.create(gtNormal, gtTumor);

        double logError = -(1000 / 10.0);

        int start = 1000;
        int stop = start + ref.length() - 1;

        return builder
                .source("SOURCE")
                .chr("1")
                .start(start)
                .stop(stop)
                .alleles(alleles)
                .genotypes(genotypesContext)
                .attributes(commonAttributes)
                .log10PError(logError)
                .unfiltered()
                .make(true);
    }

}