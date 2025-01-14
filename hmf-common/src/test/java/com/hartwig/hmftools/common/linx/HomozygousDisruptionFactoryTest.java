package com.hartwig.hmftools.common.linx;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import com.google.common.io.Resources;

import org.junit.Test;

public class HomozygousDisruptionFactoryTest {

    private static final String LINX_DRIVERS_CATALOG_TSV = Resources.getResource("linx/sample.linx.driver.catalog.tsv").getPath();

    @Test
    public void canExtractHomozygousDisruptions() throws IOException {
        List<HomozygousDisruption> homozygousDisruptions =
                HomozygousDisruptionFactory.extractFromLinxDriverCatalogTsv(LINX_DRIVERS_CATALOG_TSV);

        assertEquals(1, homozygousDisruptions.size());

        HomozygousDisruption homozygousDisruption1 = homozygousDisruptions.get(0);
        assertEquals("9", homozygousDisruption1.chromosome());
        assertEquals("p23-p24.1", homozygousDisruption1.chromosomeBand());
        assertEquals("PTPRD", homozygousDisruption1.gene());
    }
}