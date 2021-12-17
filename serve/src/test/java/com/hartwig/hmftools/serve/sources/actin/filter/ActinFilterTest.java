package com.hartwig.hmftools.serve.sources.actin.filter;

import static org.junit.Assert.assertTrue;

import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.serve.sources.actin.ActinTestFactory;
import com.hartwig.hmftools.serve.sources.actin.reader.ActinEntry;

import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;

public class ActinFilterTest {

    @Ignore
    @Test
    public void canFilterOnFullNames() {
        ActinFilter filter = new ActinFilter(createFilterEntryList(ActinFilterType.FILTER_EXACT_VARIANT_FULLNAME, "BRAF V600E"));
        ActinEntry entry = ActinTestFactory.createEntry();
        assertTrue(filter.run(Lists.newArrayList(entry)).isEmpty());
    }

    @NotNull
    private static List<ActinFilterEntry> createFilterEntryList(@NotNull ActinFilterType type, @NotNull String value) {
        return Lists.newArrayList(ImmutableActinFilterEntry.builder().type(type).value(value).build());
    }
}