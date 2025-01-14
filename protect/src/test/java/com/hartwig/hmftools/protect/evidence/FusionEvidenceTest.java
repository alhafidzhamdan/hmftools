package com.hartwig.hmftools.protect.evidence;

import static com.hartwig.hmftools.common.fusion.KnownFusionType.EXON_DEL_DUP;
import static com.hartwig.hmftools.common.fusion.KnownFusionType.IG_KNOWN_PAIR;
import static com.hartwig.hmftools.common.fusion.KnownFusionType.IG_PROMISCUOUS;
import static com.hartwig.hmftools.common.fusion.KnownFusionType.KNOWN_PAIR;
import static com.hartwig.hmftools.common.fusion.KnownFusionType.NONE;
import static com.hartwig.hmftools.common.fusion.KnownFusionType.PROMISCUOUS_3;
import static com.hartwig.hmftools.common.fusion.KnownFusionType.PROMISCUOUS_5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.linx.LinxTestFactory;
import com.hartwig.hmftools.common.protect.ProtectEvidence;
import com.hartwig.hmftools.common.protect.EvidenceType;
import com.hartwig.hmftools.common.protect.KnowledgebaseSource;
import com.hartwig.hmftools.common.protect.EventGenerator;
import com.hartwig.hmftools.common.serve.Knowledgebase;
import com.hartwig.hmftools.common.sv.linx.ImmutableLinxFusion;
import com.hartwig.hmftools.common.sv.linx.LinxFusion;
import com.hartwig.hmftools.serve.ServeTestFactory;
import com.hartwig.hmftools.serve.actionability.fusion.ActionableFusion;
import com.hartwig.hmftools.serve.actionability.fusion.ImmutableActionableFusion;
import com.hartwig.hmftools.serve.actionability.gene.ActionableGene;
import com.hartwig.hmftools.serve.actionability.gene.ImmutableActionableGene;
import com.hartwig.hmftools.serve.extraction.gene.GeneLevelEvent;
import com.hartwig.hmftools.serve.treatment.ImmutableTreatment;

import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;

public class FusionEvidenceTest {

