package com.hartwig.hmftools.ckb.json.common;

import java.util.List;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public abstract class DrugInfo {

    public abstract int id();

    @NotNull
    public abstract String drugName();

    @NotNull
    public abstract List<String> terms();
}
