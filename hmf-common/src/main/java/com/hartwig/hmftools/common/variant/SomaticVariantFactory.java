package com.hartwig.hmftools.common.variant;

import static com.hartwig.hmftools.common.variant.VariantHeader.PURPLE_GERMLINE_INFO;
import static com.hartwig.hmftools.common.variant.enrich.KataegisEnrichment.KATAEGIS_FLAG;
import static com.hartwig.hmftools.common.variant.enrich.Subclonality.SUBCLONAL_LIKELIHOOD_FLAG;

import static htsjdk.tribble.AbstractFeatureReader.getFeatureReader;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.genotype.GenotypeStatus;
import com.hartwig.hmftools.common.purple.region.GermlineStatus;
import com.hartwig.hmftools.common.sage.SageMetaData;
import com.hartwig.hmftools.common.variant.filter.HumanChromosomeFilter;
import com.hartwig.hmftools.common.variant.filter.NTFilter;
import com.hartwig.hmftools.common.variant.impact.VariantImpact;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import htsjdk.tribble.AbstractFeatureReader;
import htsjdk.tribble.readers.LineIterator;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.filter.CompoundFilter;
import htsjdk.variant.variantcontext.filter.PassingVariantFilter;
import htsjdk.variant.variantcontext.filter.VariantContextFilter;
import htsjdk.variant.vcf.VCFCodec;
import htsjdk.variant.vcf.VCFHeader;

public class SomaticVariantFactory implements VariantContextFilter {

    @NotNull
    public static SomaticVariantFactory passOnlyInstance() {
        return new SomaticVariantFactory(new PassingVariantFilter());
    }

    public static final String MAPPABILITY_TAG = "MAPPABILITY";
    private static final String RECOVERED_FLAG = "RECOVERED";

    public static final String PASS_FILTER = "PASS";

    @NotNull
    private final CompoundFilter filter;

    public SomaticVariantFactory(@NotNull final VariantContextFilter... filters) {
        this.filter = new CompoundFilter(true);
        this.filter.addAll(Arrays.asList(filters));
        this.filter.add(new HumanChromosomeFilter());
        this.filter.add(new NTFilter());
    }

    @NotNull
    public List<SomaticVariant> fromVCFFile(@NotNull final String tumor, @NotNull final String vcfFile) throws IOException {
        return fromVCFFile(tumor, null, null, vcfFile);
    }

    @NotNull
    public List<SomaticVariant> fromVCFFile(@NotNull final String tumor, @NotNull final String reference, @NotNull final String vcfFile)
            throws IOException {
        return fromVCFFileWithoutCheck(tumor, reference, null, vcfFile);
    }

    @NotNull
    public List<SomaticVariant> fromVCFFile(@NotNull final String tumor, @Nullable final String reference, @Nullable final String rna,
            @NotNull final String vcfFile) throws IOException {
        final List<SomaticVariant> result = Lists.newArrayList();
        fromVCFFile(tumor, reference, rna, vcfFile, true, result::add);
        return result;
    }

    @NotNull
    public List<SomaticVariant> fromVCFFileWithoutCheck(@NotNull final String tumor, @Nullable final String reference,
            @Nullable final String rna, @NotNull final String vcfFile) throws IOException {
        final List<SomaticVariant> result = Lists.newArrayList();
        fromVCFFile(tumor, reference, rna, vcfFile, false, result::add);
        return result;
    }

    public void fromVCFFile(@NotNull final String tumor, @Nullable final String reference, @Nullable final String rna,
            @NotNull final String vcfFile, boolean useCheckReference, @NotNull Consumer<SomaticVariant> consumer) throws IOException {
        try (final AbstractFeatureReader<VariantContext, LineIterator> reader = getFeatureReader(vcfFile, new VCFCodec(), false)) {
            final VCFHeader header = (VCFHeader) reader.getHeader();
            if (!sampleInFile(tumor, header)) {
                throw new IllegalArgumentException("Sample " + tumor + " not found in vcf file " + vcfFile);
            }

            if (useCheckReference) {
                if (reference != null && !sampleInFile(reference, header)) {
                    throw new IllegalArgumentException("Sample " + reference + " not found in vcf file " + vcfFile);
                }
            }

            if (rna != null && !sampleInFile(rna, header)) {
                throw new IllegalArgumentException("Sample " + rna + " not found in vcf file " + vcfFile);
            }

            if (!header.hasFormatLine("AD")) {
                throw new IllegalArgumentException("Allelic depths is a required format field in vcf file " + vcfFile);
            }

            for (VariantContext variant : reader.iterator()) {
                if (filter.test(variant)) {
                    createVariant(tumor, reference, rna, variant).ifPresent(consumer);
                }
            }
        }
    }

    @NotNull
    public Optional<SomaticVariant> createVariant(@NotNull final String sample, @NotNull final VariantContext context) {
        return createVariant(sample, null, null, context);
    }

