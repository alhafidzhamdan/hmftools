package com.hartwig.hmftools.orange.cohort.percentile;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.hartwig.hmftools.orange.cohort.datamodel.Observation;
import com.hartwig.hmftools.orange.cohort.mapping.CohortConstants;
import com.hartwig.hmftools.orange.cohort.mapping.DoidCohortMapper;

import org.jetbrains.annotations.NotNull;

public class PercentileGenerator {

    @NotNull
    private final DoidCohortMapper cohortMapper;

    public PercentileGenerator(@NotNull final DoidCohortMapper cohortMapper) {
        this.cohortMapper = cohortMapper;
    }

    @NotNull
    public List<CohortPercentiles> run(@NotNull List<Observation> observations) {
        Multimap<String, Double> valuesPerCancerType = ArrayListMultimap.create();
        for (Observation observation : observations) {
            valuesPerCancerType.put(cohortMapper.cancerTypeForSample(observation.sample()), observation.value());
        }

        List<CohortPercentiles> percentiles = Lists.newArrayList();
        percentiles.add(toPercentiles(CohortConstants.PAN_CANCER_COHORT, valuesPerCancerType.values()));
        for (Map.Entry<String, Collection<Double>> entry : valuesPerCancerType.asMap().entrySet()) {
            percentiles.add(toPercentiles(entry.getKey(), entry.getValue()));
        }

        return percentiles;
    }

    @NotNull
    private static CohortPercentiles toPercentiles(@NotNull String cancerType, @NotNull Collection<Double> values) {
        List<Double> sorted = Lists.newArrayList(values);
        sorted.sort(Comparator.naturalOrder());

        List<Double> percentileValues = Lists.newArrayList();
        double baseIndex = (double) sorted.size() / PercentileConstants.BUCKET_COUNT;
        for (int i = 0; i < PercentileConstants.BUCKET_COUNT; i++) {
            int index = (int) Math.round(i * baseIndex);
            percentileValues.add(sorted.get(index));
        }

        return ImmutableCohortPercentiles.builder()
                .cancerType(cancerType)
                .cohortSize(sorted.size())
                .percentileValues(percentileValues)
                .build();
    }
}
