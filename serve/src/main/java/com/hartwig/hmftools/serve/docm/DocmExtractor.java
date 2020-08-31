package com.hartwig.hmftools.serve.docm;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.variant.hotspot.VariantHotspot;
import com.hartwig.hmftools.serve.hotspot.HotspotGenerator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class DocmExtractor {

    private static final Logger LOGGER = LogManager.getLogger(DocmExtractor.class);

    @NotNull
    private final HotspotGenerator hotspotGenerator;

    public DocmExtractor(@NotNull final HotspotGenerator hotspotGenerator) {
        this.hotspotGenerator = hotspotGenerator;
    }

    @NotNull
    public Map<DocmEntry, List<VariantHotspot>> extractFromDocmEntries(@NotNull List<DocmEntry> entries) {
        Map<DocmEntry, List<VariantHotspot>> hotspotsPerEntry = Maps.newHashMap();
        for (DocmEntry entry : entries) {
            if (HotspotGenerator.isResolvableProteinAnnotation(entry.proteinAnnotation())) {
                hotspotsPerEntry.put(entry, hotspotGenerator.generateHotspots(entry.gene(), entry.transcript(), entry.proteinAnnotation()));
            } else {
                LOGGER.warn("Cannot resolve DoCM protein annotation: '{}:p.{}'", entry.gene(), entry.proteinAnnotation());
            }
        }
        return hotspotsPerEntry;
    }
}