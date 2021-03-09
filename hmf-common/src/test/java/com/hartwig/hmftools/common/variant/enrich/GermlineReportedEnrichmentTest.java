package com.hartwig.hmftools.common.variant.enrich;

import static com.hartwig.hmftools.common.variant.VariantHeader.PURPLE_BIALLELIC_FLAG;
import static com.hartwig.hmftools.common.variant.enrich.HotspotEnrichment.HOTSPOT_FLAG;
import static com.hartwig.hmftools.common.variant.enrich.HotspotEnrichment.NEAR_HOTSPOT_FLAG;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.drivercatalog.DriverCategory;
import com.hartwig.hmftools.common.drivercatalog.panel.DriverGene;
import com.hartwig.hmftools.common.drivercatalog.panel.DriverGeneGermlineReporting;
import com.hartwig.hmftools.common.drivercatalog.panel.ImmutableDriverGene;
import com.hartwig.hmftools.common.variant.CodingEffect;
import com.hartwig.hmftools.common.variant.VariantContextDecorator;
import com.hartwig.hmftools.common.variant.VariantContextFromString;

import org.junit.Test;

import htsjdk.variant.variantcontext.VariantContext;

public class GermlineReportedEnrichmentTest {

    @Test
    public void testReportHotspot() {
        List<VariantContext> consumer = Lists.newArrayList();
        DriverGene driverGene = createDriverGene("KD53", DriverGeneGermlineReporting.NONE, DriverGeneGermlineReporting.ANY);
        GermlineReportedEnrichment victim = new GermlineReportedEnrichment(Lists.newArrayList(driverGene), Collections.emptyList(), consumer::add);
        victim.accept(createGermline("PASS", "KD53", false, true, "UNKNOWN", CodingEffect.NONE));
        victim.flush();

        assertEquals(1, consumer.size());
        assertTrue(new VariantContextDecorator(consumer.get(0)).reported());
    }

    @Test
    public void testDoNotReportFailedHotspot() {
        List<VariantContext> consumer = Lists.newArrayList();
        DriverGene driverGene = createDriverGene("KD53", DriverGeneGermlineReporting.NONE, DriverGeneGermlineReporting.ANY);
        GermlineReportedEnrichment victim = new GermlineReportedEnrichment(Lists.newArrayList(driverGene), Collections.emptyList(), consumer::add);
        victim.accept(createGermline("PASS", "KD53", false, true, "UNKNOWN", CodingEffect.NONE));
        victim.accept(createGermline("FILTERED", "KD53", false, true, "UNKNOWN", CodingEffect.NONE));
        victim.flush();

        assertEquals(2, consumer.size());
        assertTrue(new VariantContextDecorator(consumer.get(0)).reported());
        assertFalse(new VariantContextDecorator(consumer.get(1)).reported());
    }

    @Test
    public void testDoNotReportHotspotWhenNone() {
        List<VariantContext> consumer = Lists.newArrayList();
        DriverGene driverGene = createDriverGene("KD53", DriverGeneGermlineReporting.NONE, DriverGeneGermlineReporting.NONE);
        GermlineReportedEnrichment victim = new GermlineReportedEnrichment(Lists.newArrayList(driverGene), Collections.emptyList(), consumer::add);
        victim.accept(createGermline("PASS", "KD53", false, true, "UNKNOWN", CodingEffect.NONE));
        victim.flush();

        assertEquals(1, consumer.size());
        assertFalse(new VariantContextDecorator(consumer.get(0)).reported());
    }

