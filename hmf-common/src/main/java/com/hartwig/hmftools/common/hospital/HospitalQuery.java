package com.hartwig.hmftools.common.hospital;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
public abstract class HospitalQuery {

    @Nullable
    public abstract String hospitalPA();

    @Nullable
    public abstract String analyseRequestName();

    @Nullable
    public abstract String analyseRequestEmail();

    @Nullable
    public abstract String hospitalId();

    @Nullable
    public abstract String hospitalName();

    @Nullable
    public abstract String hospitalAdres();
}
