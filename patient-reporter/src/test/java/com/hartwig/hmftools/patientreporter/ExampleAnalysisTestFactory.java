package com.hartwig.hmftools.patientreporter;

import static com.hartwig.hmftools.patientreporter.PatientReporterTestUtil.testReportData;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.hartwig.hmftools.common.actionability.ActionabilitySource;
import com.hartwig.hmftools.common.actionability.ClinicalTrial;
import com.hartwig.hmftools.common.actionability.EvidenceItem;
import com.hartwig.hmftools.common.actionability.EvidenceLevel;
import com.hartwig.hmftools.common.actionability.EvidenceScope;
import com.hartwig.hmftools.common.actionability.ImmutableClinicalTrial;
import com.hartwig.hmftools.common.actionability.ImmutableEvidenceItem;
import com.hartwig.hmftools.common.chord.ChordAnalysis;
import com.hartwig.hmftools.common.chord.ImmutableChordAnalysis;
import com.hartwig.hmftools.common.drivercatalog.DriverCategory;
import com.hartwig.hmftools.common.ecrf.projections.ImmutablePatientTumorLocation;
import com.hartwig.hmftools.common.variant.Hotspot;
import com.hartwig.hmftools.common.variant.structural.annotation.ImmutableReportableGeneFusion;
import com.hartwig.hmftools.common.variant.structural.annotation.ReportableGeneFusion;
import com.hartwig.hmftools.patientreporter.copynumber.CopyNumberInterpretation;
import com.hartwig.hmftools.patientreporter.copynumber.ImmutableReportableGainLoss;
import com.hartwig.hmftools.patientreporter.copynumber.ReportableGainLoss;
import com.hartwig.hmftools.patientreporter.structural.ImmutableReportableGeneDisruption;
import com.hartwig.hmftools.patientreporter.structural.ReportableGeneDisruption;
import com.hartwig.hmftools.patientreporter.variants.ImmutableReportableVariant;
import com.hartwig.hmftools.patientreporter.variants.ReportableVariant;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

public final class ExampleAnalysisTestFactory {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
    private static final String CIRCOS_PATH = Resources.getResource("test_run/purple/plot/sample.circos.png").getPath();

    private ExampleAnalysisTestFactory() {
    }

    @NotNull
    public static AnalysedPatientReport buildCOLO829() {
        final boolean hasReliablePurityFit = true;
        final double impliedTumorPurity = 1D;
        final double averageTumorPloidy = 3.1;
        final int tumorMutationalLoad = 180;
        final double tumorMutationalBurden = 13.6;
        final double microsatelliteIndelsPerMb = 0.12;

        final ReportData reportData = testReportData();

        final List<EvidenceItem> tumorLocationSpecificEvidence = createCOLO829TumorSpecificEvidence();
        final List<ClinicalTrial> clinicalTrials = createCOLO829ClinicalTrials();
        final List<EvidenceItem> offLabelEvidence = createCOLO829OffLabelEvidence();
        final List<ReportableVariant> reportableVariants = createCOLO829SomaticVariants();
        final List<ReportableGainLoss> gainsAndLosses = createCOLO829GainsLosses();
        final List<ReportableGeneFusion> fusions = Lists.newArrayList();
        final List<ReportableGeneDisruption> disruptions = createCOLO829Disruptions();
        final ChordAnalysis chordAnalysis = createCOLO829ChordAnalysis();

        final SampleReport sampleReport = createCOLO829SampleReport();

        final String clinicalSummary = "Melanoma sample with an activating BRAF mutation that is associated with "
                + "response to BRAF-inhibitors (in combination with a MEK-inhibitor). The tumor shows a complete "
                + "inactivation of CDKN2A, indicating potential benefit of CDK4/6 inhibitors (e.g. palbociclib). The "
                + "observed complete loss of PTEN likely results in an activation of the PI3K-AKT-mTOR pathway and "
                + "suggests eligibility for treatment (study) using mTOR/PI3K inhibitors. In addition, the tumor sample "
                + "shows a high mutational burden that is associated with an increased response rate to checkpoint "
                + "inhibitor immunotherapy.";

        return ImmutableAnalysedPatientReport.of(sampleReport,
                hasReliablePurityFit,
                impliedTumorPurity,
                averageTumorPloidy,
                clinicalSummary,
                tumorLocationSpecificEvidence,
                clinicalTrials,
                offLabelEvidence,
                reportableVariants,
                microsatelliteIndelsPerMb,
                tumorMutationalLoad,
                tumorMutationalBurden,
                chordAnalysis,
                gainsAndLosses,
                fusions,
                disruptions,
                CIRCOS_PATH,
                Optional.of("this is a test report and is based off COLO829"),
                reportData.signaturePath(),
                reportData.logoRVAPath(),
                reportData.logoCompanyPath());
    }