    @Test
    public void testDoNotReportSingleHotspotWhenWildType() {
        List<VariantContext> consumer = Lists.newArrayList();
        DriverGene driverGene = createDriverGene("KD53", DriverGeneGermlineReporting.WILDTYPE_LOST, DriverGeneGermlineReporting.WILDTYPE_LOST);
        GermlineReportedEnrichment victim = new GermlineReportedEnrichment(Lists.newArrayList(driverGene), Collections.emptyList(), consumer::add);
        victim.accept(createGermline("PASS", "KD53", false, true, "UNKNOWN", CodingEffect.NONE));
        victim.accept(createGermline("FILTERED", "KD53", false, false, "Pathogenic", CodingEffect.NONE));
        victim.flush();

        assertEquals(2, consumer.size());
        assertFalse(new VariantContextDecorator(consumer.get(0)).reported());
        assertFalse(new VariantContextDecorator(consumer.get(1)).reported());
    }

    @Test
    public void testDoNotReportSingleHotspotWhenWildTypeLostInUnreportedVariant() {
        List<VariantContext> consumer = Lists.newArrayList();
        DriverGene driverGene = createDriverGene("KD53", DriverGeneGermlineReporting.NONE, DriverGeneGermlineReporting.WILDTYPE_LOST);
        GermlineReportedEnrichment victim = new GermlineReportedEnrichment(Lists.newArrayList(driverGene), Collections.emptyList(), consumer::add);
        victim.accept(createGermline("PASS", "KD53", false, true, "UNKNOWN", CodingEffect.NONE));
        victim.accept(createGermline("PASS", "KD53", false, false, "Pathogenic", CodingEffect.NONE));
        victim.flush();

        assertEquals(2, consumer.size());
        assertFalse(new VariantContextDecorator(consumer.get(0)).reported());
        assertFalse(new VariantContextDecorator(consumer.get(1)).reported());
    }

    @Test
    public void testReportHotspotWhenWildTypeLost() {
        List<VariantContext> consumer = Lists.newArrayList();
        DriverGene driverGene = createDriverGene("KD53", DriverGeneGermlineReporting.WILDTYPE_LOST, DriverGeneGermlineReporting.WILDTYPE_LOST);
        GermlineReportedEnrichment victim = new GermlineReportedEnrichment(Lists.newArrayList(driverGene), Collections.emptyList(), consumer::add);
        victim.accept(createGermline("PASS", "KD53", false, true, "UNKNOWN", CodingEffect.NONE));
        victim.accept(createGermline("PASS", "KD53", false, false, "Pathogenic", CodingEffect.NONE));
        victim.flush();

        assertEquals(2, consumer.size());
        assertTrue(new VariantContextDecorator(consumer.get(0)).reported());
        assertTrue(new VariantContextDecorator(consumer.get(1)).reported());
    }

    @Test
    public void testReportUnknownFrameshift() {
        List<VariantContext> consumer = Lists.newArrayList();
        DriverGene driverGene = createDriverGene("KD53", DriverGeneGermlineReporting.ANY, DriverGeneGermlineReporting.NONE);
        GermlineReportedEnrichment victim = new GermlineReportedEnrichment(Lists.newArrayList(driverGene), Collections.emptyList(), consumer::add);
        victim.accept(createGermline("PASS", "KD53", false, false, "UNKNOWN", CodingEffect.NONSENSE_OR_FRAMESHIFT));
        victim.flush();

        assertEquals(1, consumer.size());
        assertTrue(new VariantContextDecorator(consumer.get(0)).reported());
    }

    @Test
    public void testReportPathogenic() {
        List<VariantContext> consumer = Lists.newArrayList();
        DriverGene driverGene = createDriverGene("KD53", DriverGeneGermlineReporting.ANY, DriverGeneGermlineReporting.NONE);
        GermlineReportedEnrichment victim = new GermlineReportedEnrichment(Lists.newArrayList(driverGene), Collections.emptyList(), consumer::add);
        victim.accept(createGermline("PASS", "KD53", false, false, "Pathogenic", CodingEffect.NONE));
        victim.flush();

        assertEquals(1, consumer.size());
        assertTrue(new VariantContextDecorator(consumer.get(0)).reported());
    }

