package com.hartwig.hmftools.patientreporter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.hartwig.hmftools.common.actionability.ActionabilitySource;
import com.hartwig.hmftools.common.actionability.ClinicalTrial;
import com.hartwig.hmftools.common.actionability.EvidenceItem;
import com.hartwig.hmftools.common.actionability.EvidenceLevel;
import com.hartwig.hmftools.common.actionability.EvidenceScope;
import com.hartwig.hmftools.common.actionability.ImmutableClinicalTrial;
import com.hartwig.hmftools.common.actionability.ImmutableEvidenceItem;
import com.hartwig.hmftools.common.chord.ChordStatus;
import com.hartwig.hmftools.common.clinical.ImmutablePatientPrimaryTumor;
import com.hartwig.hmftools.common.lims.Lims;
import com.hartwig.hmftools.common.lims.LimsGermlineReportingLevel;
import com.hartwig.hmftools.common.lims.cohort.ImmutableLimsCohortConfigData;
import com.hartwig.hmftools.common.lims.cohort.ImmutableLimsCohortModel;
import com.hartwig.hmftools.common.lims.cohort.LimsCohortConfigData;
import com.hartwig.hmftools.common.lims.cohort.LimsCohortModel;
import com.hartwig.hmftools.common.lims.hospital.HospitalContactData;
import com.hartwig.hmftools.common.lims.hospital.ImmutableHospitalContactData;
import com.hartwig.hmftools.common.purple.copynumber.CopyNumberInterpretation;
import com.hartwig.hmftools.common.purple.copynumber.ImmutableReportableGainLoss;
import com.hartwig.hmftools.common.purple.copynumber.ReportableGainLoss;
import com.hartwig.hmftools.common.variant.CodingEffect;
import com.hartwig.hmftools.common.variant.Hotspot;
import com.hartwig.hmftools.common.variant.msi.MicrosatelliteStatus;
import com.hartwig.hmftools.common.variant.structural.linx.FusionLikelihoodType;
import com.hartwig.hmftools.common.variant.structural.linx.FusionPhasedType;
import com.hartwig.hmftools.common.variant.structural.linx.ImmutableLinxFusion;
import com.hartwig.hmftools.common.variant.structural.linx.LinxFusion;
import com.hartwig.hmftools.common.variant.tml.TumorMutationalStatus;
import com.hartwig.hmftools.patientreporter.qcfail.ImmutableQCFailReport;
import com.hartwig.hmftools.patientreporter.qcfail.QCFailReason;
import com.hartwig.hmftools.patientreporter.qcfail.QCFailReport;
import com.hartwig.hmftools.protect.GenomicAnalysis;
import com.hartwig.hmftools.protect.ImmutableGenomicAnalysis;
import com.hartwig.hmftools.protect.homozygousdisruption.ImmutableReportableHomozygousDisruption;
import com.hartwig.hmftools.protect.homozygousdisruption.ReportableHomozygousDisruption;
import com.hartwig.hmftools.protect.structural.ImmutableReportableGeneDisruption;
import com.hartwig.hmftools.protect.structural.ReportableGeneDisruption;
import com.hartwig.hmftools.protect.variants.ImmutableReportableVariant;
import com.hartwig.hmftools.protect.variants.ReportableVariant;
import com.hartwig.hmftools.protect.variants.ReportableVariantSource;
import com.hartwig.hmftools.protect.viralinsertion.ImmutableViralInsertion;
import com.hartwig.hmftools.protect.viralinsertion.ViralInsertion;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ExampleAnalysisTestFactory {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
    private static final String CIRCOS_PATH = Resources.getResource("test_run/purple/plot/sample.circos.png").getPath();

    private ExampleAnalysisTestFactory() {
    }

    @NotNull
    public static AnalysedPatientReport buildCOLO829(@NotNull String sampleId, boolean correctionReport, @Nullable String comments,
            @NotNull LimsCohortConfigData limsCohortConfigData) {

        return buildWithCOLO829Data(sampleId, correctionReport, comments, QsFormNumber.FOR_080.display(), true, 1D, true, false, limsCohortConfigData);
    }

    @NotNull
    public static AnalysedPatientReport buildWithCOLO829Data(@NotNull String sampleId, boolean correctionReport, @Nullable String comments,
            @NotNull String qcForNumber, boolean hasReliablePurity, double impliedTumorPurity, boolean includeSummary,
            boolean reportGermline,  @NotNull LimsCohortConfigData limsCohortConfigData) {
        double averageTumorPloidy = 3.1;
        int tumorMutationalLoad = 190;
        double tumorMutationalBurden = 13.7;
        double microsatelliteIndelsPerMb = 0.12;
        double chordHrdValue = 0D;
        ChordStatus chordStatus = ChordStatus.HR_PROFICIENT;

        ReportData reportData = PatientReporterTestFactory.loadTestReportData();

        List<EvidenceItem> tumorSpecificEvidence = createCOLO829TumorSpecificEvidence();
        List<ClinicalTrial> clinicalTrials = createCOLO829ClinicalTrials();
        List<EvidenceItem> offLabelEvidence = createCOLO829OffLabelEvidence();
        List<ReportableVariant> reportableVariants = createCOLO829SomaticVariants(reportGermline);
        List<ReportableGainLoss> gainsAndLosses = createCOLO829GainsLosses();
        List<LinxFusion> fusions = Lists.newArrayList();
        List<ReportableHomozygousDisruption> homozygousDisruptions = Lists.newArrayList();
        List<ReportableGeneDisruption> disruptions = createCOLO829Disruptions();
        List<ViralInsertion> viralInsertions = Lists.newArrayList();

        SampleReport sampleReport = createSkinMelanomaSampleReport(sampleId, reportGermline, limsCohortConfigData);

        String summaryWithoutGermline = "Melanoma sample showing:\n"
                + " - activating BRAF mutation that is associated with response to BRAF-inhibitors (in combination with a MEK-inhibitor)\n"
                + " - complete inactivation of CDKN2A, indicating potential benefit of CDK4/6 inhibitors\n"
                + " - complete inactivation/loss of PTEN likely resulting in an activation of the PI3K-AKT-mTOR pathway "
                + "and indicating potential benefit of mTOR/PI3K inhibitors\n"
                + " - high mutational burden (mutational load (ML) of 180, tumor mutation burden (TMB) of 13.6) that is "
                + "potentially associated with an increased response rate to checkpoint inhibitor immunotherapy";

        String summaryWithGermline = "Melanoma sample showing:\n"
                + " - activating BRAF mutation that is associated with response to BRAF-inhibitors (in combination with a MEK-inhibitor)\n"
                + " - complete inactivation of CDKN2A, indicating potential benefit of CDK4/6 inhibitors. The observed CDKN2A mutation is "
                + "also present in the germline of the patient. Referral to a genetic specialist should be considered.\n"
                + " - complete inactivation/loss of PTEN likely resulting in an activation of the PI3K-AKT-mTOR pathway "
                + "and indicating potential benefit of mTOR/PI3K inhibitors\n"
                + " - high mutational burden (mutational load (ML) of 180, tumor mutation burden (TMB) of 13.6) that is "
                + "potentially associated with an increased response rate to checkpoint inhibitor immunotherapy";

        String clinicalSummary;
        if (includeSummary && !reportGermline) {
            clinicalSummary = summaryWithoutGermline;
        } else if (includeSummary && reportGermline) {
            clinicalSummary = summaryWithGermline;
        } else {
            clinicalSummary = Strings.EMPTY;
        }

        GenomicAnalysis analysis = ImmutableGenomicAnalysis.builder()
                .impliedPurity(impliedTumorPurity)
                .hasReliablePurity(hasReliablePurity)
                .hasReliableQuality(true)
                .averageTumorPloidy(averageTumorPloidy)
                .tumorSpecificEvidence(tumorSpecificEvidence)
                .clinicalTrials(clinicalTrials)
                .offLabelEvidence(offLabelEvidence)
                .reportableVariants(reportableVariants)
                .microsatelliteIndelsPerMb(microsatelliteIndelsPerMb)
                .microsatelliteStatus(MicrosatelliteStatus.fromIndelsPerMb(microsatelliteIndelsPerMb))
                .tumorMutationalLoad(tumorMutationalLoad)
                .tumorMutationalLoadStatus(TumorMutationalStatus.fromLoad(tumorMutationalLoad))
                .tumorMutationalBurden(tumorMutationalBurden)
                .chordHrdValue(chordHrdValue)
                .chordHrdStatus(chordStatus)
                .gainsAndLosses(gainsAndLosses)
                .geneFusions(fusions)
                .geneDisruptions(disruptions)
                .homozygousDisruptions(homozygousDisruptions)
                .viralInsertions(viralInsertions)
                .build();

        return ImmutableAnalysedPatientReport.builder()
                .sampleReport(sampleReport)
                .qsFormNumber(qcForNumber)
                .clinicalSummary(clinicalSummary)
                .genomicAnalysis(analysis)
                .circosPath(CIRCOS_PATH)
                .comments(Optional.ofNullable(comments))
                .isCorrectedReport(correctionReport)
                .signaturePath(reportData.signaturePath())
                .logoRVAPath(reportData.logoRVAPath())
                .logoCompanyPath(reportData.logoCompanyPath())
                .build();
    }

    @NotNull
    public static AnalysedPatientReport buildAnalysisWithAllTablesFilledInAndReliablePurity(@NotNull String sampleId,
            @Nullable String comments, @NotNull LimsCohortConfigData limsCohortConfigData) {
        return buildAnalysisWithAllTablesFilledIn(sampleId, comments, true, 1D, limsCohortConfigData);
    }

    @NotNull
    public static AnalysedPatientReport buildAnalysisWithAllTablesFilledIn(@NotNull String sampleId, @Nullable String comments,
            boolean hasReliablePurity, double impliedTumorPurity, @NotNull LimsCohortConfigData limsCohortConfigData) {
        double averageTumorPloidy = 3.1;
        int tumorMutationalLoad = 182;
        double tumorMutationalBurden = 13.6;
        double microsatelliteIndelsPerMb = 0.1089;
        double chordHrdValue = 0.8;
        ChordStatus chordStatus = ChordStatus.HR_DEFICIENT;

        ReportData reportData = PatientReporterTestFactory.loadTestReportData();

        List<EvidenceItem> tumorSpecificEvidence = createCOLO829TumorSpecificEvidence();
        List<ClinicalTrial> clinicalTrials = createCOLO829ClinicalTrials();
        List<EvidenceItem> offLabelEvidence = createCOLO829OffLabelEvidence();
        List<ReportableVariant> reportableVariants = createAllSomaticVariants();
        List<ReportableGainLoss> gainsAndLosses = createCOLO829GainsLosses();
        List<LinxFusion> fusions = createTestFusions();
        List<ReportableGeneDisruption> disruptions = createCOLO829Disruptions();
        List<ViralInsertion> viralInsertions = createTestViralInsertions();
        List<ReportableHomozygousDisruption> homozygousDisruptions = createTestHomozygousDisruptions();

        SampleReport sampleReport = createSkinMelanomaSampleReport(sampleId, true, limsCohortConfigData);
        String clinicalSummary = Strings.EMPTY;

        GenomicAnalysis analysis = ImmutableGenomicAnalysis.builder()
                .impliedPurity(impliedTumorPurity)
                .hasReliablePurity(hasReliablePurity)
                .hasReliableQuality(true)
                .averageTumorPloidy(averageTumorPloidy)
                .tumorSpecificEvidence(tumorSpecificEvidence)
                .clinicalTrials(clinicalTrials)
                .offLabelEvidence(offLabelEvidence)
                .reportableVariants(reportableVariants)
                .microsatelliteIndelsPerMb(microsatelliteIndelsPerMb)
                .microsatelliteStatus(MicrosatelliteStatus.fromIndelsPerMb(microsatelliteIndelsPerMb))
                .tumorMutationalLoad(tumorMutationalLoad)
                .tumorMutationalLoadStatus(TumorMutationalStatus.fromLoad(tumorMutationalLoad))
                .tumorMutationalBurden(tumorMutationalBurden)
                .chordHrdValue(chordHrdValue)
                .chordHrdStatus(chordStatus)
                .gainsAndLosses(gainsAndLosses)
                .geneFusions(fusions)
                .geneDisruptions(disruptions)
                .homozygousDisruptions(homozygousDisruptions)
                .viralInsertions(viralInsertions)
                .build();

        return ImmutableAnalysedPatientReport.builder()
                .sampleReport(sampleReport)
                .qsFormNumber(QsFormNumber.FOR_209.display())
                .clinicalSummary(clinicalSummary)
                .genomicAnalysis(analysis)
                .circosPath(CIRCOS_PATH)
                .comments(Optional.ofNullable(comments))
                .isCorrectedReport(false)
                .signaturePath(reportData.signaturePath())
                .logoRVAPath(reportData.logoRVAPath())
                .logoCompanyPath(reportData.logoCompanyPath())
                .build();
    }

    @NotNull
    public static QCFailReport buildQCFailReport(@NotNull String sampleId, @NotNull QCFailReason reason, @NotNull LimsCohortConfigData limsCohortConfigData) {
        SampleReport sampleReport = createSkinMelanomaSampleReport(sampleId, true, limsCohortConfigData);

        ReportData reportData = PatientReporterTestFactory.loadTestReportData();
        return ImmutableQCFailReport.builder()
                .sampleReport(sampleReport)
                .reason(reason)
                .comments(Optional.empty())
                .isCorrectedReport(false)
                .signaturePath(reportData.signaturePath())
                .logoRVAPath(reportData.logoRVAPath())
                .logoCompanyPath(reportData.logoCompanyPath())
                .build();
    }

    @NotNull
    private static HospitalContactData createTestHospitalContactData() {
        return ImmutableHospitalContactData.builder()
                .hospitalPI("PI")
                .requesterName("Paul")
                .requesterEmail("paul@hartwig.com")
                .hospitalName("HMF Testing Center")
                .hospitalAddress("1000 AB AMSTERDAM")
                .build();
    }

    @NotNull
    private static SampleReport createSkinMelanomaSampleReport(@NotNull String sample, boolean reportGermline,
            @NotNull LimsCohortConfigData cohort) {
        SampleMetadata sampleMetadata = ImmutableSampleMetadata.builder()
                .patientId("COLO829")
                .refSampleId(Strings.EMPTY)
                .refSampleBarcode("FR12123488")
                .tumorSampleId(sample)
                .tumorSampleBarcode("FR12345678")
                .build();

        return ImmutableSampleReport.builder()
                .sampleMetadata(sampleMetadata)
                .patientPrimaryTumor(ImmutablePatientPrimaryTumor.builder()
                        .patientIdentifier(sample)
                        .location("Skin")
                        .subLocation(Strings.EMPTY)
                        .type("Melanoma")
                        .subType(Strings.EMPTY)
                        .extraDetails(Strings.EMPTY)
                        .doids(Lists.newArrayList("8923"))
                        .isOverridden(false)
                        .build())
                .germlineReportingLevel(reportGermline
                        ? LimsGermlineReportingLevel.REPORT_WITH_NOTIFICATION
                        : LimsGermlineReportingLevel.NO_REPORTING)
                .reportViralInsertions(true)
                .refArrivalDate(LocalDate.parse("01-Oct-2020", DATE_FORMATTER))
                .tumorArrivalDate(LocalDate.parse("05-Oct-2020", DATE_FORMATTER))
                .shallowSeqPurityString(Lims.NOT_PERFORMED_STRING)
                .labProcedures("PREP013V23-QC037V20-SEQ008V25")
                .cohort(cohort)
                .projectName("TEST-001-002")
                .submissionId("SUBM")
                .hospitalContactData(createTestHospitalContactData())
                .hospitalPatientId("HOSP1")
                .hospitalPathologySampleId("PA1")
                .build();
    }

    @NotNull
    private static List<EvidenceItem> createCOLO829TumorSpecificEvidence() {
        List<EvidenceItem> evidenceItems = Lists.newArrayList();

        ImmutableEvidenceItem.Builder onLabelBuilder = evidenceBuilder().isOnLabel(true);

        evidenceItems.add(onLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Binimetinib + Encorafenib")
                .level(EvidenceLevel.LEVEL_A)
                .response("Responsive")
                .reference("V600E")
                .source(ActionabilitySource.ONCOKB)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(onLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Cobimetinib + Vemurafenib")
                .level(EvidenceLevel.LEVEL_A)
                .response("Responsive")
                .reference("V600E")
                .source(ActionabilitySource.ONCOKB)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(onLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Dabrafenib")
                .level(EvidenceLevel.LEVEL_A)
                .response("Responsive")
                .reference("V600E")
                .source(ActionabilitySource.ONCOKB)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(onLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Dabrafenib + Trametinib")
                .level(EvidenceLevel.LEVEL_A)
                .response("Responsive")
                .reference("V600E")
                .source(ActionabilitySource.ONCOKB)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(onLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Trametinib")
                .level(EvidenceLevel.LEVEL_A)
                .response("Responsive")
                .reference("V600E")
                .source(ActionabilitySource.ONCOKB)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(onLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Vemurafenib")
                .level(EvidenceLevel.LEVEL_A)
                .response("Responsive")
                .reference("V600E")
                .source(ActionabilitySource.ONCOKB)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(onLabelBuilder.event("BRAF p.Val600Glu")
                .drug("RO4987655")
                .level(EvidenceLevel.LEVEL_B)
                .response("Responsive")
                .reference("variant:208")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.GENE_LEVEL)
                .build());

        return evidenceItems;
    }

    @NotNull
    private static List<ClinicalTrial> createCOLO829ClinicalTrials() {
        List<ClinicalTrial> trials = Lists.newArrayList();
        ImmutableClinicalTrial.Builder iclusionBuilder =
                ImmutableClinicalTrial.builder().cancerType(Strings.EMPTY).isOnLabel(true).source(ActionabilitySource.ICLUSION);

        trials.add(iclusionBuilder.event("BRAF p.Val600Glu")
                .scope(EvidenceScope.GENE_LEVEL)
                .acronym("CLXH254X2101")
                .reference("EXT10453 (NL55506.078.15)")
                .build());
        trials.add(iclusionBuilder.event("BRAF p.Val600Glu")
                .scope(EvidenceScope.SPECIFIC)
                .acronym("COWBOY")
                .reference("EXT12301 (NL71732.091.19)")
                .build());
        trials.add(iclusionBuilder.event("BRAF p.Val600Glu")
                .scope(EvidenceScope.GENE_LEVEL)
                .acronym("DRUP")
                .reference("EXT10299 (NL54757.031.16)")
                .build());
        trials.add(iclusionBuilder.event("BRAF p.Val600Glu")
                .scope(EvidenceScope.GENE_LEVEL)
                .acronym("EBIN (EORTC-1612-MG)")
                .reference("EXT11284 (NL67202.031.18)")
                .build());
        trials.add(iclusionBuilder.event("BRAF p.Val600Glu")
                .scope(EvidenceScope.GENE_LEVEL)
                .acronym("POLARIS")
                .reference("EXT11388 (NL69569.028.19)")
                .build());
        trials.add(iclusionBuilder.event("CDKN2A p.Ala68fs")
                .scope(EvidenceScope.GENE_LEVEL)
                .acronym("DRUP")
                .reference("EXT10299 (NL54757.031.16)")
                .build());

        return trials;
    }

    @NotNull
    private static List<EvidenceItem> createCOLO829OffLabelEvidence() {
        List<EvidenceItem> evidenceItems = Lists.newArrayList();

        ImmutableEvidenceItem.Builder offLabelBuilder = evidenceBuilder().isOnLabel(false);

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Alpelisib + Cetuximab + Encorafenib")
                .level(EvidenceLevel.LEVEL_B)
                .response("Responsive")
                .reference("variant:17")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.GENE_LEVEL)
                .build());

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Bevacizumab")
                .level(EvidenceLevel.LEVEL_B)
                .response("Resistant")
                .reference("variant:12")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("CI-1040")
                .level(EvidenceLevel.LEVEL_B)
                .response("Responsive")
                .reference("variant:12")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Cetuximab")
                .level(EvidenceLevel.LEVEL_B)
                .response("Resistant")
                .reference("BRAF:V600E")
                .source(ActionabilitySource.CGI)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Cetuximab + Encorafenib")
                .level(EvidenceLevel.LEVEL_B)
                .response("Responsive")
                .reference("variant:17")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.GENE_LEVEL)
                .build());

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Cetuximab + Irinotecan + Vemurafenib")
                .level(EvidenceLevel.LEVEL_B)
                .response("Responsive")
                .reference("variant:12")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Dabrafenib + Panitumumab + Trametinib")
                .level(EvidenceLevel.LEVEL_B)
                .response("Responsive")
                .reference("variant:12")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Irinotecan")
                .level(EvidenceLevel.LEVEL_B)
                .response("Resistant")
                .reference("variant:12")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Oxaliplatin")
                .level(EvidenceLevel.LEVEL_B)
                .response("Resistant")
                .reference("variant:12")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Panitumumab")
                .level(EvidenceLevel.LEVEL_B)
                .response("Resistant")
                .reference("BRAF:V600E")
                .source(ActionabilitySource.CGI)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Vemurafenib")
                .level(EvidenceLevel.LEVEL_B)
                .response("Resistant")
                .reference("variant:17")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.GENE_LEVEL)
                .build());

        evidenceItems.add(offLabelBuilder.event("PTEN Deletion")
                .drug("EGFR mAB inhibitor")
                .level(EvidenceLevel.LEVEL_B)
                .response("Resistant")
                .reference("PTEN:del")
                .source(ActionabilitySource.CGI)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(offLabelBuilder.event("PTEN Deletion")
                .drug("Everolimus")
                .level(EvidenceLevel.LEVEL_B)
                .response("Responsive")
                .reference("variant:213")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        return evidenceItems;
    }

    @NotNull
    private static List<ReportableVariant> createCOLO829SomaticVariants(boolean forceCDKN2AVariantToBeGermline) {
        ReportableVariant variant1 = ImmutableReportableVariant.builder()
                .source(ReportableVariantSource.SOMATIC)
                .gene("BRAF")
                .chromosome("7")
                .position(140453136)
                .ref("T")
                .alt("A")
                .canonicalCodingEffect(CodingEffect.MISSENSE)
                .canonicalHgvsCodingImpact("c.1799T>A")
                .canonicalHgvsProteinImpact("p.Val600Glu")
                .alleleReadCount(150)
                .totalReadCount(221)
                .alleleCopyNumber(4.08)
                .totalCopyNumber(6.0)
                .hotspot(Hotspot.HOTSPOT)
                .driverLikelihood(1D)
                .clonalLikelihood(1D)
                .biallelic(false)
                .build();

        ReportableVariant variant2 = ImmutableReportableVariant.builder()
                .source(forceCDKN2AVariantToBeGermline ? ReportableVariantSource.GERMLINE : ReportableVariantSource.SOMATIC)
                .gene("CDKN2A")
                .chromosome("9")
                .position(21971153)
                .ref("CCG")
                .alt("C")
                .canonicalCodingEffect(CodingEffect.NONSENSE_OR_FRAMESHIFT)
                .canonicalHgvsCodingImpact("c.203_204delCG")
                .canonicalHgvsProteinImpact("p.Ala68fs")
                .alleleReadCount(99)
                .totalReadCount(99)
                .alleleCopyNumber(1.99)
                .totalCopyNumber(1.99)
                .hotspot(Hotspot.NEAR_HOTSPOT)
                .clonalLikelihood(1D)
                .driverLikelihood(0.9)
                .biallelic(true)
                .build();

        ReportableVariant variant3 = ImmutableReportableVariant.builder()
                .source(ReportableVariantSource.SOMATIC)
                .gene("TERT")
                .chromosome("5")
                .position(1295228)
                .ref("GG")
                .alt("AA")
                .canonicalCodingEffect(CodingEffect.NONE)
                .canonicalHgvsCodingImpact("c.-125_-124delCCinsTT")
                .canonicalHgvsProteinImpact(Strings.EMPTY)
                .alleleReadCount(56)
                .totalReadCount(65)
                .alleleCopyNumber(1.74)
                .totalCopyNumber(2.0)
                .hotspot(Hotspot.HOTSPOT)
                .clonalLikelihood(1D)
                .driverLikelihood(0.85)
                .biallelic(true)
                .build();

        ReportableVariant variant4 = ImmutableReportableVariant.builder()
                .source(ReportableVariantSource.SOMATIC)
                .gene("SF3B1")
                .chromosome("2")
                .position(198266779)
                .ref("C")
                .alt("T")
                .canonicalCodingEffect(CodingEffect.MISSENSE)
                .canonicalHgvsCodingImpact("c.2153C>T")
                .canonicalHgvsProteinImpact("p.Pro718Leu")
                .alleleReadCount(74)
                .totalReadCount(111)
                .alleleCopyNumber(2.01)
                .totalCopyNumber(3.02)
                .hotspot(Hotspot.NON_HOTSPOT)
                .clonalLikelihood(1D)
                .driverLikelihood(0.15)
                .biallelic(false)
                .build();

        ReportableVariant variant5 = ImmutableReportableVariant.builder()
                .source(ReportableVariantSource.SOMATIC)
                .gene("TP63")
                .chromosome("3")
                .position(189604330)
                .ref("G")
                .alt("T")
                .canonicalCodingEffect(CodingEffect.MISSENSE)
                .canonicalHgvsCodingImpact("c.1497G>T")
                .canonicalHgvsProteinImpact("p.Met499Ile")
                .alleleReadCount(47)
                .totalReadCount(112)
                .alleleCopyNumber(1.67)
                .totalCopyNumber(3.98)
                .hotspot(Hotspot.NON_HOTSPOT)
                .clonalLikelihood(1D)
                .driverLikelihood(0.1)
                .biallelic(false)
                .build();

        return Lists.newArrayList(variant1, variant2, variant3, variant4, variant5);
    }

    @NotNull
    private static List<ReportableVariant> createAllSomaticVariants() {
        ReportableVariant variant1 = ImmutableReportableVariant.builder()
                .source(ReportableVariantSource.SOMATIC)
                .gene("TP63")
                .chromosome("3")
                .position(189604330)
                .ref("G")
                .alt("T")
                .canonicalCodingEffect(CodingEffect.MISSENSE)
                .canonicalHgvsCodingImpact("c.1497G>T")
                .canonicalHgvsProteinImpact("p.Met499Ile")
                .alleleReadCount(48)
                .totalReadCount(103)
                .alleleCopyNumber(2.1)
                .totalCopyNumber(4.1)
                .hotspot(Hotspot.NON_HOTSPOT)
                .clonalLikelihood(0.47)
                .driverLikelihood(0.1)
                .biallelic(false)
                .build();

        ReportableVariant variant2 = ImmutableReportableVariant.builder()
                .source(ReportableVariantSource.SOMATIC)
                .gene("KIT")
                .chromosome("3")
                .position(81627197)
                .ref("G")
                .alt("T")
                .canonicalCodingEffect(CodingEffect.MISSENSE)
                .canonicalHgvsCodingImpact("c.1497G>T")
                .canonicalHgvsProteinImpact("p.Met499Ile")
                .alleleReadCount(48)
                .totalReadCount(103)
                .alleleCopyNumber(1.3)
                .totalCopyNumber(2.5)
                .hotspot(Hotspot.NON_HOTSPOT)
                .clonalLikelihood(0.68)
                .driverLikelihood(0.1)
                .biallelic(true)
                .build();

        return Lists.newArrayList(variant1, variant2);
    }

    @NotNull
    private static List<ReportableGainLoss> createCOLO829GainsLosses() {
        ReportableGainLoss gainLoss1 = ImmutableReportableGainLoss.builder()
                .chromosome("10")
                .chromosomeBand("q23.31")
                .gene("PTEN")
                .copies(0)
                .interpretation(CopyNumberInterpretation.PARTIAL_LOSS)
                .build();

        return Lists.newArrayList(gainLoss1);
    }

    @NotNull
    private static List<LinxFusion> createTestFusions() {
        LinxFusion fusion1 = ImmutableLinxFusion.builder()
                .fivePrimeBreakendId(1)
                .threePrimeBreakendId(2)
                .name(Strings.EMPTY)
                .reported(true)
                .reportedType(Strings.EMPTY)
                .phased(FusionPhasedType.INFRAME)
                .likelihood(FusionLikelihoodType.HIGH)
                .chainLength(1)
                .chainLinks(1)
                .chainTerminated(true)
                .domainsKept(Strings.EMPTY)
                .domainsLost(Strings.EMPTY)
                .skippedExonsUp(2)
                .skippedExonsDown(4)
                .fusedExonUp(6)
                .fusedExonDown(7)
                .geneStart("TMPRSS2")
                .geneContextStart("Intron 5")
                .geneTranscriptStart("ENST00000398585")
                .geneEnd("PNPLA7")
                .geneContextEnd("Intron 3")
                .geneTranscriptEnd("ENST00000406427")
                .junctionCopyNumber(0.4)
                .build();

        LinxFusion fusion2 = ImmutableLinxFusion.builder()
                .fivePrimeBreakendId(1)
                .threePrimeBreakendId(2)
                .name(Strings.EMPTY)
                .reported(true)
                .reportedType(Strings.EMPTY)
                .phased(FusionPhasedType.SKIPPED_EXONS)
                .likelihood(FusionLikelihoodType.LOW)
                .chainLength(1)
                .chainLinks(1)
                .chainTerminated(true)
                .domainsKept(Strings.EMPTY)
                .domainsLost(Strings.EMPTY)
                .skippedExonsUp(2)
                .skippedExonsDown(4)
                .fusedExonUp(6)
                .fusedExonDown(7)
                .geneStart("CLCN6")
                .geneContextStart("Intron 1")
                .geneTranscriptStart("ENST00000346436")
                .geneEnd("BRAF")
                .geneContextEnd("Intron 8")
                .geneTranscriptEnd("ENST00000288602")
                .junctionCopyNumber(1D)
                .build();

        return Lists.newArrayList(fusion1, fusion2);
    }

    @NotNull
    private static List<ReportableGeneDisruption> createCOLO829Disruptions() {
        ReportableGeneDisruption disruption1 = createDisruptionBuilder().location("10q23.31")
                .gene("PTEN")
                .range("Intron 5 -> Intron 6")
                .type("DEL")
                .junctionCopyNumber(2D)
                .undisruptedCopyNumber(0)
                .build();

        return Lists.newArrayList(disruption1);
    }

    @NotNull
    private static List<ReportableHomozygousDisruption> createTestHomozygousDisruptions() {
        List<ReportableHomozygousDisruption> homozygousDisruptions = Lists.newArrayList(ImmutableReportableHomozygousDisruption.builder()
                .chromosome("8")
                .chromosomeBand("p22")
                .gene("SGCZ")
                .build());
        return Lists.newArrayList(homozygousDisruptions);
    }

    @NotNull
    private static List<ViralInsertion> createTestViralInsertions() {
        List<ViralInsertion> viralInsertions =
                Lists.newArrayList(ImmutableViralInsertion.builder().virus("Human papillomavirus type 16").viralInsertionCount(2).build());
        return Lists.newArrayList(viralInsertions);
    }

    @NotNull
    private static ImmutableEvidenceItem.Builder evidenceBuilder() {
        return ImmutableEvidenceItem.builder().drugsType(Strings.EMPTY).cancerType(Strings.EMPTY);
    }

    @NotNull
    private static ImmutableReportableGeneDisruption.Builder createDisruptionBuilder() {
        return ImmutableReportableGeneDisruption.builder().firstAffectedExon(1);
    }
}

