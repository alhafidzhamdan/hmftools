package com.hartwig.hmftools.serve.sources.actin.curation;

import java.util.Map;

import com.google.common.collect.Maps;

final class CurationFactory {

    static final Map<String, String> GENE_MAPPINGS = Maps.newHashMap();

    static final Map<CurationEntry, CurationEntry> MUTATION_MAPPINGS = Maps.newHashMap();

    private CurationFactory() {
    }

    static {
        GENE_MAPPINGS.put("AKT", "AKT1");
        GENE_MAPPINGS.put("PRA1", "RABAC1");
        GENE_MAPPINGS.put("MRE11", "MRE11A");
    }
}