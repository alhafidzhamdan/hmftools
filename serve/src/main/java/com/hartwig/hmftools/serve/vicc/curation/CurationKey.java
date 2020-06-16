package com.hartwig.hmftools.serve.vicc.curation;

import java.util.Objects;

import com.google.common.annotations.VisibleForTesting;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CurationKey {

    @NotNull
    private final String gene;
    @Nullable
    private final String transcript;
    @NotNull
    private final String featureName;

    public CurationKey(@NotNull final String gene, @Nullable final String transcript, @NotNull final String featureName) {
        this.gene = gene;
        this.transcript = transcript;
        this.featureName = featureName;
    }

    @VisibleForTesting
    @NotNull
    String gene() {
        return gene;
    }

    @VisibleForTesting
    @Nullable
    String transcript() {
        return transcript;
    }

    @VisibleForTesting
    @NotNull
    String featureName() {
        return featureName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CurationKey that = (CurationKey) o;
        return gene.equals(that.gene) && Objects.equals(transcript, that.transcript) && featureName.equals(that.featureName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gene, transcript, featureName);
    }
}
