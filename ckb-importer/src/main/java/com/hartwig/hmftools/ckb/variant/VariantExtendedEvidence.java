package com.hartwig.hmftools.ckb.variant;

import java.util.List;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public abstract class VariantExtendedEvidence {

    @NotNull
    public abstract String id();

    @NotNull
    public abstract String approvalStatus();

    @NotNull
    public abstract String evidenceType();

    @NotNull
    public abstract String efficacyEvidence();

    @NotNull
    public abstract VariantMolecularProfile molecularProfile();

    @NotNull
    public abstract VariantTherapy therapy();

    @NotNull
    public abstract VariantIndication indication();

    @NotNull
    public abstract String responseType();

    @NotNull
    public abstract List<VarinatReference> reference();

    @NotNull
    public abstract String ampCapAscoEvidenceLevel();

    @NotNull
    public abstract String ampCapAscoInferredTier();
}
