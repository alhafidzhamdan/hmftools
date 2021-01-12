package com.hartwig.hmftools.ckb.drugclasses;

import java.util.List;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public abstract class DrugClassDrugs {

    @NotNull
    public abstract String id();

    @NotNull
    public abstract String drugName();

    @Nullable
    public abstract List<String> drugsTerms();
}