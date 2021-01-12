package com.hartwig.hmftools.ckb.clinicaltrials;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public abstract class ClinicalTrialContacts {

    @Nullable
    public abstract String name();

    @Nullable
    public abstract String email();

    @Nullable
    public abstract String phone();

    @Nullable
    public abstract String phoneExt();

    @NotNull
    public abstract String role();

}