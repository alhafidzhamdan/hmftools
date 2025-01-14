package com.hartwig.hmftools.virusinterpreter.coverages;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
public abstract class CoveragesAnalysis {

    public abstract double expectedClonalCoverage();
}
