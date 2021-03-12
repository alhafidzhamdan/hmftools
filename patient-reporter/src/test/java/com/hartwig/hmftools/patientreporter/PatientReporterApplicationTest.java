package com.hartwig.hmftools.patientreporter;

import static org.junit.Assert.assertNotNull;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.variant.CodingEffect;
import com.hartwig.hmftools.common.variant.Hotspot;
import com.hartwig.hmftools.common.variant.VariantType;
import com.hartwig.hmftools.patientreporter.algo.AnalysedPatientReport;
import com.hartwig.hmftools.patientreporter.algo.ImmutableAnalysedPatientReport;
import com.hartwig.hmftools.patientreporter.algo.ImmutableGenomicAnalysis;
import com.hartwig.hmftools.protect.purple.ImmutableReportableVariant;
import com.hartwig.hmftools.protect.purple.ReportableVariant;
import com.hartwig.hmftools.protect.purple.ReportableVariantSource;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class PatientReporterApplicationTest {

    @Test
    public void canConvertNaNDataToJson() {
        AnalysedPatientReport patientReport = ExampleAnalysisTestFactory.buildTestReport();

        PatientReport nanReport = ImmutableAnalysedPatientReport.builder()
                .from(patientReport)
                .genomicAnalysis(ImmutableGenomicAnalysis.builder()
                        .from(patientReport.genomicAnalysis())
                        .reportableVariants(Lists.newArrayList(nanVariant()))
                        .build())
                .build();

        assertNotNull(PatientReporterApplication.convertToJson(nanReport));
    }

    @NotNull
    private static ReportableVariant nanVariant() {
        return ImmutableReportableVariant.builder()
                .source(ReportableVariantSource.SOMATIC)
                .gene(Strings.EMPTY)
                .chromosome(Strings.EMPTY)
                .position(1)
                .ref(Strings.EMPTY)
                .alt(Strings.EMPTY)
                .type(VariantType.SNP)
                .canonicalTranscript(Strings.EMPTY)
                .canonicalCodingEffect(CodingEffect.MISSENSE)
                .canonicalHgvsCodingImpact(Strings.EMPTY)
                .canonicalHgvsProteinImpact(Strings.EMPTY)
                .alleleReadCount(0)
                .totalReadCount(1)
                .alleleCopyNumber(Double.NaN)
                .totalCopyNumber(Double.NaN)
                .hotspot(Hotspot.HOTSPOT)
                .driverLikelihood(0D)
                .clonalLikelihood(0D)
                .biallelic(false)
                .build();
    }

}