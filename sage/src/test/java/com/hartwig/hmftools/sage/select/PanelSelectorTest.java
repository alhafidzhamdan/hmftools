package com.hartwig.hmftools.sage.select;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.utils.sv.ChrBaseRegion;

import org.junit.Test;

public class PanelSelectorTest
{
    private final List<ChrBaseRegion> panel =
            Lists.newArrayList(region(995, 995), region(998, 1102), region(1995, 1995), region(1998, 2102));

    private final PanelSelector victim = new PanelSelector(panel);

    @Test
    public void testOverlap()
    {
        assertTrue(victim.inPanel(995, 995));
        assertTrue(victim.inPanel(994, 996));
        assertTrue(victim.inPanel(1102, 1102));
        assertTrue(victim.inPanel(1000, 1002));
        assertTrue(victim.inPanel(996, 1000));
        assertFalse(victim.inPanel(1, 994));
        assertTrue(victim.inPanel(998, 998));
        assertFalse(victim.inPanel(996, 997));
        assertTrue(victim.inPanel(994, 1000));
        assertFalse(victim.inPanel(2200, 10000));
    }

    @Test
    public void testOutOfOrder()
    {
        testOverlap();
        testOverlap();
    }

    private static ChrBaseRegion region(int start, int end)
    {
        return new ChrBaseRegion("1", start, end);
    }

}
