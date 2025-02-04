package com.hartwig.hmftools.common.genome.position;

import java.util.Optional;
import java.util.function.Consumer;

import com.hartwig.hmftools.common.genome.region.GenomeRegion;

import org.jetbrains.annotations.NotNull;

public interface GenomePositionSelector<P extends GenomePosition>  {

    @NotNull
    Optional<P> select(@NotNull final GenomePosition position);

    void select(GenomeRegion region, Consumer<P> handler);
}
