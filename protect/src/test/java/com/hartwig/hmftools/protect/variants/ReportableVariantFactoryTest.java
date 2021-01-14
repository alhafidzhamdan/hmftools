package com.hartwig.hmftools.protect.variants;

import static com.hartwig.hmftools.protect.variants.ReportableVariantFactory.reportableGermlineVariants;
import static com.hartwig.hmftools.protect.variants.ReportableVariantFactory.reportableSomaticVariants;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.drivercatalog.DriverCatalog;
import com.hartwig.hmftools.common.drivercatalog.DriverCategory;
import com.hartwig.hmftools.common.drivercatalog.DriverType;
import com.hartwig.hmftools.common.drivercatalog.ImmutableDriverCatalog;
import com.hartwig.hmftools.common.drivercatalog.LikelihoodMethod;
import com.hartwig.hmftools.common.variant.CodingEffect;
import com.hartwig.hmftools.common.variant.SomaticVariant;
import com.hartwig.hmftools.common.variant.SomaticVariantTestBuilderFactory;
import com.hartwig.hmftools.common.variant.germline.ImmutableReportableGermlineVariant;
import com.hartwig.hmftools.common.variant.germline.ReportableGermlineVariant;
import com.hartwig.hmftools.protect.ProtectTestFactory;
import com.hartwig.hmftools.protect.variants.germline.GermlineReportingModel;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class ReportableVariantFactoryTest {

    private static final double EPSILON = 1.0e-10;
    private static final String GENE = "Gene";

    @Test
    public void singleHitNotIncluded() {
        GermlineReportingModel germlineReportingModel = ProtectTestFactory.createTestGermlineModel(GENE, true, null);
        List<ReportableGermlineVariant> variants = Lists.newArrayList(create(GENE, false));

        List<ReportableVariant> victims = reportableGermlineVariants(variants, Collections.emptySet(), germlineReportingModel);
        assertEquals(0, victims.size());
    }

    @Test
    public void singleHitIncludedWhenAllowed() {
        GermlineReportingModel germlineReportingModel = ProtectTestFactory.createTestGermlineModel(GENE, false, null);
        List<ReportableGermlineVariant> variants = Lists.newArrayList(create(GENE, false));

        List<ReportableVariant> victims = reportableGermlineVariants(variants, Collections.emptySet(), germlineReportingModel);
        assertEquals(1, victims.size());
        assertEquals(GENE, victims.get(0).gene());
    }

    @Test
    public void biallelicIncluded() {
        GermlineReportingModel germlineReportingModel = ProtectTestFactory.createTestGermlineModel(GENE, true, null);
        List<ReportableGermlineVariant> variants = Lists.newArrayList(create(GENE, true));

        List<ReportableVariant> victims = reportableGermlineVariants(variants, Collections.emptySet(), germlineReportingModel);
        assertEquals(1, victims.size());
        assertEquals(GENE, victims.get(0).gene());
    }

    @Test
    public void biallelicExcludedWhenNotInGermlineList() {
        GermlineReportingModel germlineReportingModel = ProtectTestFactory.createEmptyGermlineReportingModel();
        List<ReportableGermlineVariant> variants = Lists.newArrayList(create(GENE, true));

        List<ReportableVariant> victims = reportableGermlineVariants(variants, Collections.emptySet(), germlineReportingModel);
        assertEquals(0, victims.size());
    }

    @Test
    public void biallelicExcludedWhenNotInTumor() {
        GermlineReportingModel germlineReportingModel = ProtectTestFactory.createTestGermlineModel(GENE, true, null);
        List<ReportableGermlineVariant> variants = Lists.newArrayList(create(GENE, 1, "protein", 0.1, true));

        List<ReportableVariant> victims = reportableGermlineVariants(variants, Collections.emptySet(), germlineReportingModel);
        assertEquals(0, victims.size());
    }

    @Test
    public void singleHitIncludedOnExtraGermlineHit() {
        GermlineReportingModel germlineReportingModel = ProtectTestFactory.createTestGermlineModel(GENE, true, null);
        List<ReportableGermlineVariant> variants = Lists.newArrayList(create(GENE, 1, false), create(GENE, 2, false));

        List<ReportableVariant> victims = reportableGermlineVariants(variants, Collections.emptySet(), germlineReportingModel);
        assertEquals(2, victims.size());
        assertEquals(GENE, victims.get(0).gene());
        assertEquals(GENE, victims.get(1).gene());
    }

    @Test
    public void singleHitIncludedOnExtraSomaticHit() {
        GermlineReportingModel germlineReportingModel = ProtectTestFactory.createTestGermlineModel(GENE, true, null);
        List<ReportableGermlineVariant> variants = Lists.newArrayList(create(GENE, false));

        List<ReportableVariant> victims = reportableGermlineVariants(variants, Collections.singleton(GENE), germlineReportingModel);
        assertEquals(1, victims.size());
        assertEquals(GENE, victims.get(0).gene());
    }

    @Test
    public void exclusiveHgvsProteinFilterWorks() {
        GermlineReportingModel germlineReportingModel = ProtectTestFactory.createTestGermlineModel(GENE, true, "proteinMatch");
        List<ReportableGermlineVariant> variantMatch = Lists.newArrayList(create(GENE, 1, "proteinMatch", 0.4, false));

        List<ReportableVariant> victimsMatch = reportableGermlineVariants(variantMatch, Collections.emptySet(), germlineReportingModel);
        assertEquals(1, victimsMatch.size());
        assertEquals(GENE, victimsMatch.get(0).gene());

        List<ReportableGermlineVariant> variantsNonMatch = Lists.newArrayList(create(GENE, 1, "weirdProtein", 0.4, false));
        List<ReportableVariant> victimsNonMatch =
                reportableGermlineVariants(variantsNonMatch, Collections.emptySet(), germlineReportingModel);
        assertEquals(0, victimsNonMatch.size());
    }

    @Test
    public void canResolveReportableSomaticVariants() {
        String gene1 = "gene1";
        String gene2 = "gene2";
        SomaticVariant variant1 = SomaticVariantTestBuilderFactory.create().reported(true).gene(gene1).build();
        SomaticVariant variant2 = SomaticVariantTestBuilderFactory.create().reported(false).gene(gene2).build();

        double likelihood = 0.6;
        DriverCatalog driverGene1 = createMutationEntryForGene(gene1, likelihood);
        List<ReportableVariant> reportable =
                reportableSomaticVariants(Lists.newArrayList(variant1, variant2), Lists.newArrayList(driverGene1));

        assertEquals(1, reportable.size());
        assertEquals(likelihood, reportable.get(0).driverLikelihood(), EPSILON);
    }

    @NotNull
    private static DriverCatalog createMutationEntryForGene(@NotNull String gene, double likelihood) {
        return ImmutableDriverCatalog.builder()
                .chromosome(Strings.EMPTY)
                .chromosomeBand(Strings.EMPTY)
                .gene(gene)
                .driver(DriverType.MUTATION)
                .category(DriverCategory.ONCO)
                .likelihoodMethod(LikelihoodMethod.DNDS)
                .driverLikelihood(likelihood)
                .dndsLikelihood(0D)
                .missense(0)
                .nonsense(0)
                .splice(0)
                .inframe(0)
                .frameshift(0)
                .biallelic(false)
                .minCopyNumber(0)
                .maxCopyNumber(0)
                .build();
    }

    @NotNull
    private static ReportableGermlineVariant create(@NotNull String gene, boolean biallelic) {
        return create(gene, 1, biallelic);
    }

    @NotNull
    private static ReportableGermlineVariant create(@NotNull String gene, int position, boolean biallelic) {
        return create(gene, position, "protein", 0.4, biallelic);
    }

    @NotNull
    private static ReportableGermlineVariant create(@NotNull String gene, int position, @NotNull String hgvsProtein, double adjustedVaf,
            boolean biallelic) {
        return ImmutableReportableGermlineVariant.builder()
                .gene(gene)
                .chromosome("1")
                .biallelic(biallelic)
                .position(position)
                .ref("C")
                .alt("G")
                .codingEffect(CodingEffect.MISSENSE)
                .hgvsCoding("coding")
                .hgvsProtein(hgvsProtein)
                .alleleReadCount(1)
                .totalReadCount(10)
                .adjustedVaf(adjustedVaf)
                .adjustedCopyNumber(2)
                .build();
    }
}
