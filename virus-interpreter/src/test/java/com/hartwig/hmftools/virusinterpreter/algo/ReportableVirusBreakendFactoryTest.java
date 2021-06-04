package com.hartwig.hmftools.virusinterpreter.algo;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.virus.VirusBreakend;
import com.hartwig.hmftools.common.virus.VirusBreakendQCStatus;
import com.hartwig.hmftools.common.virus.VirusInterpretation;
import com.hartwig.hmftools.common.virus.VirusTestFactory;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class ReportableVirusBreakendFactoryTest {

    @Test
    public void canInterpretVirusBreakendForReportingPos() {
        List<VirusBreakend> virusBreakends = createTestVirusBreakends();

        Map<Integer, String> taxonomyMap = Maps.newHashMap();
        taxonomyMap.put(1, "Human papillomavirus type 16");
        TaxonomyDb taxonomyDb = new TaxonomyDb(taxonomyMap);

        Map<Integer, VirusInterpretation> virusInterpretationMap = Maps.newHashMap();
        virusInterpretationMap.put(1, VirusInterpretation.HPV);
        virusInterpretationMap.put(2, VirusInterpretation.EBV);
        VirusInterpretationModel virusInterpretationModel = new VirusInterpretationModel(virusInterpretationMap);

        VirusBlacklistModel virusBlacklistModel = new VirusBlacklistModel(Sets.newHashSet(1), Sets.newHashSet());

        ReportableVirusBreakendFactory factory =
                new ReportableVirusBreakendFactory(taxonomyDb, virusInterpretationModel, virusBlacklistModel);
        assertEquals(1, factory.analyze(virusBreakends).size());

        ReportableVirusBreakend reportableVirusbreakend = factory.analyze(virusBreakends).get(0);
        assertEquals("Human papillomavirus type 16", reportableVirusbreakend.virusName());
        assertEquals(2, reportableVirusbreakend.integrations());
        assertEquals(VirusInterpretation.HPV, reportableVirusbreakend.interpretation());
    }

    @NotNull
    private static List<VirusBreakend> createTestVirusBreakends() {
        List<VirusBreakend> virusBreakends = Lists.newArrayList();

        // This one should be added.
        virusBreakends.add(VirusTestFactory.testVirusBreakendBuilder()
                .referenceTaxid(1)
                .taxidGenus(2)
                .taxidSpecies(1)
                .integrations(2)
                .build());

        // This one has a blacklisted genus taxid
        virusBreakends.add(VirusTestFactory.testVirusBreakendBuilder()
                .referenceTaxid(1)
                .taxidGenus(1)
                .taxidSpecies(1)
                .integrations(2)
                .build());

        // This one has a failed QC
        virusBreakends.add(VirusTestFactory.testVirusBreakendBuilder()
                .referenceTaxid(1)
                .taxidGenus(2)
                .taxidSpecies(1)
                .qcStatus(VirusBreakendQCStatus.LOW_VIRAL_COVERAGE)
                .integrations(2)
                .build());

        // This one has no integrations
        virusBreakends.add(VirusTestFactory.testVirusBreakendBuilder()
                .referenceTaxid(1)
                .taxidGenus(2)
                .taxidSpecies(1)
                .integrations(0)
                .build());

        return virusBreakends;
    }
}