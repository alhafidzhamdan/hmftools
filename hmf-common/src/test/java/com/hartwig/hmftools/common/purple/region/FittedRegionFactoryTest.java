package com.hartwig.hmftools.common.purple.region;

import static com.hartwig.hmftools.common.purple.region.FittedRegionFactory.NORMAL_BAF;
import static com.hartwig.hmftools.common.purple.region.FittedRegionFactory.bafDeviation;
import static com.hartwig.hmftools.common.purple.region.FittedRegionFactory.cnvDeviation;
import static com.hartwig.hmftools.common.purple.region.FittedRegionFactory.isEven;
import static com.hartwig.hmftools.common.purple.region.FittedRegionFactory.modelBAF;
import static com.hartwig.hmftools.common.purple.region.FittedRegionFactory.modelBAFToMinimizeDeviation;
import static com.hartwig.hmftools.common.purple.region.FittedRegionFactory.modelRatio;
import static com.hartwig.hmftools.common.purple.region.FittedRegionFactory.purityAdjustedBAF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class FittedRegionFactoryTest {

    private static final double EPSILON = 1e-10;

    @Test
    public void expectedFit() {
        final FittedRegionFactory victim = new FittedRegionFactory(12, 0.2);
        final FittedRegion result = victim.fitRegion(0.8, 0.7, create(180d / 280d + 0.01, 0.98 - 0.01));
        assertEquals(3, result.fittedPloidy());
        assertEquals(0.01, result.bafDeviation(), EPSILON);
        assertEquals(0.002, result.cnvDeviation(), EPSILON);
        assertEquals(0.011058009947947143, result.deviation(), EPSILON);
        assertEquals(0.6786402753872633, result.purityAdjustedBAF(), EPSILON);
    }

    @NotNull
    private static ObservedRegion create(final double baf, final double ratio) {
        return ImmutableEnrichedRegion.builder().observedBAF(baf).bafCount(1).chromosome("1").start(1).end(
                2).observedTumorRatio(ratio).observedNormalRatio(1).build();
    }

    @Test
    public void diploidModelCNVRatio() {
        diploidModelCNVRatio(0.5, 0.4, 0.5);
        diploidModelCNVRatio(1, 0.4, 1);
        diploidModelCNVRatio(1, 0.6, 1);
    }

    @Test
    public void pureModelCNVRatio() {
        pureModelCNVRatio(1, 1, 2);
        pureModelCNVRatio(1.5, 1, 3);
        pureModelCNVRatio(2, 1, 4);

        pureModelCNVRatio(0.5, 0.5, 2);
        pureModelCNVRatio(0.75, 0.5, 3);
        pureModelCNVRatio(1, 0.5, 4);
    }

    @Test
    public void testModelCNVRatio() {
        assertModelCNVRatio(1.4, 0.8, 1, 3);
        assertModelCNVRatio(1.8, 0.8, 1, 4);

        assertModelCNVRatio(0.8 + 0.36, 0.9, 0.8, 3);
        assertModelCNVRatio(0.8 + 0.72, 0.9, 0.8, 4);

        assertModelCNVRatio(1.2, 1, 0.8, 3);
        assertModelCNVRatio(1.6, 1, 0.8, 4);
        assertModelCNVRatio(2.0, 1, 0.8, 5);
    }

    @Test
    public void testCNVDeviation() {
        assertCNVDeviation(0, 1, 0.3, 0.3);
        assertCNVDeviation(0, 0.8, 0.4, 0.4);

        assertCNVDeviation(0.05, 0.5, 1, 1.1);

        assertCNVDeviation(0.1, 1, 1, 1.1);
        assertCNVDeviation(0.1, 1, 1.5, 1.6);
        assertCNVDeviation(0.1, 1, 2, 2.1);
    }

    @Test
    public void testModelBAF() {
        assertModelBAF(10d / 28d, 0.8, 3, 1);
        assertModelBAF(18d / 28d, 0.8, 3, 2);
        assertModelBAF(26d / 28d, 0.8, 3, 3);

        assertModelBAF(NORMAL_BAF, 0.8, 4, 2);
    }

    @Test
    public void testModelBAFToMinimizeDeviation() {
        assertModelBAFToMinimizeDeviation(1, 1, 2, 1);

        assertModelBAFToMinimizeDeviation(18d / 28d, 0.8, 3, 0.65);
        assertModelBAFToMinimizeDeviation(26d / 28d, 0.8, 3, 0.95);

        assertModelBAFToMinimizeDeviation(17d / 27d, 0.7, 3, 0.65);
        assertModelBAFToMinimizeDeviation(24d / 27d, 0.7, 3, 0.95);
    }

    @Test
    public void testBAFDeviation() {
        assertEquals(0.01, bafDeviation(true, NORMAL_BAF, NORMAL_BAF + 0.01), EPSILON);
        assertEquals(0, bafDeviation(true, NORMAL_BAF, NORMAL_BAF), EPSILON);
        assertEquals(0, bafDeviation(true, NORMAL_BAF, NORMAL_BAF - 0.01), EPSILON);
        assertEquals(0, bafDeviation(true, NORMAL_BAF, NORMAL_BAF - 0.02), EPSILON);
    }

    @Test
    public void testIsEvenCopyNumber() {

        assertFalse(isEven(0.5));
        assertFalse(isEven(0.75));
        assertFalse(isEven(1));
        assertFalse(isEven(1.74));

        assertTrue(isEven(1.75));
        assertTrue(isEven(2));
        assertTrue(isEven(2.25));

        assertFalse(isEven(2.5));
        assertFalse(isEven(2.75));
        assertFalse(isEven(3));
        assertFalse(isEven(3.74));

        assertTrue(isEven(3.75));
        assertTrue(isEven(4));
        assertTrue(isEven(4.25));
    }

    @Test
    public void testPurityAdjustedBaf() {
        testPurityAdjustedBaf(0.1);
        testPurityAdjustedBaf(0.2);
        testPurityAdjustedBaf(0.3);
        testPurityAdjustedBaf(0.4);
        testPurityAdjustedBaf(0.5);
        testPurityAdjustedBaf(0.6);
        testPurityAdjustedBaf(0.7);
        testPurityAdjustedBaf(0.8);
        testPurityAdjustedBaf(0.9);
    }

    private void testPurityAdjustedBaf(double purity) {
        testPurityAdjustedBaf(purity, 2, 1);
        testPurityAdjustedBaf(purity, 2, 2);
        testPurityAdjustedBaf(purity, 3, 2);
        testPurityAdjustedBaf(purity, 3, 3);
        testPurityAdjustedBaf(purity, 4, 2);
        testPurityAdjustedBaf(purity, 4, 3);
        testPurityAdjustedBaf(purity, 4, 4);
        testPurityAdjustedBaf(purity, 5, 3);
        testPurityAdjustedBaf(purity, 5, 4);
    }

    private static void testPurityAdjustedBaf(final double purity, final int ploidy, final int alleleCount) {
        double expectedPurityAdjustedBAF = 1d * alleleCount / ploidy;
        double observedBAF = modelBAF(purity, ploidy, alleleCount);
        assertEquals(expectedPurityAdjustedBAF, purityAdjustedBAF(purity, ploidy, observedBAF), EPSILON);
    }

    private static void assertModelBAFToMinimizeDeviation(double expectedBAF, double purity, int ploidy,
            double actualBAF) {
        assertEquals(expectedBAF, modelBAFToMinimizeDeviation(purity, ploidy, actualBAF)[0], EPSILON);
    }

    private static void assertModelBAF(double expectedBAF, double purity, int ploidy, int betaAllele) {
        assertEquals(expectedBAF, modelBAF(purity, ploidy, betaAllele), EPSILON);
    }

    private static void assertCNVDeviation(double expectedDeviation, double cnvRatioWeighFactor, double modelCNVRatio,
            double tumorRatio) {
        assertEquals(expectedDeviation, cnvDeviation(cnvRatioWeighFactor, modelCNVRatio, tumorRatio), EPSILON);
    }

    private static void assertModelCNVRatio(double expectedRatio, double purity, double normFactor, int ploidy) {
        assertEquals(expectedRatio, modelRatio(purity, normFactor, ploidy), EPSILON);
    }

    private static void diploidModelCNVRatio(double expectedRatio, double purity, double normFactor) {
        assertEquals(expectedRatio, modelRatio(purity, normFactor, 2), EPSILON);
    }

    private static void pureModelCNVRatio(double expectedRatio, double normFactor, int ploidy) {
        assertEquals(expectedRatio, modelRatio(1, normFactor, ploidy), EPSILON);
    }
}