    @NotNull
    public static AnalysedPatientReport buildAnalysisWithAllTablesFilledIn() {
        final boolean hasReliablePurityFit = true;
        final double impliedTumorPurity = 1D;
        final double averageTumorPloidy = 3.1;
        final int tumorMutationalLoad = 182;
        final double tumorMutationalBurden = 13.6;
        final double microsatelliteIndelsPerMb = 0.1089;

        final ReportData reportData = testReportData();

        final List<EvidenceItem> tumorLocationSpecificEvidence = createCOLO829TumorSpecificEvidence();
        final List<ClinicalTrial> clinicalTrials = createCOLO829ClinicalTrials();
        final List<EvidenceItem> offLabelEvidence = createCOLO829OffLabelEvidence();
        final List<ReportableVariant> reportableVariants = createAllSomaticVariants();
        final List<ReportableGainLoss> gainsAndLosses = createCOLO829GainsLosses();
        final List<ReportableGeneFusion> fusions = createTestFusions();
        final ChordAnalysis chordAnalysis = createCOLO829ChordAnalysis();
        final List<ReportableGeneDisruption> disruptions = createCOLO829Disruptions();

        final SampleReport sampleReport = createCOLO829SampleReport();
        final String clinicalSummary = Strings.EMPTY;

        return ImmutableAnalysedPatientReport.of(sampleReport,
                hasReliablePurityFit,
                impliedTumorPurity,
                averageTumorPloidy,
                clinicalSummary,
                tumorLocationSpecificEvidence,
                clinicalTrials,
                offLabelEvidence,
                reportableVariants,
                microsatelliteIndelsPerMb,
                tumorMutationalLoad,
                tumorMutationalBurden,
                chordAnalysis,
                gainsAndLosses,
                fusions,
                disruptions,
                CIRCOS_PATH,
                Optional.of("this is a test report and does not relate to any real patient"),
                reportData.signaturePath(),
                reportData.logoRVAPath(),
                reportData.logoCompanyPath());
    }

    @NotNull
    public static AnalysedPatientReport buildAnalysisWithAllTablesForBelowDetectionLimitSample() {
        final boolean hasReliablePurityFit = false;
        final double impliedTumorPurity = 1D;
        final double averageTumorPloidy = 3.1;
        final int tumorMutationalLoad = 182;
        final double tumorMutationalBurden = 13.6;
        final double microsatelliteIndelsPerMb = 0.1089;

        final ReportData reportData = testReportData();

        final List<EvidenceItem> tumorLocationSpecificEvidence = createCOLO829TumorSpecificEvidence();
        final List<ClinicalTrial> clinicalTrials = createCOLO829ClinicalTrials();
        final List<EvidenceItem> offLabelEvidence = createCOLO829OffLabelEvidence();
        final List<ReportableVariant> reportableVariants = createAllSomaticVariants();
        final List<ReportableGainLoss> gainsAndLosses = createCOLO829GainsLosses();
        final List<ReportableGeneFusion> fusions = createTestFusions();
        final ChordAnalysis chordAnalysis = createCOLO829ChordAnalysis();
        final List<ReportableGeneDisruption> disruptions = createCOLO829Disruptions();

        final SampleReport sampleReport = createCOLO829SampleReport();
        final String clinicalSummary = Strings.EMPTY;

        return ImmutableAnalysedPatientReport.of(sampleReport,
                hasReliablePurityFit,
                impliedTumorPurity,
                averageTumorPloidy,
                clinicalSummary,
                tumorLocationSpecificEvidence,
                clinicalTrials,
                offLabelEvidence,
                reportableVariants,
                microsatelliteIndelsPerMb,
                tumorMutationalLoad,
                tumorMutationalBurden,
                chordAnalysis,
                gainsAndLosses,
                fusions,
                disruptions,
                CIRCOS_PATH,
                Optional.of("this is a test report and does not relate to any real patient"),
                reportData.signaturePath(),
                reportData.logoRVAPath(),
                reportData.logoCompanyPath());
    }