    @Test
    public void canDetermineFusionEvidence() {
        ActionableGene promiscuous3_1 = ImmutableActionableGene.builder()
                .from(ServeTestFactory.createTestActionableGene())
                .gene("EGFR")
                .event(GeneLevelEvent.FUSION)
                .source(Knowledgebase.ACTIN)
                .treatment(ImmutableTreatment.builder()
                        .treament("treatment1")
                        .sourceRelevantTreatmentApproaches(Sets.newHashSet("drugClasses"))
                        .relevantTreatmentApproaches(Sets.newHashSet("drugClasses"))
                        .build())
                .build();
        ActionableGene promiscuous3_2 = ImmutableActionableGene.builder()
                .from(ServeTestFactory.createTestActionableGene())
                .gene("TP53")
                .event(GeneLevelEvent.FUSION)
                .source(Knowledgebase.ACTIN)
                .treatment(ImmutableTreatment.builder()
                        .treament("treatment2")
                        .sourceRelevantTreatmentApproaches(Sets.newHashSet("drugClasses"))
                        .relevantTreatmentApproaches(Sets.newHashSet("drugClasses"))
                        .build())
                .build();
        ActionableGene promiscuous3_3 = ImmutableActionableGene.builder()
                .from(ServeTestFactory.createTestActionableGene())
                .gene("PTEN")
                .event(GeneLevelEvent.FUSION)
                .source(Knowledgebase.ACTIN)
                .treatment(ImmutableTreatment.builder()
                        .treament("treatment3")
                        .sourceRelevantTreatmentApproaches(Sets.newHashSet("drugClasses"))
                        .relevantTreatmentApproaches(Sets.newHashSet("drugClasses"))
                        .build())
                .build();
        ActionableGene promiscuous5 = ImmutableActionableGene.builder()
                .from(ServeTestFactory.createTestActionableGene())
                .gene("BRAF")
                .event(GeneLevelEvent.FUSION)
                .source(Knowledgebase.ACTIN)
                .treatment(ImmutableTreatment.builder()
                        .treament("treatment4")
                        .sourceRelevantTreatmentApproaches(Sets.newHashSet("drugClasses"))
                        .relevantTreatmentApproaches(Sets.newHashSet("drugClasses"))
                        .build())
                .build();
        ActionableGene amp = ImmutableActionableGene.builder()
                .from(ServeTestFactory.createTestActionableGene())
                .gene("KRAS")
                .event(GeneLevelEvent.AMPLIFICATION)
                .source(Knowledgebase.CKB)
                .treatment(ImmutableTreatment.builder()
                        .treament("treatment5")
                        .sourceRelevantTreatmentApproaches(Sets.newHashSet("drugClasses"))
                        .relevantTreatmentApproaches(Sets.newHashSet("drugClasses"))
                        .build())
                .build();
        ActionableFusion fusion = ImmutableActionableFusion.builder()
                .from(ServeTestFactory.createTestActionableFusion())
                .geneUp("EML4")
                .geneDown("ALK")
                .source(Knowledgebase.CKB)
                .treatment(ImmutableTreatment.builder()
                        .treament("treatment6")
                        .sourceRelevantTreatmentApproaches(Sets.newHashSet("drugClasses"))
                        .relevantTreatmentApproaches(Sets.newHashSet("drugClasses"))
                        .build())
                .build();
        ActionableGene other = ImmutableActionableGene.builder()
                .from(ServeTestFactory.createTestActionableGene())
                .gene("AB")
                .event(GeneLevelEvent.FUSION)
                .source(Knowledgebase.CKB)
                .treatment(ImmutableTreatment.builder()
                        .treament("treatment9")
                        .sourceRelevantTreatmentApproaches(Sets.newHashSet("drugClasses"))
                        .relevantTreatmentApproaches(Sets.newHashSet("drugClasses"))
                        .build())
                .build();
        ActionableFusion ig_pair = ImmutableActionableFusion.builder()
                .from(ServeTestFactory.createTestActionableFusion())
                .geneUp("IGH")
                .geneDown("BCL2")
                .source(Knowledgebase.CKB)
                .treatment(ImmutableTreatment.builder()
                        .treament("treatment10")
                        .sourceRelevantTreatmentApproaches(Sets.newHashSet("drugClasses"))
                        .relevantTreatmentApproaches(Sets.newHashSet("drugClasses"))
                        .build())
                .build();
        ActionableGene ig_fusion = ImmutableActionableGene.builder()
                .from(ServeTestFactory.createTestActionableGene())
                .gene("IGH")
                .event(GeneLevelEvent.FUSION)
                .source(Knowledgebase.ACTIN)
                .treatment(ImmutableTreatment.builder()
                        .treament("treatment11")
                        .sourceRelevantTreatmentApproaches(Sets.newHashSet("drugClasses"))
                        .relevantTreatmentApproaches(Sets.newHashSet("drugClasses"))
                        .build())
                .build();

        FusionEvidence fusionEvidence = new FusionEvidence(EvidenceTestFactory.create(),
                Lists.newArrayList(promiscuous3_1, promiscuous3_2, promiscuous3_3, promiscuous5, amp, other, ig_fusion),
                Lists.newArrayList(fusion, ig_pair));

        LinxFusion reportedFusionMatch = create("EML4", "ALK", true, KNOWN_PAIR.toString());
        LinxFusion reportedFusionUnMatch = create("NRG1", "NRG1", true, EXON_DEL_DUP.toString());
        LinxFusion reportedPromiscuousMatch5 = create("BRAF", "other gene", true, PROMISCUOUS_5.toString());
        LinxFusion reportedPromiscuousMatch3 = create("other gene", "EGFR", true, PROMISCUOUS_3.toString());
        LinxFusion reportedPromiscuousUnMatch3 = create("other gene", "KRAS", true, PROMISCUOUS_3.toString());
        LinxFusion reportedPromiscuousNonMatch = create("other gene", "PIK3CA", true, PROMISCUOUS_3.toString());
        LinxFusion unreportedPromiscuousMatch = create("other gene", "PTEN", false, PROMISCUOUS_3.toString());
        LinxFusion reportedPromiscuousMatch = create("other gene", "CDK4", true, PROMISCUOUS_3.toString());
        LinxFusion reportedOtherMatch = create("other gene", "AB", false, NONE.toString());
        LinxFusion reportedIgPromiscuous = create("IGH", "other gene", false, IG_PROMISCUOUS.toString());
        LinxFusion reportedIgKnown = create("IGH", "BCL2", false, IG_KNOWN_PAIR.toString());

        List<LinxFusion> reportableFusions = Lists.newArrayList(reportedFusionMatch,
                reportedFusionUnMatch,
                reportedPromiscuousMatch5,
                reportedPromiscuousMatch3,
                reportedPromiscuousUnMatch3,
                reportedPromiscuousNonMatch,
                reportedPromiscuousMatch,
                reportedOtherMatch,
                reportedIgPromiscuous,
                reportedIgKnown);
        List<LinxFusion> allFusions = Lists.newArrayList(
                unreportedPromiscuousMatch);
        List<ProtectEvidence> evidences = fusionEvidence.evidence(reportableFusions, allFusions);

        assertEquals(8, evidences.size());

        ProtectEvidence evidence1 = findByFusion(evidences, reportedFusionMatch, "treatment6");
        assertTrue(evidence1.reported());
        assertEquals(evidence1.sources().size(), 1);
        assertEquals(EvidenceType.FUSION_PAIR,
                findByKnowledgebase(evidence1, "EML4 - ALK fusion", Knowledgebase.CKB, "treatment6").evidenceType());

        ProtectEvidence evidence2 = findByFusion(evidences, reportedPromiscuousMatch5, "treatment4");
        assertTrue(evidence2.reported());
        assertEquals(evidence2.sources().size(), 1);
        assertEquals(EvidenceType.PROMISCUOUS_FUSION,
                findByKnowledgebase(evidence2, "BRAF - other gene fusion", Knowledgebase.ACTIN, "treatment4").evidenceType());

        ProtectEvidence evidence3 = findByFusion(evidences, reportedPromiscuousMatch3, "treatment1");
        assertTrue(evidence3.reported());
        assertEquals(evidence3.sources().size(), 1);
        assertEquals(EvidenceType.PROMISCUOUS_FUSION,
                findByKnowledgebase(evidence3, "other gene - EGFR fusion", Knowledgebase.ACTIN, "treatment1").evidenceType());

        ProtectEvidence evidence4 = findByFusion(evidences, reportedOtherMatch, "treatment9");
        assertFalse(evidence4.reported());
        assertEquals(evidence4.sources().size(), 1);
        assertEquals(EvidenceType.PROMISCUOUS_FUSION,
                findByKnowledgebase(evidence4, "other gene - AB fusion", Knowledgebase.CKB, "treatment9").evidenceType());

        ProtectEvidence evidence5 = findByFusion(evidences, unreportedPromiscuousMatch, "treatment3");
        assertFalse(evidence5.reported());
        assertEquals(evidence5.sources().size(), 1);
        assertEquals(EvidenceType.PROMISCUOUS_FUSION,
                findByKnowledgebase(evidence5, "other gene - PTEN fusion", Knowledgebase.ACTIN, "treatment3").evidenceType());

        ProtectEvidence evidence6 = findByFusion(evidences, reportedIgPromiscuous, "treatment11");
        assertFalse(evidence6.reported());
        assertEquals(evidence6.sources().size(), 1);
        assertEquals(EvidenceType.PROMISCUOUS_FUSION,
                findByKnowledgebase(evidence6, "IGH - other gene fusion", Knowledgebase.ACTIN, "treatment11").evidenceType());

        ProtectEvidence evidence7 = findByFusion(evidences, reportedIgKnown, "treatment11");
        assertFalse(evidence7.reported());
        assertEquals(evidence7.sources().size(), 1);
        assertEquals(EvidenceType.PROMISCUOUS_FUSION,
                findByKnowledgebase(evidence7, "IGH - BCL2 fusion", Knowledgebase.ACTIN, "treatment11").evidenceType());

        ProtectEvidence evidence8 = findByFusion(evidences, reportedIgKnown, "treatment10");
        assertFalse(evidence8.reported());
        assertEquals(evidence8.sources().size(), 1);
        assertEquals(EvidenceType.FUSION_PAIR,
                findByKnowledgebase(evidence8, "IGH - BCL2 fusion", Knowledgebase.CKB, "treatment10").evidenceType());
    }

