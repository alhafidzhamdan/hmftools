package com.hartwig.hmftools.serve.sources.ckb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.ckb.datamodel.CkbEntry;
import com.hartwig.hmftools.common.serve.Knowledgebase;
import com.hartwig.hmftools.common.serve.actionability.EvidenceDirection;
import com.hartwig.hmftools.common.serve.actionability.EvidenceLevel;
import com.hartwig.hmftools.serve.cancertype.CancerTypeConstants;
import com.hartwig.hmftools.serve.sources.ckb.treatementapproach.RelevantTreatmentApproachCurationType;
import com.hartwig.hmftools.serve.sources.ckb.treatementapproach.RelevantTreatmentApprochCurationEntry;
import com.hartwig.hmftools.serve.sources.ckb.treatementapproach.RelevantTreatmentApprochCurationEntryKey;
import com.hartwig.hmftools.serve.sources.ckb.treatementapproach.RelevantTreatmentAproachCuration;
import com.hartwig.hmftools.serve.sources.ckb.treatementapproach.RelevantTreatmentAprroachCurationTest;

import org.apache.logging.log4j.util.Strings;
import org.junit.Test;

public class ActionableEntryFactoryTest {

    @Test
    public void canCreateActionableEntries() {
        Map<RelevantTreatmentApprochCurationEntryKey, RelevantTreatmentApprochCurationEntry> curationEntries = Maps.newHashMap();
        curationEntries.put(RelevantTreatmentAprroachCurationTest.canGenerateCurationKey("A",
                        "A",
                        "BRAF amplification",
                        EvidenceLevel.A,
                        EvidenceDirection.RESPONSIVE),
                RelevantTreatmentAprroachCurationTest.canGenerateCurationEntry(RelevantTreatmentApproachCurationType.TREATMENT_APPROACH_CURATION,
                        "A",
                        "A",
                        "BRAF amplification",
                        EvidenceLevel.A,
                        EvidenceDirection.RESPONSIVE,
                        "AA"));
        RelevantTreatmentAproachCuration curator = new RelevantTreatmentAproachCuration(curationEntries);

        CkbEntry entryDeletion =
                CkbTestFactory.createEntry("KRAS", "deletion", "KRAS deletion", "sensitive", "Emerging", "AB", "AB", "A", "DOID:162");
        Set<ActionableEntry> entryDeletionSet =
                ActionableEntryFactory.toActionableEntries(entryDeletion, "KRAS", curator, "gene", entryDeletion.type());
        assertEquals(0, entryDeletionSet.size());

        CkbEntry entryCharacteristics =
                CkbTestFactory.createEntry("-", "MSI neg", "MSI neg", "sensitive", "Actionable", "AB", "AB", "A", "DOID:162");
        Set<ActionableEntry> entryCharacteristicsSet =
                ActionableEntryFactory.toActionableEntries(entryCharacteristics, Strings.EMPTY, curator, "-", entryCharacteristics.type());
        assertEquals(1, entryCharacteristicsSet.size());
        ActionableEntry characteristics = entryCharacteristicsSet.iterator().next();
        assertEquals(Strings.EMPTY, characteristics.sourceEvent());
        assertEquals(Knowledgebase.CKB, characteristics.source());
        assertEquals("AB", characteristics.treatment().treament());
        assertEquals("AB", characteristics.applicableCancerType().name());
        assertEquals("162", characteristics.applicableCancerType().doid());
        assertEquals(Sets.newHashSet(CancerTypeConstants.REFRACTORY_HEMATOLOGIC_TYPE,
                CancerTypeConstants.BONE_MARROW_TYPE,
                CancerTypeConstants.LEUKEMIA_TYPE), characteristics.blacklistCancerTypes());
        assertEquals(EvidenceLevel.A, characteristics.level());
        assertEquals(EvidenceDirection.RESPONSIVE, characteristics.direction());

        CkbEntry entryAmplification = CkbTestFactory.createEntry("KRAS",
                "KRAS amplification",
                "KRAS amplification",
                "sensitive",
                "Actionable",
                "AB",
                "AB",
                "A",
                "DOID:163");
        Set<ActionableEntry> entryAmplificationSet =
                ActionableEntryFactory.toActionableEntries(entryAmplification, "KRAS", curator, "KRAS", entryAmplification.type());
        assertEquals(1, entryAmplificationSet.size());
        ActionableEntry amplification = entryAmplificationSet.iterator().next();
        assertEquals("KRAS", amplification.sourceEvent());
        assertEquals(Knowledgebase.CKB, amplification.source());
        assertEquals("AB", amplification.treatment().treament());
        assertEquals("AB", amplification.applicableCancerType().name());
        assertEquals("163", amplification.applicableCancerType().doid());
        assertTrue(amplification.blacklistCancerTypes().isEmpty());
        assertEquals(EvidenceLevel.A, amplification.level());
        assertEquals(EvidenceDirection.RESPONSIVE, amplification.direction());

        CkbEntry entryHotspot =
                CkbTestFactory.createEntry("BRAF", "BRAF V600E", "BRAF V600E", "sensitive", "Actionable", "AB", "AB", "A", "DOID:162");
        Set<ActionableEntry> entryHotspotSet =
                ActionableEntryFactory.toActionableEntries(entryHotspot, "BRAF", curator, "BRAF", entryHotspot.type());
        assertEquals(1, entryHotspotSet.size());
        ActionableEntry hotspot = entryHotspotSet.iterator().next();
        assertEquals("BRAF", hotspot.sourceEvent());
        assertEquals(Knowledgebase.CKB, hotspot.source());
        assertEquals("AB", hotspot.treatment().treament());
        assertEquals("AB", hotspot.applicableCancerType().name());
        assertEquals("162", hotspot.applicableCancerType().doid());
        assertEquals(Sets.newHashSet(CancerTypeConstants.REFRACTORY_HEMATOLOGIC_TYPE,
                CancerTypeConstants.BONE_MARROW_TYPE,
                CancerTypeConstants.LEUKEMIA_TYPE), hotspot.blacklistCancerTypes());
        assertEquals(EvidenceLevel.A, characteristics.level());
        assertEquals(EvidenceDirection.RESPONSIVE, characteristics.direction());
    }