    @NotNull
    public Optional<SomaticVariant> createVariant(@NotNull final String sample, @Nullable final String reference,
            @Nullable final String rna, @NotNull final VariantContext context) {
        final Genotype genotype = context.getGenotype(sample);

        final VariantContextDecorator decorator = new VariantContextDecorator(context);
        final GenotypeStatus genotypeStatus = reference != null ? decorator.genotypeStatus(reference) : null;

        if (filter.test(context) && AllelicDepth.containsAllelicDepth(genotype)) {
            final AllelicDepth tumorDepth = AllelicDepth.fromGenotype(context.getGenotype(sample));

            final Optional<AllelicDepth> referenceDepth = Optional.ofNullable(reference)
                    .flatMap(x -> Optional.ofNullable(context.getGenotype(x)))
                    .filter(AllelicDepth::containsAllelicDepth)
                    .map(AllelicDepth::fromGenotype);

            final Optional<AllelicDepth> rnaDepth = Optional.ofNullable(rna)
                    .flatMap(x -> Optional.ofNullable(context.getGenotype(x)))
                    .filter(AllelicDepth::containsAllelicDepth)
                    .map(AllelicDepth::fromGenotype);

            if (tumorDepth.totalReadCount() > 0) {
                ImmutableSomaticVariantImpl.Builder builder = createVariantBuilder(tumorDepth, context);
                builder.genotypeStatus(genotypeStatus != null ? genotypeStatus : GenotypeStatus.UNKNOWN);

                return Optional.of(builder)
                        .map(x -> x.rnaDepth(rnaDepth.orElse(null)))
                        .map(x -> x.referenceDepth(referenceDepth.orElse(null)))
                        .map(ImmutableSomaticVariantImpl.Builder::build);
            }
        }
        return Optional.empty();
    }

    @NotNull
    private static ImmutableSomaticVariantImpl.Builder createVariantBuilder(@NotNull final AllelicDepth allelicDepth,
            @NotNull final VariantContext context) {
        final VariantContextDecorator decorator = new VariantContextDecorator(context);
        final VariantImpact variantImpact = decorator.variantImpact();

        ImmutableSomaticVariantImpl.Builder builder = ImmutableSomaticVariantImpl.builder()
                .qual(decorator.qual())
                .type(decorator.type())
                .filter(decorator.filter())
                .chromosome(decorator.chromosome())
                .position(decorator.position())
                .ref(decorator.ref())
                .alt(decorator.alt())
                .alleleReadCount(allelicDepth.alleleReadCount())
                .totalReadCount(allelicDepth.totalReadCount())
                .hotspot(decorator.hotspot())
                .minorAlleleCopyNumber(decorator.minorAlleleCopyNumber())
                .adjustedCopyNumber(decorator.adjustedCopyNumber())
                .adjustedVAF(decorator.adjustedVaf())
                .variantCopyNumber(decorator.variantCopyNumber())
                .mappability(decorator.mappability())
                .tier(decorator.tier())
                .trinucleotideContext(decorator.trinucleotideContext())
                .microhomology(decorator.microhomology())
                .repeatCount(decorator.repeatCount())
                .repeatSequence(decorator.repeatSequence())
                // Note: getAttributeAsBoolean(x, false) is safer than hasAttribute(x)
                .reported(decorator.reported())
                .biallelic(decorator.biallelic())
                .worstEffect(variantImpact.WorstEffect)
                .worstCodingEffect(variantImpact.WorstCodingEffect)
                .worstEffectTranscript(variantImpact.WorstTranscript)
                .canonicalEffect(variantImpact.CanonicalEffect)
                .canonicalTranscript(variantImpact.CanonicalTranscript)
                .canonicalCodingEffect(variantImpact.CanonicalCodingEffect)
                .canonicalHgvsCodingImpact(variantImpact.CanonicalHgvsCodingImpact)
                .canonicalHgvsProteinImpact(variantImpact.CanonicalHgvsProteinImpact)
                .gene(variantImpact.gene())
                .genesAffected(variantImpact.GenesAffected)
                .subclonalLikelihood(context.getAttributeAsDouble(SUBCLONAL_LIKELIHOOD_FLAG, 0))
                .germlineStatus(GermlineStatus.valueOf(context.getAttributeAsString(PURPLE_GERMLINE_INFO, "UNKNOWN")))
                .kataegis(context.getAttributeAsString(KATAEGIS_FLAG, Strings.EMPTY))
                .recovered(context.getAttributeAsBoolean(RECOVERED_FLAG, false));

        if (context.hasAttribute(SageMetaData.PHASED_INFRAME_INDEL)) {
            builder.phasedInframeIndelIdentifier(context.getAttributeAsInt(SageMetaData.PHASED_INFRAME_INDEL, 0));
        }

        if (context.hasAttribute(SageMetaData.LOCAL_PHASE_SET)) {
            builder.localPhaseSet(context.getAttributeAsInt(SageMetaData.LOCAL_PHASE_SET, 0));
        }

        if (context.hasAttribute(SageMetaData.LOCAL_REALIGN_SET)) {
            builder.localRealignmentSet(context.getAttributeAsInt(SageMetaData.LOCAL_REALIGN_SET, 0));
        }

        return builder;
    }

    private static boolean sampleInFile(@NotNull final String sample, @NotNull final VCFHeader header) {
        return header.getSampleNamesInOrder().stream().anyMatch(x -> x.equals(sample));
    }

    @Override
    public boolean test(final VariantContext variantContext) {
        return filter.test(variantContext);
    }
}