    @NotNull
    private static SampleReport createCOLO829SampleReport() {
        final String sample = "PNT00012345T";
        return ImmutableSampleReport.builder()
                .sampleId(sample)
                .patientTumorLocation(ImmutablePatientTumorLocation.of("COLO829", "Skin", "Melanoma"))
                .refBarcode("FR12123488")
                .refArrivalDate(LocalDate.parse("01-Jan-2019", DATE_FORMATTER))
                .tumorBarcode("FR12345678")
                .tumorArrivalDate(LocalDate.parse("05-Jan-2019", DATE_FORMATTER))
                .purityShallowSeq(Strings.EMPTY)
                .pathologyTumorPercentage("80%")
                .labProcedures("PREP013V23-QC037V20-SEQ008V25")
                .requesterName(Strings.EMPTY)
                .requesterEmail(Strings.EMPTY)
                .addressee("HMF Testing Center")
                .hospitalName(Strings.EMPTY)
                .hospitalPIName(Strings.EMPTY)
                .hospitalPIEmail(Strings.EMPTY)
                .projectName("COLO")
                .submissionId(Strings.EMPTY)
                .hospitalPatientId(Strings.EMPTY)
                .hospitalPathologySampleId(Strings.EMPTY)
                .build();
    }

    @NotNull
    private static List<EvidenceItem> createCOLO829TumorSpecificEvidence() {
        List<EvidenceItem> evidenceItems = Lists.newArrayList();

        ImmutableEvidenceItem.Builder onLabelBuilder = evidenceBuilder().isOnLabel(true);

        evidenceItems.add(onLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Binimetinib + Encorafenib")
                .drugsType("Immuno")
                .level(EvidenceLevel.LEVEL_A)
                .response("Responsive")
                .reference("V600E")
                .source(ActionabilitySource.ONCOKB)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(onLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Cobimetinib + Vemurafenib")
                .drugsType("Immuno")
                .level(EvidenceLevel.LEVEL_A)
                .response("Responsive")
                .reference("V600E")
                .source(ActionabilitySource.ONCOKB)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(onLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Dabrafenib")
                .drugsType("Immuno")
                .level(EvidenceLevel.LEVEL_A)
                .response("Responsive")
                .reference("V600E")
                .source(ActionabilitySource.ONCOKB)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(onLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Dabrafenib + Trametinib")
                .drugsType("Immuno")
                .level(EvidenceLevel.LEVEL_A)
                .response("Responsive")
                .reference("V600E")
                .source(ActionabilitySource.ONCOKB)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(onLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Trametinib")
                .drugsType("Immuno")
                .level(EvidenceLevel.LEVEL_A)
                .response("Responsive")
                .reference("V600E")
                .source(ActionabilitySource.ONCOKB)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(onLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Vemurafenib")
                .drugsType("Immuno")
                .level(EvidenceLevel.LEVEL_A)
                .response("Responsive")
                .reference("V600E")
                .source(ActionabilitySource.ONCOKB)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(onLabelBuilder.event("BRAF p.Val600Glu")
                .drug("RO4987655")
                .drugsType("Immuno")
                .level(EvidenceLevel.LEVEL_B)
                .response("Responsive")
                .reference("variant:208")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.BROAD)
                .build());

        return evidenceItems;
    }

    @NotNull
    private static List<ClinicalTrial> createCOLO829ClinicalTrials() {
        List<ClinicalTrial> trials = Lists.newArrayList();
        ImmutableClinicalTrial.Builder iclusionBuilder =
                ImmutableClinicalTrial.builder().cancerType(Strings.EMPTY).isOnLabel(true).source(ActionabilitySource.ICLUSION);

        trials.add(iclusionBuilder.event("BRAF p.Val600Glu")
                .scope(EvidenceScope.BROAD)
                .acronym("LXH254 in tumors with MAPK pathway alterations")
                .reference("EXT10453 (NL55506.078.15)")
                .build());
        trials.add(iclusionBuilder.event("BRAF p.Val600Glu")
                .scope(EvidenceScope.BROAD)
                .acronym("Novartis CTMT212X2102")
                .reference("EXT3437 (NL56240.056.16)")
                .build());
        trials.add(iclusionBuilder.event("BRAF p.Val600Glu")
                .scope(EvidenceScope.SPECIFIC)
                .acronym("PROCLAIM-001")
                .reference("EXT10241 (NL59299.042.17)")
                .build());
        trials.add(iclusionBuilder.event("BRAF p.Val600Glu")
                .scope(EvidenceScope.SPECIFIC)
                .acronym("REDUCTOR")
                .reference("EXT6690 (NL45261.031.13)")
                .build());

        return trials;
    }

    @NotNull
    private static List<EvidenceItem> createCOLO829OffLabelEvidence() {
        List<EvidenceItem> evidenceItems = Lists.newArrayList();

        ImmutableEvidenceItem.Builder offLabelBuilder = evidenceBuilder().isOnLabel(false);

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Alpelisib + Cetuximab + Encorafenib")
                .drugsType("Immuno")
                .level(EvidenceLevel.LEVEL_B)
                .response("Responsive")
                .reference("variant:17")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.BROAD)
                .build());

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Bevacizumab")
                .drugsType("Immuno")
                .level(EvidenceLevel.LEVEL_B)
                .response("Resistant")
                .reference("variant:12")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("CI-1040")
                .drugsType("Immuno")
                .level(EvidenceLevel.LEVEL_B)
                .response("Responsive")
                .reference("variant:12")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Cetuximab")
                .drugsType("Immuno")
                .level(EvidenceLevel.LEVEL_B)
                .response("Resistant")
                .reference("BRAF:V600E")
                .source(ActionabilitySource.CGI)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Cetuximab + Encorafenib")
                .drugsType("Immuno")
                .level(EvidenceLevel.LEVEL_B)
                .response("Responsive")
                .reference("variant:17")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.BROAD)
                .build());

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Cetuximab + Irinotecan + Vemurafenib")
                .drugsType("Immuno")
                .level(EvidenceLevel.LEVEL_B)
                .response("Responsive")
                .reference("variant:12")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Dabrafenib + Panitumumab + Trametinib")
                .drugsType("Immuno")
                .level(EvidenceLevel.LEVEL_B)
                .response("Responsive")
                .reference("variant:12")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Irinotecan")
                .drugsType("Immuno")
                .level(EvidenceLevel.LEVEL_B)
                .response("Resistant")
                .reference("variant:12")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Oxaliplatin")
                .drugsType("Immuno")
                .level(EvidenceLevel.LEVEL_B)
                .response("Resistant")
                .reference("variant:12")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Panitumumab")
                .drugsType("Immuno")
                .level(EvidenceLevel.LEVEL_B)
                .response("Resistant")
                .reference("BRAF:V600E")
                .source(ActionabilitySource.CGI)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(offLabelBuilder.event("BRAF p.Val600Glu")
                .drug("Vemurafenib")
                .drugsType("Immuno")
                .level(EvidenceLevel.LEVEL_B)
                .response("Resistant")
                .reference("variant:17")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.BROAD)
                .build());

        evidenceItems.add(offLabelBuilder.event("PTEN Deletion")
                .drug("EGFR mAB inhibitor")
                .drugsType("Immuno")
                .level(EvidenceLevel.LEVEL_B)
                .response("Resistant")
                .reference("PTEN:del")
                .source(ActionabilitySource.CGI)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(offLabelBuilder.event("PTEN Deletion")
                .drug("Everolimus")
                .drugsType("Immuno")
                .level(EvidenceLevel.LEVEL_B)
                .response("Responsive")
                .reference("variant:213")
                .source(ActionabilitySource.CIVIC)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        return evidenceItems;
    }

    @NotNull
    private static List<ReportableVariant> createCOLO829SomaticVariants() {
        ReportableVariant variant1 = ImmutableReportableVariant.builder()
                .gene("BRAF")
                .isDrupActionable(true)
                .notifyClinicalGeneticist(false)
                .driverCategory(DriverCategory.ONCO)
                .gDNA("7:140453136")
                .hgvsCodingImpact("c.1799T>A")
                .hgvsProteinImpact("p.Val600Glu")
                .alleleReadCount(154)
                .totalReadCount(225)
                .allelePloidy(4.1)
                .totalPloidy(6.1)
                .hotspot(Hotspot.HOTSPOT)
                .biallelic(false)
                .driverLikelihood(1D)
                .clonalLikelihood(1D)
                .build();

        ReportableVariant variant2 = ImmutableReportableVariant.builder()
                .gene("CDKN2A")
                .isDrupActionable(true)
                .notifyClinicalGeneticist(false)
                .driverCategory(DriverCategory.TSG)
                .gDNA("9:21971153")
                .hgvsCodingImpact("c.203_204delCG")
                .hgvsProteinImpact("p.Ala68fs")
                .alleleReadCount(94)
                .totalReadCount(94)
                .allelePloidy(2D)
                .totalPloidy(2D)
                .hotspot(Hotspot.NON_HOTSPOT)
                .biallelic(true)
                .clonalLikelihood(1D)
                .driverLikelihood(0.9)
                .build();

        ReportableVariant variant3 = ImmutableReportableVariant.builder()
                .gene("TERT")
                .isDrupActionable(false)
                .notifyClinicalGeneticist(false)
                .driverCategory(DriverCategory.ONCO)
                .gDNA("5:1295228")
                .hgvsCodingImpact("c.-125_-124delCCinsTT")
                .hgvsProteinImpact(Strings.EMPTY)
                .alleleReadCount(49)
                .totalReadCount(49)
                .allelePloidy(2D)
                .totalPloidy(2D)
                .hotspot(Hotspot.HOTSPOT)
                .biallelic(false)
                .clonalLikelihood(1D)
                .driverLikelihood(0.85)
                .build();

        ReportableVariant variant4 = ImmutableReportableVariant.builder()
                .gene("SF3B1")
                .isDrupActionable(false)
                .notifyClinicalGeneticist(false)
                .driverCategory(DriverCategory.ONCO)
                .gDNA("2:198266779")
                .hgvsCodingImpact("c.2153C>T")
                .hgvsProteinImpact("p.Pro718Leu")
                .alleleReadCount(76)
                .totalReadCount(115)
                .allelePloidy(2D)
                .totalPloidy(3.1)
                .hotspot(Hotspot.NON_HOTSPOT)
                .biallelic(false)
                .clonalLikelihood(1D)
                .driverLikelihood(0.5)
                .build();

        ReportableVariant variant5 = ImmutableReportableVariant.builder()
                .gene("TP63")
                .isDrupActionable(false)
                .notifyClinicalGeneticist(false)
                .driverCategory(DriverCategory.TSG)
                .gDNA("3:189604330")
                .hgvsCodingImpact("c.1497G>T")
                .hgvsProteinImpact("p.Met499Ile")
                .alleleReadCount(52)
                .totalReadCount(119)
                .allelePloidy(1.8)
                .totalPloidy(4D)
                .hotspot(Hotspot.NON_HOTSPOT)
                .biallelic(false)
                .clonalLikelihood(1D)
                .driverLikelihood(0.1)
                .build();

        return Lists.newArrayList(variant1, variant2, variant3, variant4, variant5);
    }

    @NotNull
    private static List<ReportableVariant> createAllSomaticVariants() {
        ReportableVariant variant1 = ImmutableReportableVariant.builder()
                .gene("TP63")
                .isDrupActionable(false)
                .notifyClinicalGeneticist(false)
                .driverCategory(DriverCategory.TSG)
                .gDNA("3:189604330")
                .hgvsCodingImpact("c.1497G>T")
                .hgvsProteinImpact("p.Met499Ile")
                .alleleReadCount(48)
                .totalReadCount(103)
                .allelePloidy(2.1)
                .totalPloidy(4.1)
                .biallelic(false)
                .hotspot(Hotspot.NON_HOTSPOT)
                .clonalLikelihood(0.47)
                .driverLikelihood(0.1)
                .build();

        ReportableVariant variant2 = ImmutableReportableVariant.builder()
                .gene("KIT")
                .isDrupActionable(false)
                .notifyClinicalGeneticist(true)
                .driverCategory(DriverCategory.TSG)
                .gDNA("3:81627197")
                .hgvsCodingImpact("c.1497G>T")
                .hgvsProteinImpact("p.Met499Ile")
                .alleleReadCount(48)
                .totalReadCount(103)
                .allelePloidy(1.3)
                .totalPloidy(2.5)
                .hotspot(Hotspot.NON_HOTSPOT)
                .biallelic(true)
                .clonalLikelihood(0.68)
                .driverLikelihood(0.1)
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
    private static List<ReportableGeneFusion> createTestFusions() {
        ReportableGeneFusion fusion1 = ImmutableReportableGeneFusion.builder()
                .geneStart("TMPRSS2")
                .geneTranscriptStart("ENST00000398585")
                .geneContextStart("Intron 5")
                .geneEnd("PNPLA7")
                .geneTranscriptEnd("ENST00000406427")
                .geneContextEnd("Intron 3")
                .ploidy(0.4)
                .build();

        ReportableGeneFusion fusion2 = ImmutableReportableGeneFusion.builder()
                .geneStart("CLCN6")
                .geneTranscriptStart("ENST00000346436")
                .geneContextStart("Intron 1")
                .geneEnd("BRAF")
                .geneTranscriptEnd("ENST00000288602")
                .geneContextEnd("Intron 8")
                .ploidy(1D)
                .build();

        return Lists.newArrayList(fusion1, fusion2);
    }

    @NotNull
    private static ChordAnalysis createCOLO829ChordAnalysis() {
        double brca1Value = 0D;
        double brca2Value = 0D;

        return ImmutableChordAnalysis.builder()
                .noneValue(1 - (brca1Value + brca2Value))
                .BRCA1Value(brca1Value)
                .BRCA2Value(brca2Value)
                .hrdValue(brca1Value + brca2Value)
                .predictedResponseValue(brca1Value + brca2Value > 0.5 ? 1 : 0)
                .build();
    }

    @NotNull
    private static List<ReportableGeneDisruption> createCOLO829Disruptions() {
        ReportableGeneDisruption disruption1 = createDisruptionBuilder().location("10q23.31")
                .gene("PTEN")
                .range("Intron 5 -> Intron 6")
                .type("DEL")
                .ploidy(2D)
                .geneMinCopies(0)
                .geneMaxCopies(2)
                .build();

        return Lists.newArrayList(disruption1);
    }

    @NotNull
    private static ImmutableEvidenceItem.Builder evidenceBuilder() {
        return ImmutableEvidenceItem.builder().cancerType(Strings.EMPTY);
    }

    @NotNull
    private static ImmutableReportableGeneDisruption.Builder createDisruptionBuilder() {
        return ImmutableReportableGeneDisruption.builder().firstAffectedExon(1);
    }
}

