package com.hartwig.hmftools.orange;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.serve.actionability.EvidenceLevel;
import com.hartwig.hmftools.orange.report.ImmutableReportConfig;
import com.hartwig.hmftools.orange.report.ReportConfig;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public interface OrangeConfig {

    Logger LOGGER = LogManager.getLogger(OrangeConfig.class);

    String DOID_SEPARATOR = ";";

    // General params needed for every analysis
    String TUMOR_SAMPLE_ID = "tumor_sample_id";
    String REFERENCE_SAMPLE_ID = "reference_sample_id";
    String PRIMARY_TUMOR_DOIDS = "primary_tumor_doids";
    String OUTPUT_DIRECTORY = "output_dir";

    // Input files used by the algorithm
    String DOID_JSON = "doid_json";

    // Files containing the actual genomic results for this sample.
    String PIPELINE_VERSION_FILE = "pipeline_version_file";
    String REF_SAMPLE_WGS_METRICS_FILE = "ref_sample_wgs_metrics_file";
    String REF_SAMPLE_FLAGSTAT_FILE = "ref_sample_flagstat_file";
    String TUMOR_SAMPLE_WGS_METRICS_FILE = "tumor_sample_wgs_metrics_file";
    String TUMOR_SAMPLE_FLAGSTAT_FILE = "tumor_sample_flagstat_file";
    String SAGE_GERMLINE_GENE_COVERAGE_TSV = "sage_germline_gene_coverage_tsv";
    String SAGE_SOMATIC_REF_SAMPLE_BQR_PLOT = "sage_somatic_ref_sample_bqr_plot";
    String SAGE_SOMATIC_TUMOR_SAMPLE_BQR_PLOT = "sage_somatic_tumor_sample_bqr_plot";
    String PURPLE_PURITY_TSV = "purple_purity_tsv";
    String PURPLE_QC_FILE = "purple_qc_file";
    String PURPLE_GENE_COPY_NUMBER_TSV = "purple_gene_copy_number_tsv";
    String PURPLE_SOMATIC_DRIVER_CATALOG_TSV = "purple_somatic_driver_catalog_tsv";
    String PURPLE_GERMLINE_DRIVER_CATALOG_TSV = "purple_germline_driver_catalog_tsv";
    String PURPLE_SOMATIC_VARIANT_VCF = "purple_somatic_variant_vcf";
    String PURPLE_GERMLINE_VARIANT_VCF = "purple_germline_variant_vcf";
    String PURPLE_PLOT_DIRECTORY = "purple_plot_directory";
    String LINX_FUSION_TSV = "linx_fusion_tsv";
    String LINX_BREAKEND_TSV = "linx_breakend_tsv";
    String LINX_DRIVER_CATALOG_TSV = "linx_driver_catalog_tsv";
    String LINX_DRIVER_TSV = "linx_driver_tsv";
    String LINX_PLOT_DIRECTORY = "linx_plot_directory";
    String CHORD_PREDICTION_TXT = "chord_prediction_txt";
    String CUPPA_CONCLUSION_TXT = "cuppa_conclusion_txt";
    String CUPPA_RESULT_CSV = "cuppa_result_csv";
    String CUPPA_SUMMARY_PLOT = "cuppa_summary_plot";
    String CUPPA_FEATURE_PLOT = "cuppa_feature_plot";
    String ANNOTATED_VIRUS_TSV = "annotated_virus_tsv";
    String PEACH_GENOTYPE_TSV = "peach_genotype_tsv";
    String PROTECT_EVIDENCE_TSV = "protect_evidence_tsv";

    // Some additional optional params and flags
    String DISABLE_GERMLINE = "disable_germline";
    String MAX_EVIDENCE_LEVEL = "max_evidence_level";
    String LOG_DEBUG = "log_debug";

    @NotNull
    static Options createOptions() {
        Options options = new Options();

        options.addOption(TUMOR_SAMPLE_ID, true, "The sample ID for which PROTECT will run.");
        options.addOption(REFERENCE_SAMPLE_ID, true, "(Optional) The reference sample of the tumor sample for which PROTECT will run.");
        options.addOption(PRIMARY_TUMOR_DOIDS, true, "A semicolon-separated list of DOIDs representing the primary tumor of patient.");
        options.addOption(OUTPUT_DIRECTORY, true, "Path to where the PROTECT output data will be written to.");

        options.addOption(DOID_JSON, true, "Path to JSON file containing the full DOID tree.");

        options.addOption(PIPELINE_VERSION_FILE, true, "Path towards the pipeline version file.");
        options.addOption(REF_SAMPLE_WGS_METRICS_FILE, true, "Path towards the ref sample WGS metrics file.");
        options.addOption(REF_SAMPLE_FLAGSTAT_FILE, true, "Path towards the ref sample flagstat file.");
        options.addOption(TUMOR_SAMPLE_WGS_METRICS_FILE, true, "Path towards the tumor sample WGS metrics file.");
        options.addOption(TUMOR_SAMPLE_FLAGSTAT_FILE, true, "Path towards the tumor sample flagstat file.");
        options.addOption(SAGE_GERMLINE_GENE_COVERAGE_TSV, true, "Path towards the SAGE germline gene coverage TSV.");
        options.addOption(SAGE_SOMATIC_REF_SAMPLE_BQR_PLOT, true, "Path towards the SAGE somatic ref sample BQR plot.");
        options.addOption(SAGE_SOMATIC_TUMOR_SAMPLE_BQR_PLOT, true, "Path towards the SAGE somatic tumor sample BQR plot.");
        options.addOption(PURPLE_PURITY_TSV, true, "Path towards the purple purity TSV.");
        options.addOption(PURPLE_QC_FILE, true, "Path towards the purple qc file.");
        options.addOption(PURPLE_GENE_COPY_NUMBER_TSV, true, "Path towards the purple gene copynumber TSV.");
        options.addOption(PURPLE_SOMATIC_DRIVER_CATALOG_TSV, true, "Path towards the purple somatic driver catalog TSV.");
        options.addOption(PURPLE_GERMLINE_DRIVER_CATALOG_TSV, true, "Path towards the purple germline driver catalog TSV.");
        options.addOption(PURPLE_SOMATIC_VARIANT_VCF, true, "Path towards the purple somatic variant VCF.");
        options.addOption(PURPLE_GERMLINE_VARIANT_VCF, true, "Path towards the purple germline variant VCF.");
        options.addOption(PURPLE_PLOT_DIRECTORY, true, "Path towards the directory holding all purple plots.");
        options.addOption(LINX_FUSION_TSV, true, "Path towards the LINX fusion TSV.");
        options.addOption(LINX_BREAKEND_TSV, true, "Path towards the LINX breakend TSV.");
        options.addOption(LINX_DRIVER_CATALOG_TSV, true, "Path towards the LINX driver catalog TSV.");
        options.addOption(LINX_DRIVER_TSV, true, "Path towards the LINX driver TSV.");
        options.addOption(LINX_PLOT_DIRECTORY, true, "Path towards the directory holding all linx plots.");
        options.addOption(CHORD_PREDICTION_TXT, true, "Path towards the CHORD prediction TXT.");
        options.addOption(CUPPA_CONCLUSION_TXT, true, "Path towards the Cuppa conclusion TXT.");
        options.addOption(CUPPA_RESULT_CSV, true, "Path towards the Cuppa result CSV.");
        options.addOption(CUPPA_SUMMARY_PLOT, true, "Path towards the Cuppa report summary plot PNG.");
        options.addOption(CUPPA_FEATURE_PLOT, true, "Path towards the Cuppa report feature plot PNG.");
        options.addOption(ANNOTATED_VIRUS_TSV, true, "Path towards the annotated virus TSV.");
        options.addOption(PEACH_GENOTYPE_TSV, true, "Path towards the peach genotype TSV.");
        options.addOption(PROTECT_EVIDENCE_TSV, true, "Path towards the protect evidence TSV.");

        options.addOption(LOG_DEBUG, false, "If provided, set the log level to debug rather than default.");
        options.addOption(DISABLE_GERMLINE, false, "If provided, germline results are not added to the report");
        options.addOption(MAX_EVIDENCE_LEVEL, true, "If provided, only evidence up to provided maximum level are added to report");

        return options;
    }

    @NotNull
    String tumorSampleId();

    @Nullable
    String referenceSampleId();

    @NotNull
    ReportConfig reportConfig();

    @NotNull
    Set<String> primaryTumorDoids();

    @NotNull
    String outputDir();

    @NotNull
    String doidJsonFile();

    @Nullable
    String pipelineVersionFile();

    @NotNull
    String refSampleWGSMetricsFile();

    @NotNull
    String refSampleFlagstatFile();

    @NotNull
    String tumorSampleWGSMetricsFile();

    @NotNull
    String tumorSampleFlagstatFile();

    @NotNull
    String sageGermlineGeneCoverageTsv();

    @NotNull
    String sageSomaticRefSampleBQRPlot();

    @NotNull
    String sageSomaticTumorSampleBQRPlot();

    @NotNull
    String purplePurityTsv();

    @NotNull
    String purpleQcFile();

    @NotNull
    String purpleGeneCopyNumberTsv();

    @NotNull
    String purpleSomaticDriverCatalogTsv();

    @NotNull
    String purpleGermlineDriverCatalogTsv();

    @NotNull
    String purpleSomaticVariantVcf();

    @NotNull
    String purpleGermlineVariantVcf();

    @NotNull
    String purplePlotDirectory();

    @NotNull
    String linxFusionTsv();

    @NotNull
    String linxBreakendTsv();

    @NotNull
    String linxDriverCatalogTsv();

    @NotNull
    String linxDriverTsv();

    @NotNull
    String linxPlotDirectory();

    @NotNull
    String chordPredictionTxt();

    @NotNull
    String cuppaConclusionTxt();

    @NotNull
    String cuppaResultCsv();

    @NotNull
    String cuppaSummaryPlot();

    @Nullable
    String cuppaFeaturePlot();

    @NotNull
    String annotatedVirusTsv();

    @NotNull
    String peachGenotypeTsv();

    @NotNull
    String protectEvidenceTsv();

    @NotNull
    static OrangeConfig createConfig(@NotNull CommandLine cmd) throws ParseException, IOException {
        if (cmd.hasOption(LOG_DEBUG)) {
            Configurator.setRootLevel(Level.DEBUG);
            LOGGER.debug("Switched root level logging to DEBUG");
        }

        ReportConfig report = ImmutableReportConfig.builder()
                .reportGermline(!cmd.hasOption(DISABLE_GERMLINE))
                .maxEvidenceLevel(cmd.hasOption(MAX_EVIDENCE_LEVEL)
                        ? EvidenceLevel.valueOf(cmd.getOptionValue(MAX_EVIDENCE_LEVEL))
                        : null)
                .build();

        if (!report.reportGermline()) {
            LOGGER.info("Germline reporting has been disabled");
        }

        if (report.maxEvidenceLevel() != null) {
            LOGGER.info("Max reporting level configured to {}", report.maxEvidenceLevel());
        }

        return ImmutableOrangeConfig.builder()
                .tumorSampleId(nonOptionalValue(cmd, TUMOR_SAMPLE_ID))
                .referenceSampleId(optionalValue(cmd, REFERENCE_SAMPLE_ID))
                .reportConfig(report)
                .primaryTumorDoids(toStringSet(nonOptionalValue(cmd, PRIMARY_TUMOR_DOIDS), DOID_SEPARATOR))
                .outputDir(outputDir(cmd, OUTPUT_DIRECTORY))
                .doidJsonFile(nonOptionalFile(cmd, DOID_JSON))
                .pipelineVersionFile(optionalValue(cmd, PIPELINE_VERSION_FILE))
                .refSampleWGSMetricsFile(nonOptionalValue(cmd, REF_SAMPLE_WGS_METRICS_FILE))
                .refSampleFlagstatFile(nonOptionalValue(cmd, REF_SAMPLE_FLAGSTAT_FILE))
                .tumorSampleWGSMetricsFile(nonOptionalValue(cmd, TUMOR_SAMPLE_WGS_METRICS_FILE))
                .tumorSampleFlagstatFile(nonOptionalValue(cmd, TUMOR_SAMPLE_FLAGSTAT_FILE))
                .sageGermlineGeneCoverageTsv(nonOptionalFile(cmd, SAGE_GERMLINE_GENE_COVERAGE_TSV))
                .sageSomaticRefSampleBQRPlot(nonOptionalFile(cmd, SAGE_SOMATIC_REF_SAMPLE_BQR_PLOT))
                .sageSomaticTumorSampleBQRPlot(nonOptionalFile(cmd, SAGE_SOMATIC_TUMOR_SAMPLE_BQR_PLOT))
                .purplePurityTsv(nonOptionalFile(cmd, PURPLE_PURITY_TSV))
                .purpleQcFile(nonOptionalFile(cmd, PURPLE_QC_FILE))
                .purpleGeneCopyNumberTsv(nonOptionalFile(cmd, PURPLE_GENE_COPY_NUMBER_TSV))
                .purpleSomaticDriverCatalogTsv(nonOptionalFile(cmd, PURPLE_SOMATIC_DRIVER_CATALOG_TSV))
                .purpleGermlineDriverCatalogTsv(nonOptionalFile(cmd, PURPLE_GERMLINE_DRIVER_CATALOG_TSV))
                .purpleSomaticVariantVcf(nonOptionalFile(cmd, PURPLE_SOMATIC_VARIANT_VCF))
                .purpleGermlineVariantVcf(nonOptionalFile(cmd, PURPLE_GERMLINE_VARIANT_VCF))
                .purplePlotDirectory(nonOptionalDir(cmd, PURPLE_PLOT_DIRECTORY))
                .linxFusionTsv(nonOptionalFile(cmd, LINX_FUSION_TSV))
                .linxBreakendTsv(nonOptionalFile(cmd, LINX_BREAKEND_TSV))
                .linxDriverCatalogTsv(nonOptionalFile(cmd, LINX_DRIVER_CATALOG_TSV))
                .linxDriverTsv(nonOptionalFile(cmd, LINX_DRIVER_TSV))
                .linxPlotDirectory(nonOptionalDir(cmd, LINX_PLOT_DIRECTORY))
                .chordPredictionTxt(nonOptionalFile(cmd, CHORD_PREDICTION_TXT))
                .cuppaConclusionTxt(nonOptionalFile(cmd, CUPPA_CONCLUSION_TXT))
                .cuppaResultCsv(nonOptionalFile(cmd, CUPPA_RESULT_CSV))
                .cuppaSummaryPlot(nonOptionalFile(cmd, CUPPA_SUMMARY_PLOT))
                .cuppaFeaturePlot(optionalFile(cmd, CUPPA_FEATURE_PLOT))
                .annotatedVirusTsv(nonOptionalFile(cmd, ANNOTATED_VIRUS_TSV))
                .peachGenotypeTsv(nonOptionalFile(cmd, PEACH_GENOTYPE_TSV))
                .protectEvidenceTsv(nonOptionalFile(cmd, PROTECT_EVIDENCE_TSV))
                .build();
    }

    @NotNull
    static Iterable<String> toStringSet(@NotNull String paramValue, @NotNull String separator) {
        return !paramValue.isEmpty() ? Sets.newHashSet(paramValue.split(separator)) : Sets.newHashSet();
    }

    @NotNull
    static String nonOptionalValue(@NotNull CommandLine cmd, @NotNull String param) throws ParseException {
        String value = cmd.getOptionValue(param);
        if (value == null) {
            throw new ParseException("Parameter must be provided: " + param);
        }

        return value;
    }

    @Nullable
    static String optionalValue(@NotNull CommandLine cmd, @NotNull String param) {
        String value = null;
        if (cmd.hasOption(param)) {
            value = cmd.getOptionValue(param);
        }

        if (value != null && value.isEmpty()) {
            value = null;
        }
        return value;
    }

    @NotNull
    static String outputDir(@NotNull CommandLine cmd, @NotNull String param) throws ParseException, IOException {
        String value = nonOptionalValue(cmd, param);
        File outputDir = new File(value);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Unable to write to directory " + value);
        }
        return value;
    }

    @NotNull
    static String nonOptionalDir(@NotNull CommandLine cmd, @NotNull String param) throws ParseException {
        String value = nonOptionalValue(cmd, param);

        if (!pathExists(value) || !pathIsDirectory(value)) {
            throw new ParseException("Parameter '" + param + "' must be an existing directory: " + value);
        }

        return value;
    }

    @NotNull
    static String nonOptionalFile(@NotNull CommandLine cmd, @NotNull String param) throws ParseException {
        String value = nonOptionalValue(cmd, param);

        if (!pathExists(value)) {
            throw new ParseException("Parameter '" + param + "' must be an existing file: " + value);
        }

        return value;
    }

    @Nullable
    static String optionalFile(@NotNull CommandLine cmd, @NotNull String param) throws ParseException {
        String value = optionalValue(cmd, param);

        if (value != null && !pathExists(value)) {
            throw new ParseException("Parameter '" + param + "' must be an existing file: " + value);
        }

        return value;
    }

    static boolean pathExists(@NotNull String path) {
        return Files.exists(new File(path).toPath());
    }

    static boolean pathIsDirectory(@NotNull String path) {
        return Files.isDirectory(new File(path).toPath());
    }
}
