package com.hartwig.hmftools.serve.sources.docm;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
public abstract class DocmEntry {

    @NotNull
    public abstract String gene();

    @NotNull
    public abstract String transcript();

    @NotNull
    public abstract String proteinAnnotation();

}
