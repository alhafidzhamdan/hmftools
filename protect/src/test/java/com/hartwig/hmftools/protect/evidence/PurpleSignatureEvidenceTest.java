package com.hartwig.hmftools.protect.evidence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.protect.ProtectEvidence;
import com.hartwig.hmftools.common.purple.PurpleData;
import com.hartwig.hmftools.common.purple.PurpleTestFactory;
import com.hartwig.hmftools.common.variant.msi.MicrosatelliteStatus;
import com.hartwig.hmftools.common.variant.tml.TumorMutationalStatus;
import com.hartwig.hmftools.serve.ServeTestFactory;
import com.hartwig.hmftools.serve.actionability.characteristic.ActionableCharacteristic;
import com.hartwig.hmftools.serve.actionability.characteristic.ImmutableActionableCharacteristic;
import com.hartwig.hmftools.serve.extraction.characteristic.TumorCharacteristic;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class PurpleSignatureEvidenceTest {

    @Test
    public void canDeterminePurpleSignatureEvidence() {
        ActionableCharacteristic signature1 = ImmutableActionableCharacteristic.builder()
                .from(ServeTestFactory.createTestActionableCharacteristic())
                .name(TumorCharacteristic.HOMOLOGOUS_RECOMBINATION_DEFICIENT)
                .build();

        ActionableCharacteristic signature2 = ImmutableActionableCharacteristic.builder()
                .from(ServeTestFactory.createTestActionableCharacteristic())
                .name(TumorCharacteristic.HIGH_TUMOR_MUTATIONAL_LOAD)
                .build();

        ActionableCharacteristic signature3 = ImmutableActionableCharacteristic.builder()
                .from(ServeTestFactory.createTestActionableCharacteristic())
                .name(TumorCharacteristic.MICROSATELLITE_UNSTABLE)
                .build();

        PurpleSignatureEvidence purpleSignatureEvidence = new PurpleSignatureEvidence(EvidenceTestFactory.createTestEvidenceFactory(),
                Lists.newArrayList(signature1, signature2, signature3));

        PurpleData all = createPurpleData(MicrosatelliteStatus.MSI, TumorMutationalStatus.HIGH);
        List<ProtectEvidence> evidence = purpleSignatureEvidence.evidence(all);
        assertEquals(2, evidence.size());
        assertTrue(evidence.get(0).reported());
        assertTrue(evidence.get(1).reported());

        PurpleData msi = createPurpleData(MicrosatelliteStatus.MSI, TumorMutationalStatus.LOW);
        assertEquals(1, purpleSignatureEvidence.evidence(msi).size());

        PurpleData tmlHigh = createPurpleData(MicrosatelliteStatus.MSS, TumorMutationalStatus.HIGH);
        assertEquals(1, purpleSignatureEvidence.evidence(tmlHigh).size());

        PurpleData none = createPurpleData(MicrosatelliteStatus.MSS, TumorMutationalStatus.LOW);
        assertTrue(purpleSignatureEvidence.evidence(none).isEmpty());
    }

    @NotNull
    private static PurpleData createPurpleData(@NotNull MicrosatelliteStatus msStatus, @NotNull TumorMutationalStatus tmlStatus) {
        return PurpleTestFactory.testPurpleDataBuilder().microsatelliteStatus(msStatus).tumorMutationalLoadStatus(tmlStatus).build();
    }
}