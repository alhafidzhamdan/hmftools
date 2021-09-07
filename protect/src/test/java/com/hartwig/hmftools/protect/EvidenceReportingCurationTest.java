package com.hartwig.hmftools.protect;

import static com.hartwig.hmftools.common.protect.ProtectTestFactory.testEvidenceBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.protect.ProtectEvidence;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class EvidenceReportingCurationTest {

    @Test
    public void canApplyEventBlacklist() {
        String event1 = "any event";
        String event2 = "TP53 loss";

        ProtectEvidence evidence1 = testEvidenceBuilder().genomicEvent(event1).reported(true).build();
        ProtectEvidence evidence2 = testEvidenceBuilder().genomicEvent(event2).reported(true).build();

        List<ProtectEvidence> evidence = EvidenceReportingCuration.applyReportingBlacklist(Lists.newArrayList(evidence1, evidence2));
        assertEquals(2, evidence.size());
        assertTrue(evidence.contains(evidence1));

        ProtectEvidence blacklisted = findByEvent(evidence, event2);
        assertFalse(blacklisted.reported());
    }

    @Test
    public void canApplyTreatmentBlacklist() {
        String treatment1 = "Chemotherapy";
        String treatment2 = "Immunotherapy";

        ProtectEvidence evidence1 = testEvidenceBuilder().treatment(treatment1).reported(true).build();
        ProtectEvidence evidence2 = testEvidenceBuilder().treatment(treatment2).reported(true).build();

        List<ProtectEvidence> evidence = EvidenceReportingCuration.applyReportingBlacklist(Lists.newArrayList(evidence1, evidence2));
        assertEquals(2, evidence.size());
        assertTrue(evidence.contains(evidence2));

        ProtectEvidence blacklisted = findByTreatment(evidence, treatment1);
        assertFalse(blacklisted.reported());
    }

    @NotNull
    private static ProtectEvidence findByTreatment(@NotNull Iterable<ProtectEvidence> evidences, @NotNull String treatment) {
        for (ProtectEvidence evidence : evidences) {
            if (evidence.treatment().equals(treatment)) {
                return evidence;
            }
        }

        throw new IllegalStateException("Could not find evidence with treatment: " + treatment);
    }

    @NotNull
    private static ProtectEvidence findByEvent(@NotNull Iterable<ProtectEvidence> evidences, @NotNull String event) {
        for (ProtectEvidence evidence : evidences) {
            if (evidence.genomicEvent().equals(event)) {
                return evidence;
            }
        }

        throw new IllegalStateException("Could not find evidence with genomic event: " + event);
    }
}