    @NotNull
    private static ProtectEvidence findByFusion(@NotNull List<ProtectEvidence> evidences, @NotNull LinxFusion fusion, @NotNull String treatment) {
        String event = EventGenerator.fusionEvent(fusion);
        for (ProtectEvidence evidence : evidences) {
            if (evidence.event().equals(event) && evidence.treatment().equals(treatment)) {
                return evidence;
            }
        }

        throw new IllegalStateException("Cannot find evidence with fusion event: " + event);
    }

    @Test
    public void canCorrectlyFilterOnExonRange() {
        int minExonUp = 5;
        int maxExonUp = 7;
        int minExonDown = 2;
        int maxExonDown = 4;

        ActionableFusion fusion = ImmutableActionableFusion.builder()
                .from(ServeTestFactory.createTestActionableFusion())
                .geneUp("EML4")
                .minExonUp(minExonUp)
                .maxExonUp(maxExonUp)
                .geneDown("ALK")
                .minExonDown(minExonDown)
                .maxExonDown(maxExonDown)
                .build();

        FusionEvidence fusionEvidence = new FusionEvidence(EvidenceTestFactory.create(), Lists.newArrayList(), Lists.newArrayList(fusion));

        ImmutableLinxFusion.Builder builder = linxFusionBuilder("EML4", "ALK", true, KNOWN_PAIR.toString());

        List<LinxFusion> onMinRange = Lists.newArrayList(builder.fusedExonUp(minExonUp).fusedExonDown(minExonDown).build());
        assertEquals(1, fusionEvidence.evidence(onMinRange, Lists.newArrayList()).size());

        List<LinxFusion> onMaxRange = Lists.newArrayList(builder.fusedExonUp(maxExonUp).fusedExonDown(maxExonDown).build());
        assertEquals(1, fusionEvidence.evidence(onMaxRange, Lists.newArrayList()).size());

        List<LinxFusion> upGeneExonTooLow = Lists.newArrayList(builder.fusedExonUp(minExonUp - 1).fusedExonDown(minExonDown).build());
        assertEquals(0, fusionEvidence.evidence(upGeneExonTooLow, Lists.newArrayList()).size());

        List<LinxFusion> upGeneExonTooHigh = Lists.newArrayList(builder.fusedExonUp(maxExonUp + 1).fusedExonDown(minExonDown).build());
        assertEquals(0, fusionEvidence.evidence(upGeneExonTooHigh, Lists.newArrayList()).size());

        List<LinxFusion> downGeneExonTooLow = Lists.newArrayList(builder.fusedExonUp(minExonUp).fusedExonDown(minExonDown - 1).build());
        assertEquals(0, fusionEvidence.evidence(downGeneExonTooLow, Lists.newArrayList()).size());

        List<LinxFusion> downGeneExonTooHigh = Lists.newArrayList(builder.fusedExonUp(maxExonUp).fusedExonDown(maxExonDown + 1).build());
        assertEquals(0, fusionEvidence.evidence(downGeneExonTooHigh, Lists.newArrayList()).size());
    }

    @NotNull
    private static LinxFusion create(@NotNull String geneStart, @NotNull String geneEnd, boolean reported, @NotNull String reportType) {
        return linxFusionBuilder(geneStart, geneEnd, reported, reportType).build();
    }

    @NotNull
    private static ImmutableLinxFusion.Builder linxFusionBuilder(@NotNull String geneStart, @NotNull String geneEnd, boolean reported,
            @NotNull String reportType) {
        return ImmutableLinxFusion.builder()
                .from(LinxTestFactory.createMinimalTestFusion())
                .geneStart(geneStart)
                .geneEnd(geneEnd)
                .reported(reported)
                .reportedType(reportType);
    }

    @NotNull
    private static KnowledgebaseSource findByKnowledgebase(@NotNull ProtectEvidence evidence, @NotNull String event,
            @NotNull Knowledgebase knowledgebaseToFind, @NotNull String treatment) {
        if (evidence.treatment().equals(treatment) && evidence.event().equals(event)) {
            for (KnowledgebaseSource source : evidence.sources()) {
                if (source.name() == knowledgebaseToFind) {
                    return source;
                }
            }
        }

        throw new IllegalStateException("Could not find evidence from source: " + knowledgebaseToFind);
    }
}