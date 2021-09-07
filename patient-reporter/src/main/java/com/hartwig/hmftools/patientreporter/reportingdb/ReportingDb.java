package com.hartwig.hmftools.patientreporter.reportingdb;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

import com.hartwig.hmftools.common.lims.cohort.LimsCohortConfig;
import com.hartwig.hmftools.common.lims.reportingdb.ReportingDatabase;
import com.hartwig.hmftools.common.lims.reportingdb.ReportingEntry;
import com.hartwig.hmftools.patientreporter.SampleReport;
import com.hartwig.hmftools.patientreporter.algo.AnalysedPatientReport;
import com.hartwig.hmftools.patientreporter.algo.GenomicAnalysis;
import com.hartwig.hmftools.patientreporter.cfreport.ReportResources;
import com.hartwig.hmftools.patientreporter.qcfail.QCFailReport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ReportingDb {

    private static final Logger LOGGER = LogManager.getLogger(ReportingDb.class);

    private static final String NA_STRING = "N/A";

    @NotNull
    private final String reportingDbTsv;

    public ReportingDb(@NotNull final String reportingDbTsv) {
        this.reportingDbTsv = reportingDbTsv;
    }

    public void appendAnalysedReport(@NotNull AnalysedPatientReport report) throws IOException {
        if (shouldBeAddedToReportingDb(report.sampleReport())) {
            String sampleId = report.sampleReport().tumorSampleId();
            LimsCohortConfig cohort = report.sampleReport().cohort();

            if (report.sampleReport().cohort().reportConclusion() && report.clinicalSummary().isEmpty()) {
                LOGGER.warn("Skipping addition to reporting db, missing summary for sample '{}'!", sampleId);
            } else {
                String tumorBarcode = report.sampleReport().tumorSampleBarcode();
                String reportDate = ReportResources.REPORT_DATE;
                GenomicAnalysis analysis = report.genomicAnalysis();

                String purity = new DecimalFormat("0.00").format(analysis.impliedPurity());
                boolean hasReliableQuality = analysis.hasReliableQuality();
                boolean hasReliablePurity = analysis.hasReliablePurity();

                String reportType;
                if (hasReliablePurity && analysis.impliedPurity() > ReportResources.PURITY_CUTOFF) {
                    reportType = "dna_analysis_report";
                } else {
                    reportType = "dna_analysis_report_insufficient_tcp";
                }

                if (report.isCorrectedReport()) {
                    reportType = reportType + "_corrected";
                }

                addToReportingDb(tumorBarcode, sampleId, cohort, reportType, reportDate, purity, hasReliableQuality, hasReliablePurity);
            }
        }
    }

    private void addToReportingDb(@NotNull String tumorBarcode, @NotNull String sampleId, @NotNull LimsCohortConfig cohort,
            @NotNull String reportType, @NotNull String reportDate, @NotNull String purity, boolean hasReliableQuality,
            boolean hasReliablePurity) throws IOException {
        boolean present = false;
        for (ReportingEntry entry : ReportingDatabase.read(reportingDbTsv)) {
            if (!present && sampleId.equals(entry.sampleId()) && tumorBarcode.equals(entry.tumorBarcode())
                    && reportType.equals(entry.reportType())) {
                LOGGER.warn("Sample {} has already been reported with report type '{}'!", sampleId, reportType);
                present = true;
            }
        }

        if (!present) {
            LOGGER.info("Adding {} to reporting db at {} with type '{}'", sampleId, reportingDbTsv, reportType);
            String stringToAppend =
                    tumorBarcode + "\t" + sampleId + "\t" + cohort.cohortId() + "\t" + reportDate + "\t" + reportType + "\t" + purity + "\t"
                            + hasReliableQuality + "\t" + hasReliablePurity + "\n";
            appendToTsv(reportingDbTsv, stringToAppend);
        }
    }

    public void appendQCFailReport(@NotNull QCFailReport report) throws IOException {
        if (shouldBeAddedToReportingDb(report.sampleReport())) {
            String sampleId = report.sampleReport().tumorSampleId();
            LimsCohortConfig cohort = report.sampleReport().cohort();
            String tumorBarcode = report.sampleReport().tumorSampleBarcode();
            String reportDate = ReportResources.REPORT_DATE;

            String reportType = report.isCorrectedReport() ? report.reason().identifier() + "_corrected" : report.reason().identifier();

            boolean present = false;
            for (ReportingEntry entry : ReportingDatabase.read(reportingDbTsv)) {
                if (!present && sampleId.equals(entry.sampleId()) && tumorBarcode.equals(entry.tumorBarcode())
                        && reportType.equals(entry.reportType()) && reportDate.equals(entry.reportDate())) {
                    LOGGER.warn("Sample {} has already been reported with report type '{}' on {}!", sampleId, reportType, reportDate);
                    present = true;
                }
            }

            if (!present) {
                LOGGER.info("Adding {} to reporting db at {} with type '{}'", sampleId, reportingDbTsv, reportType);
                String stringToAppend =
                        tumorBarcode + "\t" + sampleId + "\t" + cohort.cohortId() + "\t" + reportDate + "\t" + reportType + "\t" + NA_STRING
                                + "\t" + NA_STRING + "\t" + NA_STRING + "\n";
                appendToTsv(reportingDbTsv, stringToAppend);
            }
        }
    }

    private static boolean shouldBeAddedToReportingDb(@NotNull SampleReport report) {
        String sampleId = report.tumorSampleId();
        if (sampleId.startsWith("COLO")) {
            LOGGER.info("Sample '{}' filtered for reporting db because it appears to be belong to COLO test samples", sampleId);
            return false;
        } else if (report.cohort().cohortId().isEmpty()) {
            LOGGER.info("Sample '{}' filtered for reporting db since it does not belong to a cohort and likely a test sample", sampleId);
            return false;
        }
        return true;
    }

    private static void appendToTsv(@NotNull String reportingDbTsv, @NotNull String stringToAppend) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(reportingDbTsv, true));
        writer.write(stringToAppend);
        writer.close();
    }
}