    @Test
    public void canExtractAndCurateDoid() {
        assertNull(ActionableEntryFactory.extractAndCurateDoid(null));
        assertNull(ActionableEntryFactory.extractAndCurateDoid(new String[] { "jax", "not a doid" }));

        assertEquals("0060463", ActionableEntryFactory.extractAndCurateDoid(new String[] { "DOID", "0060463" }));
        assertEquals(CancerTypeConstants.CANCER_DOID, ActionableEntryFactory.extractAndCurateDoid(new String[] { "JAX", "10000003" }));
        assertEquals(CancerTypeConstants.SQUAMOUD_CELL_CARCINOMA_OF_UNKNOWN_PRIMARY,
                ActionableEntryFactory.extractAndCurateDoid(new String[] { "JAX", "10000009" }));
        assertEquals(CancerTypeConstants.ADENOCARCINOMA_OF_UNKNOWN_PRIMARY,
                ActionableEntryFactory.extractAndCurateDoid(new String[] { "JAX", "10000008" }));
        assertNull(ActionableEntryFactory.extractAndCurateDoid(new String[] { "JAX", "10000004" }));
    }

    @Test
    public void canExtractSourceCancerTypeID() {
        assertNull(ActionableEntryFactory.extractSourceCancerTypeId(null));
        assertNull(ActionableEntryFactory.extractSourceCancerTypeId("not a doid"));

        assertNotNull(ActionableEntryFactory.extractSourceCancerTypeId("DOID:0060463"));
        assertEquals("0060463", ActionableEntryFactory.extractSourceCancerTypeId("DOID:0060463")[1]);
        assertEquals("10000003", ActionableEntryFactory.extractSourceCancerTypeId("JAX:10000003")[1]);
    }

    @Test
    public void canConvertToUrlString() {
        assertEquals("predicted+-+sensitive", ActionableEntryFactory.toUrlString("predicted - sensitive"));
        assertEquals("predicted+-+resistant", ActionableEntryFactory.toUrlString("predicted - resistant"));
        assertEquals("resistant", ActionableEntryFactory.toUrlString("resistant"));
        assertEquals("sensitive", ActionableEntryFactory.toUrlString("sensitive"));
    }

    @Test
    public void canDetermineIfHasUsableEvidenceType() {
        assertTrue(ActionableEntryFactory.hasUsableEvidenceType("Actionable"));
        assertFalse(ActionableEntryFactory.hasUsableEvidenceType("Prognostic"));
        assertFalse(ActionableEntryFactory.hasUsableEvidenceType("Emerging"));
        assertFalse(ActionableEntryFactory.hasUsableEvidenceType("Risk Factor"));
        assertFalse(ActionableEntryFactory.hasUsableEvidenceType("Diagnostic"));
    }

    @Test
    public void canResolveLevels() {
        assertNull(ActionableEntryFactory.resolveLevel("NA"));
        assertEquals(EvidenceLevel.A, ActionableEntryFactory.resolveLevel("A"));
        assertEquals(EvidenceLevel.B, ActionableEntryFactory.resolveLevel("B"));
        assertEquals(EvidenceLevel.C, ActionableEntryFactory.resolveLevel("C"));
        assertEquals(EvidenceLevel.D, ActionableEntryFactory.resolveLevel("D"));
    }

    @Test
    public void canResolveDirections() {
        assertNull(ActionableEntryFactory.resolveDirection(null));
        assertNull(ActionableEntryFactory.resolveDirection("unknown"));
        assertNull(ActionableEntryFactory.resolveDirection("not applicable"));
        assertNull(ActionableEntryFactory.resolveDirection("conflicting"));
        assertNull(ActionableEntryFactory.resolveDirection("no benefit"));
        assertNull(ActionableEntryFactory.resolveDirection("not predictive"));
        assertNull(ActionableEntryFactory.resolveDirection("decreased response"));

        assertEquals(EvidenceDirection.RESPONSIVE, ActionableEntryFactory.resolveDirection("sensitive"));
        assertEquals(EvidenceDirection.PREDICTED_RESPONSIVE, ActionableEntryFactory.resolveDirection("predicted - sensitive"));
        assertEquals(EvidenceDirection.RESISTANT, ActionableEntryFactory.resolveDirection("resistant"));
        assertEquals(EvidenceDirection.PREDICTED_RESISTANT, ActionableEntryFactory.resolveDirection("predicted - resistant"));
    }
}