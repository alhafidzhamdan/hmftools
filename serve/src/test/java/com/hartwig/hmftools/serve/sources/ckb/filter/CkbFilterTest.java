package com.hartwig.hmftools.serve.sources.ckb.filter;

import static org.junit.Assert.assertTrue;

import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.ckb.classification.CkbConstants;
import com.hartwig.hmftools.ckb.datamodel.CkbEntry;
import com.hartwig.hmftools.serve.sources.ckb.CkbTestFactory;

import org.junit.Test;

public class CkbFilterTest {

    @Test
    public void canFilterOnKeywords() {
        CkbFilter filter = new CkbFilter();

        String firstFilterKeyword = FilterFactory.VARIANT_KEYWORDS_TO_FILTER.iterator().next();
        CkbEntry entry = CkbTestFactory.createEntryWithVariant(firstFilterKeyword + " filter me!");

        List<CkbEntry> entries = filter.run(Lists.newArrayList(entry));
        assertTrue(entries.isEmpty());

        filter.reportUnusedFilterEntries();
    }

    @Test
    public void canFilterOnGenes() {
        CkbFilter filter = new CkbFilter();

        String firstFilterGene = FilterFactory.GENES_TO_FILTER.iterator().next();
        CkbEntry filterEntry = CkbTestFactory.createEntryWithGene(firstFilterGene);
        assertTrue(filter.run(Lists.newArrayList(filterEntry)).isEmpty());

        String firstUnmappableGene = CkbConstants.UNMAPPABLE_GENES.iterator().next();
        CkbEntry unmappableEntry = CkbTestFactory.createEntryWithGene(firstUnmappableGene);
        assertTrue(filter.run(Lists.newArrayList(unmappableEntry)).isEmpty());

        filter.reportUnusedFilterEntries();
    }
}