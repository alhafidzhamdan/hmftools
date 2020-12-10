package com.hartwig.hmftools.common.lims;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LimsCohortTest {

    @Test
    public void canExtractCohortTypeFromSample() {
        assertEquals(LimsCohort.CPCT, LimsCohort.fromCohort("CPCT"));
        assertEquals(LimsCohort.CPCT_PANCREAS, LimsCohort.fromCohort("CPCTPancreas"));
        assertEquals(LimsCohort.DRUP, LimsCohort.fromCohort("DRUP"));
        assertEquals(LimsCohort.DRUP_STAGE3, LimsCohort.fromCohort("DRUPstage3"));
        assertEquals(LimsCohort.CORE, LimsCohort.fromCohort("CORE"));
        assertEquals(LimsCohort.CORELR02, LimsCohort.fromCohort("CORELR02"));
        assertEquals(LimsCohort.CORERI02, LimsCohort.fromCohort("CORERI02"));
        assertEquals(LimsCohort.CORELR11, LimsCohort.fromCohort("CORELR11"));
        assertEquals(LimsCohort.CORESC11, LimsCohort.fromCohort("CORESC11"));
        assertEquals(LimsCohort.WIDE, LimsCohort.fromCohort("WIDE"));
        assertEquals(LimsCohort.COREDB, LimsCohort.fromCohort("COREDB"));
    }

    @Test(expected = IllegalStateException.class)
    public void hasUnknownCohortType() {
        LimsCohort.fromCohort("ABCD");
        LimsCohort.fromCohort("");
    }
}