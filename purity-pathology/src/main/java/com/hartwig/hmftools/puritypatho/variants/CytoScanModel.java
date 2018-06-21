package com.hartwig.hmftools.puritypatho.variants;

import java.util.Map;


import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })

public abstract class CytoScanModel {

    @NotNull
    public abstract Map<String, CytoScanModel> dataCyto();
}
