package com.hartwig.hmftools.common.purple.segment;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.hartwig.hmftools.common.centromeres.Centromeres;
import com.hartwig.hmftools.common.chromosome.ChromosomeLength;
import com.hartwig.hmftools.common.chromosome.HumanChromosome;
import com.hartwig.hmftools.common.position.GenomePosition;
import com.hartwig.hmftools.common.region.GenomeRegion;

import org.jetbrains.annotations.NotNull;

public class PurpleSegmentFactoryNew2 {

    private static final Map<String, GenomeRegion> CENTROMERES = Centromeres.grch37();

    @NotNull
    public static List<PurpleSegment> segment(@NotNull final Multimap<String, Cluster> clusters,
            @NotNull final Map<String, ChromosomeLength> lengths) {
        final List<PurpleSegment> results = Lists.newArrayList();
        results.addAll(segmentMap(clusters, lengths).values());
        Collections.sort(results);
        return results;
    }

    @NotNull
    static Multimap<String, PurpleSegment> segmentMap(@NotNull final Multimap<String, Cluster> clusters,
            @NotNull final Map<String, ChromosomeLength> lengths) {

        final Multimap<String, PurpleSegment> segments = ArrayListMultimap.create();
        for (String chromosome : lengths.keySet()) {
            if (HumanChromosome.contains(chromosome)) {
                final Collection<Cluster> cluster = clusters.containsKey(chromosome) ? clusters.get(chromosome) : Collections.emptyList();
                segments.putAll(chromosome, create(lengths.get(chromosome), cluster));
            }
        }

        return segments;
    }

    @NotNull
    public static List<PurpleSegment> create(@NotNull final ChromosomeLength chromosome, @NotNull final Collection<Cluster> clusters) {

        final List<PurpleSegment> result = Lists.newArrayList();
        ModifiablePurpleSegment segment = create(chromosome.chromosome(), 1);
        for (final Cluster cluster : clusters) {

            boolean ratioSupport = !cluster.ratios().isEmpty();

            final List<StructuralVariantPosition> variants = cluster.variants();
            if (!variants.isEmpty()) {
                for (final StructuralVariantPosition variant : variants) {
                    if (variant.position() != segment.start()) {
                        result.add(setStatus(segment.setEnd(variant.position() - 1)));
                        segment = createFromCluster(cluster, variant, ratioSupport);
                    } else {
                        segment.setStructuralVariantSupport(StructuralVariantSupport.MULTIPLE);
                    }
                }
                segment.setStatus(PurpleSegmentStatus.NORMAL);
            } else {

                final List<GenomePosition> ratioPositions = cluster.ratios();

                // DO ALL
//                for (GenomePosition genomePosition : ratioPositions) {
//                    result.add(setStatus(segment.setEnd(genomePosition.position() - 1)));
//                    segment = create(genomePosition.chromosome(), genomePosition.position());
//                }

                // DO FIRST AND LAST
                final GenomePosition firstRatioBreak = ratioPositions.get(0);
                result.add(setStatus(segment.setEnd(firstRatioBreak.position() - 1)));
                segment = create(firstRatioBreak.chromosome(), firstRatioBreak.position());
                if (ratioPositions.size() > 1) {
                    final GenomePosition finalRatioPosition = ratioPositions.get(ratioPositions.size() - 1);
                    result.add(setStatus(segment.setEnd(finalRatioPosition.position() - 1)));
                    segment = create(firstRatioBreak.chromosome(), finalRatioPosition.position());
                }
            }
        }

        result.add(segment.setEnd(chromosome.position()));
        return result;
    }

    private static ModifiablePurpleSegment create(String chromosome, long start) {
        return ModifiablePurpleSegment.create()
                .setChromosome(chromosome)
                .setRatioSupport(true)
                .setStart(start)
                .setEnd(0)
                .setStatus(PurpleSegmentStatus.NORMAL)
                .setStructuralVariantSupport(StructuralVariantSupport.NONE);
    }

    private static ModifiablePurpleSegment createFromCluster(Cluster cluster, StructuralVariantPosition variant, boolean ratioSupport) {
        return ModifiablePurpleSegment.create()
                .setChromosome(cluster.chromosome())
                .setRatioSupport(ratioSupport)
                .setStart(variant.position())
                .setEnd(0)
                .setStatus(cluster.variants().size() > 1 ? PurpleSegmentStatus.CLUSTER : PurpleSegmentStatus.NORMAL)
                .setStructuralVariantSupport(StructuralVariantSupport.fromVariant(variant.type()));
    }

    private static ModifiablePurpleSegment setStatus(@NotNull ModifiablePurpleSegment segment) {
        final GenomeRegion centromere = CENTROMERES.get(segment.chromosome());
        if (centromere != null && centromere.overlaps(segment)) {
            segment.setStatus(PurpleSegmentStatus.CENTROMERE);
        }

        return segment;
    }
}
