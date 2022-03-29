package com.hartwig.hmftools.common.variant;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.drivercatalog.DriverCatalog;
import com.hartwig.hmftools.common.drivercatalog.DriverType;
import com.hartwig.hmftools.common.purple.PurpleDataLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public final class ReportableVariantFactory {
    private static final Logger LOGGER = LogManager.getLogger(ReportableVariantFactory.class);

    private ReportableVariantFactory() {
    }

    @NotNull
    public static List<ReportableVariant> toReportableGermlineVariants(@NotNull List<SomaticVariant> variants,
            @NotNull List<DriverCatalog> germlineDriverCatalog) {
        List<DriverCatalog> germlineMutationCatalog =
                germlineDriverCatalog.stream().filter(x -> x.driver() == DriverType.GERMLINE_MUTATION).collect(Collectors.toList());
        return toReportableVariants(variants, germlineMutationCatalog, ReportableVariantSource.GERMLINE);
    }

    @NotNull
    public static List<ReportableVariant> toReportableSomaticVariants(@NotNull List<SomaticVariant> variants,
            @NotNull List<DriverCatalog> somaticDriverCatalog) {
        List<DriverCatalog> somaticMutationCatalog =
                somaticDriverCatalog.stream().filter(x -> x.driver() == DriverType.MUTATION).collect(Collectors.toList());
        return toReportableVariants(variants, somaticMutationCatalog, ReportableVariantSource.SOMATIC);
    }

    @NotNull
    private static List<ReportableVariant> toReportableVariants(@NotNull List<SomaticVariant> variants,
            @NotNull List<DriverCatalog> driverCatalog, @NotNull ReportableVariantSource source) {
        Map<String, DriverCatalog> geneDriverMap = toDriverMap(driverCatalog);

        List<ReportableVariant> result = Lists.newArrayList();
        for (SomaticVariant variant : variants) {
            if (variant.reported()) {
                DriverCatalog geneDriver = geneDriverMap.get(variant.gene());

                if (geneDriver == null) {
                    throw new IllegalStateException("Could not find driver entry for variant on gene '" + variant.gene() + "'");
                }

                SomaticVariant variantCorrect = ImmutableSomaticVariantImpl.builder().from(variant).gene(formatGene(geneDriver)).build();

                ReportableVariant reportable = fromVariant(variantCorrect, source).driverLikelihood(geneDriver.driverLikelihood()).build();
                result.add(reportable);
            }
        } return result;
    }

    @NotNull
    private static String formatGene(@NotNull DriverCatalog driverCatalog) {
        String formatGene = driverCatalog.gene();
        if (formatGene.equals("CDKN2A") && driverCatalog.isCanonical()) {
            formatGene = driverCatalog.gene() + " (P16)";
        } else if (formatGene.equals("CDKN2A") && !driverCatalog.isCanonical()) {
            formatGene = driverCatalog.gene() + " (P14ARF)";
        }

        return formatGene;
    }

    @NotNull
    private static Map<String, DriverCatalog> toDriverMap(@NotNull List<DriverCatalog> driverCatalog) {
        Map<String, DriverCatalog> map = Maps.newHashMap();
        for (DriverCatalog driver : driverCatalog) {
            boolean genePresent = map.containsKey(driver.gene());
            if (!genePresent || (genePresent && driver.isCanonical()) && !driver.gene().equals("CDKN2A")) {
                map.put(driver.gene(), driver);
            }
            if (driver.gene().equals("CDKN2A")) {
                map.put(driver.gene(), driver);
            }
        }
        return map;
    }

    @NotNull
    public static List<ReportableVariant> mergeVariantLists(@NotNull List<ReportableVariant> list1,
            @NotNull List<ReportableVariant> list2) {
        List<ReportableVariant> result = Lists.newArrayList();

        Map<String, Double> maxLikelihoodPerGene = Maps.newHashMap();
        for (ReportableVariant variant : list1) {
            maxLikelihoodPerGene.merge(variant.gene(), variant.driverLikelihood(), Math::max);
        }

        for (ReportableVariant variant : list2) {
            maxLikelihoodPerGene.merge(variant.gene(), variant.driverLikelihood(), Math::max);
        }

        for (ReportableVariant variant : list1) {
            result.add(ImmutableReportableVariant.builder()
                    .from(variant)
                    .driverLikelihood(maxLikelihoodPerGene.get(variant.gene()))
                    .build());
        }

        for (ReportableVariant variant : list2) {
            result.add(ImmutableReportableVariant.builder()
                    .from(variant)
                    .driverLikelihood(maxLikelihoodPerGene.get(variant.gene()))
                    .build());
        }

        return result;
    }

    @NotNull
    public static ImmutableReportableVariant.Builder fromVariant(@NotNull SomaticVariant variant, @NotNull ReportableVariantSource source) {
        return ImmutableReportableVariant.builder()
                .type(variant.type())
                .source(source)
                .gene(variant.gene())
                .chromosome(variant.chromosome())
                .position(variant.position())
                .ref(variant.ref())
                .alt(variant.alt())
                .canonicalTranscript(variant.canonicalTranscript())
                .canonicalEffect(variant.canonicalEffect())
                .canonicalCodingEffect(variant.canonicalCodingEffect())
                .canonicalHgvsCodingImpact(variant.canonicalHgvsCodingImpact())
                .canonicalHgvsProteinImpact(variant.canonicalHgvsProteinImpact())
                .totalReadCount(variant.totalReadCount())
                .alleleReadCount(variant.alleleReadCount())
                .totalCopyNumber(variant.adjustedCopyNumber())
                .minorAlleleCopyNumber(variant.minorAlleleCopyNumber())
                .alleleCopyNumber(calcAlleleCopyNumber(variant.adjustedCopyNumber(), variant.adjustedVAF()))
                .hotspot(variant.hotspot())
                .clonalLikelihood(variant.clonalLikelihood())
                .biallelic(variant.biallelic())
                .genotypeStatus(variant.genotypeStatus())
                .localPhaseSet(variant.topLocalPhaseSet());
    }

    private static double calcAlleleCopyNumber(double adjustedCopyNumber, double adjustedVAF) {
        return adjustedCopyNumber * Math.max(0, Math.min(1, adjustedVAF));
    }
}
