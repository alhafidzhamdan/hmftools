package com.hartwig.hmftools.common.variant;

import java.util.List;

import com.hartwig.hmftools.common.position.GenomePosition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SomaticVariant extends GenomePosition, AllelicDepth {

    @NotNull
    String ref();

    @NotNull
    String alt();

    @NotNull
    VariantType type();

    @NotNull
    String filter();

    @Nullable
    String dbsnpID();

    @Nullable
    String cosmicID();

    @NotNull
    List<VariantAnnotation> annotations();

    double mappability();

    default boolean isDBSNP() {
        return dbsnpID() != null;
    }

    default boolean isCOSMIC() {
        return cosmicID() != null;
    }

    default boolean hasConsequence(@NotNull final VariantConsequence consequence) {
        for (final VariantAnnotation annotation : annotations()) {
            if (annotation.consequences().contains(consequence)) {
                return true;
            }
        }
        return false;
    }
}