    @Test
    public void testReportBiallelicAsWildTypeLost() {
        List<VariantContext> consumer = Lists.newArrayList();
        DriverGene driverGene = createDriverGene("KD53", DriverGeneGermlineReporting.ANY, DriverGeneGermlineReporting.WILDTYPE_LOST);
        GermlineReportedEnrichment victim = new GermlineReportedEnrichment(Lists.newArrayList(driverGene), Collections.emptyList(), consumer::add);
        victim.accept(createGermline("PASS", "KD53", true, false, "Pathogenic", CodingEffect.NONE));
        victim.flush();

        assertEquals(1, consumer.size());
        assertTrue(new VariantContextDecorator(consumer.get(0)).reported());
    }


    @Test
    public void testIgnoreSinglePathogenicWhenWildType() {
        List<VariantContext> consumer = Lists.newArrayList();
        DriverGene driverGene = createDriverGene("KD53", DriverGeneGermlineReporting.WILDTYPE_LOST, DriverGeneGermlineReporting.NONE);
        GermlineReportedEnrichment victim = new GermlineReportedEnrichment(Lists.newArrayList(driverGene), Collections.emptyList(), consumer::add);
        victim.accept(createGermline("PASS", "KD53", false, false, "Pathogenic", CodingEffect.NONE));
        victim.flush();

        assertEquals(1, consumer.size());
        assertFalse(new VariantContextDecorator(consumer.get(0)).reported());
    }

    @Test
    public void testSomaticCountTowardsWildTypeLost() {
        List<VariantContext> consumer = Lists.newArrayList();
        DriverGene driverGene = createDriverGene("KD53", DriverGeneGermlineReporting.WILDTYPE_LOST, DriverGeneGermlineReporting.NONE);
        GermlineReportedEnrichment victim = new GermlineReportedEnrichment(Lists.newArrayList(driverGene), Lists.newArrayList(createReportedSomaticVariant("KD53")), consumer::add);
        victim.accept(createGermline("PASS", "KD53", false, false, "Pathogenic", CodingEffect.NONE));
        victim.flush();

        assertEquals(1, consumer.size());
        assertTrue(new VariantContextDecorator(consumer.get(0)).reported());
    }


    private static VariantContext createGermline(String filter, String gene, boolean biallelic, boolean isHotspot, String clinsig,
            CodingEffect codingEffect) {
        final String hotspotFlag = isHotspot ? HOTSPOT_FLAG : NEAR_HOTSPOT_FLAG;
        final String line =
                "11\t1000\tCOSM123;COSM456\tG\tA\t100\t" + filter + "\t" + PURPLE_BIALLELIC_FLAG + "=" + biallelic + ";" + hotspotFlag
                        + ";SEC=" + gene + ",ENST00000393562,UTR_variant," + codingEffect.toString() + ",c.-275T>G,;CLNSIG=" + clinsig
                        + "\tGT:AD:DP\t0/1:73,17:91";
        return VariantContextFromString.decode(line);
    }

    private static VariantContext createReportedSomaticVariant(String gene) {
        final String line = "11\t1000\tCOSM123;COSM456\tG\tA\t100\tPASS\tREPORTED;SEW=" + gene
                + ",ENST00000393562,UTR_variant,NONE,1;||\tGT:AD:DP\t0/1:73,17:91";
        return VariantContextFromString.decode(line);
    }

    private DriverGene createDriverGene(String gene, DriverGeneGermlineReporting variantReporting,
            DriverGeneGermlineReporting hotspotReporting) {
        return ImmutableDriverGene.builder()
                .gene(gene)
                .reportDisruption(false)
                .reportDeletion(false)
                .reportNonsenseAndFrameshift(false)
                .reportMissenseAndInframe(false)
                .reportGermlineHotspot(hotspotReporting)
                .reportGermlineVariant(variantReporting)
                .likelihoodType(DriverCategory.TSG)
                .reportAmplification(false)
                .reportSomaticHotspot(false)
                .reportSplice(false)
                .build();
    }

}