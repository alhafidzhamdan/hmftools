package com.hartwig.hmftools.patientreporter.algo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import com.hartwig.hmftools.patientreporter.ImmutableSampleMetadata;
import com.hartwig.hmftools.patientreporter.PatientReporterConfig;
import com.hartwig.hmftools.patientreporter.PatientReporterTestFactory;
import com.hartwig.hmftools.patientreporter.QsFormNumber;
import com.hartwig.hmftools.patientreporter.SampleMetadata;

import org.apache.logging.log4j.util.Strings;
import org.junit.Assert;
import org.junit.Test;

public class AnalysedPatientReporterTest {

    private static final String REF_SAMPLE_ID = "ref_sample";
    private static final String TUMOR_SAMPLE_ID = "sample";

    @Test
    public void canRunOnRunDirectory() throws IOException {
        AnalysedPatientReporter reporter = new AnalysedPatientReporter(PatientReporterTestFactory.loadTestAnalysedReportData());
        PatientReporterConfig config = PatientReporterTestFactory.createTestReporterConfig();

        SampleMetadata sampleMetadata = ImmutableSampleMetadata.builder()
                .refSampleId(REF_SAMPLE_ID)
                .refSampleIdDuringRun(REF_SAMPLE_ID)
                .refSampleBarcode(Strings.EMPTY)
                .tumorSampleId(TUMOR_SAMPLE_ID)
                .tumorSampleIdDuringRun(TUMOR_SAMPLE_ID)
                .tumorSampleBarcode(Strings.EMPTY)
                .build();

        assertNotNull(reporter.run(sampleMetadata, config));
    }

    @Test
    public void canDetermineForNumber() {
        double purityCorrect = 0.40;
        boolean hasReliablePurityCorrect = true;

        Assert.assertEquals(QsFormNumber.FOR_080.display(),
                AnalysedPatientReporter.determineForNumber(hasReliablePurityCorrect, purityCorrect));

        double purityNotCorrect = 0.10;
        boolean hasReliablePurityNotCorrect = false;

        assertEquals(QsFormNumber.FOR_209.display(),
                AnalysedPatientReporter.determineForNumber(hasReliablePurityNotCorrect, purityNotCorrect));
    }

    @Test
    public void testPipelineVersion() {
        AnalysedPatientReporter.checkPipelineVersion("5.22", "5.22", false);
        AnalysedPatientReporter.checkPipelineVersion("5.22", "5.22", true);
        AnalysedPatientReporter.checkPipelineVersion("5.22", "5.21", true);
        AnalysedPatientReporter.checkPipelineVersion(null, "5.21", true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void crashTestPipelineVersionOnNoActual() {
        AnalysedPatientReporter.checkPipelineVersion(null, "5.21", false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void crashTestPipelineVersionOnVersionDifference() {
        AnalysedPatientReporter.checkPipelineVersion("5.22", "5.21", false);
    }
}
