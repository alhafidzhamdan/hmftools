package com.hartwig.hmftools.patientreporter.cfreport.data;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.actionability.ActionabilitySource;
import com.hartwig.hmftools.common.actionability.EvidenceItem;
import com.hartwig.hmftools.common.actionability.EvidenceLevel;
import com.hartwig.hmftools.common.actionability.EvidenceScope;
import com.hartwig.hmftools.common.actionability.ImmutableEvidenceItem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public final class EvidenceDrugTypeMergerTest {
    private static final Logger LOGGER = LogManager.getLogger(EvidenceDrugTypeMergerTest.class);

    @Test
    public void evidenceMergerUnknownDrugsType() {
        List<EvidenceItem> evidenceItems = Lists.newArrayList();
        ImmutableEvidenceItem.Builder onLabelBuilder = evidenceBuilder().isOnLabel(true);

        evidenceItems.add(onLabelBuilder.event("BRAF p.Val600Glu")
                .drug("A + B")
                .drugsType("Unknown")
                .level(EvidenceLevel.LEVEL_A)
                .response("Responsive")
                .reference("V600E")
                .source(ActionabilitySource.ONCOKB)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(onLabelBuilder.event("BRAF p.Val600Glu")
                .drug("D")
                .drugsType("Unknown")
                .level(EvidenceLevel.LEVEL_A)
                .response("Responsive")
                .reference("V600E")
                .source(ActionabilitySource.ONCOKB)
                .scope(EvidenceScope.SPECIFIC)
                .build());

        evidenceItems.add(onLabelBuilder.event("BRAF p.Val600Glu")
                .drug("C")
                .drugsType("Unknown")
                .level(EvidenceLevel.LEVEL_A)
                .response("Responsive")
                .reference("V600E")
                .source(ActionabilitySource.ONCOKB)
                .scope(EvidenceScope.SPECIFIC)
                .build());
      //  LOGGER.info(evidenceItems);
     //   LOGGER.info(EvidenceDrugTypeMerger.merge(evidenceItems));

        assertEquals(1, EvidenceDrugTypeMerger.merge(evidenceItems).size());





    }

    @NotNull
    private static ImmutableEvidenceItem.Builder evidenceBuilder() {
        return ImmutableEvidenceItem.builder().cancerType(Strings.EMPTY);
    }

}