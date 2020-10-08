package com.hartwig.hmftools.protect.variants;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.lims.LimsGermlineReportingLevel;
import com.hartwig.hmftools.protect.ProtectTestFactory;
import com.hartwig.hmftools.protect.variants.germline.DriverGermlineVariant;
import com.hartwig.hmftools.protect.variants.germline.GermlineReportingModel;
import com.hartwig.hmftools.protect.variants.germline.ImmutableDriverGermlineVariant;
import com.hartwig.hmftools.protect.variants.somatic.DriverSomaticVariant;
import com.hartwig.hmftools.protect.variants.somatic.ImmutableDriverSomaticVariant;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class ReportableVariantAnalyzerTest {

    private static final double EPSILON = 1.0e-10;
    private static final String ONCO = "KIT";
    private static final String TSG = "PTEN";

    @Test
    public void overruleDriverLikelihoodOfSomaticVariants() {
        DriverSomaticVariant variant1 = ImmutableDriverSomaticVariant.builder()
                .variant(ProtectTestFactory.createTestSomaticVariantBuilder().gene(ONCO).build())
                .driverLikelihood(0D)
                .build();
        DriverSomaticVariant variant2 = ImmutableDriverSomaticVariant.builder()
                .variant(ProtectTestFactory.createTestSomaticVariantBuilder().gene(TSG).build())
                .driverLikelihood(0D)
                .build();
        List<DriverSomaticVariant> somaticVariantsToReport = Lists.newArrayList(variant1, variant2);
        List<DriverGermlineVariant> germlineVariantsToReport = createBiallelicGermlineVariantsOnOncoAndTSG();

        ReportableVariantAnalysis reportableVariantsAnalysis =
                ReportableVariantAnalyzer.mergeSomaticAndGermlineVariants(somaticVariantsToReport,
                        germlineVariantsToReport,
                        ProtectTestFactory.createTestEmptyGermlineGenesReporting(),
                        LimsGermlineReportingLevel.REPORT_WITH_NOTIFICATION,
                        ProtectTestFactory.loadTestActionabilityAnalyzer(),
                        null);

        List<ReportableVariant> reportableVariants = reportableVariantsAnalysis.variantsToReport();

        assertEquals(4, reportableVariants.size());
        assertEquals(1.0, reportableVariants.get(0).driverLikelihood(), EPSILON);
        assertEquals(0.5, reportableVariants.get(1).driverLikelihood(), EPSILON);
        assertEquals(1.0, reportableVariants.get(2).driverLikelihood(), EPSILON);
        assertEquals(0.5, reportableVariants.get(3).driverLikelihood(), EPSILON);
    }

    @Test
    public void mergeSomaticAndGermlineVariantWithNotifyGermline() {
        List<DriverSomaticVariant> somaticVariantsToReport = Lists.newArrayList(ImmutableDriverSomaticVariant.builder()
                .variant(ProtectTestFactory.createTestSomaticVariantBuilder().gene(TSG).build())
                .driverLikelihood(0D)
                .build());

        List<DriverGermlineVariant> germlineVariantsToReport = createBiallelicGermlineVariantsOnOncoAndTSG();
        GermlineReportingModel germlineReportingModel = createGermlineReportingModelWithOncoAndTSG();

        ReportableVariantAnalysis reportableVariantsAnalysis =
                ReportableVariantAnalyzer.mergeSomaticAndGermlineVariants(somaticVariantsToReport,
                        germlineVariantsToReport,
                        germlineReportingModel,
                        LimsGermlineReportingLevel.REPORT_WITH_NOTIFICATION,
                        ProtectTestFactory.loadTestActionabilityAnalyzer(),
                        null);

        List<ReportableVariant> reportableVariants = reportableVariantsAnalysis.variantsToReport();

        assertEquals(3, reportableVariants.size());
        assertEquals(TSG, reportableVariants.get(0).gene());
        assertFalse(reportableVariants.get(0).notifyClinicalGeneticist());

        assertEquals(ONCO, reportableVariants.get(1).gene());
        assertTrue(reportableVariants.get(1).notifyClinicalGeneticist());
        assertTrue(reportableVariants.get(1).biallelic());

        assertEquals(TSG, reportableVariants.get(2).gene());
        assertFalse(reportableVariants.get(2).notifyClinicalGeneticist());
        assertFalse(reportableVariants.get(2).biallelic());
    }

    @Test
    public void mergeSomaticAndGermlineVariantWithoutNotifyGermline() {
        List<DriverSomaticVariant> somaticVariantsToReport = Lists.newArrayList(ImmutableDriverSomaticVariant.builder()
                .variant(ProtectTestFactory.createTestSomaticVariantBuilder().gene(TSG).build())
                .driverLikelihood(0D)
                .build());

        List<DriverGermlineVariant> germlineVariantsToReport = createBiallelicGermlineVariantsOnOncoAndTSG();
        GermlineReportingModel germlineReportingModel = createGermlineReportingModelWithOncoAndTSG();

        ReportableVariantAnalysis reportableVariantsAnalysis =
                ReportableVariantAnalyzer.mergeSomaticAndGermlineVariants(somaticVariantsToReport,
                        germlineVariantsToReport,
                        germlineReportingModel,
                        LimsGermlineReportingLevel.NO_REPORTING,
                        ProtectTestFactory.loadTestActionabilityAnalyzer(),
                        null);

        List<ReportableVariant> reportableVariants = reportableVariantsAnalysis.variantsToReport();

        assertEquals(3, reportableVariants.size());
        assertEquals(TSG, reportableVariants.get(0).gene());
        assertFalse(reportableVariants.get(0).notifyClinicalGeneticist());
        assertTrue(reportableVariants.get(0).biallelic());

        assertEquals(ONCO, reportableVariants.get(1).gene());
        assertFalse(reportableVariants.get(1).notifyClinicalGeneticist());
        assertTrue(reportableVariants.get(1).biallelic());

        assertEquals(TSG, reportableVariants.get(2).gene());
        assertFalse(reportableVariants.get(2).notifyClinicalGeneticist());
        assertFalse(reportableVariants.get(2).biallelic());
    }

    @NotNull
    private static GermlineReportingModel createGermlineReportingModelWithOncoAndTSG() {
        Map<String, Boolean> germlineGenesReportingMap = Maps.newHashMap();
        germlineGenesReportingMap.put(ONCO, true);
        germlineGenesReportingMap.put(TSG, false);
        return new GermlineReportingModel(germlineGenesReportingMap);
    }

    @NotNull
    private static List<DriverGermlineVariant> createBiallelicGermlineVariantsOnOncoAndTSG() {
        DriverGermlineVariant germlineVariant1 = ImmutableDriverGermlineVariant.builder()
                .variant(ProtectTestFactory.createTestGermlineVariantBuilder()
                        .gene(ONCO)
                        .biallelic(true)
                        .adjustedVaf(0)
                        .adjustedCopyNumber(0)
                        .build())
                .driverLikelihood(1.0)
                .build();
        DriverGermlineVariant germlineVariant2 = ImmutableDriverGermlineVariant.builder()
                .variant(ProtectTestFactory.createTestGermlineVariantBuilder()
                        .gene(TSG)
                        .biallelic(false)
                        .adjustedVaf(0)
                        .adjustedCopyNumber(0)
                        .build())
                .driverLikelihood(0.5)
                .build();
        return Lists.newArrayList(germlineVariant1, germlineVariant2);
    }
}