package com.hartwig.hmftools.lilac.evidence;

import static com.hartwig.hmftools.lilac.LilacConfig.LL_LOGGER;
import static com.hartwig.hmftools.lilac.seq.HlaSequence.WILD_STR;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.lilac.LilacConfig;
import com.hartwig.hmftools.lilac.ReferenceData;
import com.hartwig.hmftools.lilac.seq.SequenceCount;
import com.hartwig.hmftools.lilac.fragment.Fragment;
import com.hartwig.hmftools.lilac.hla.HlaAllele;
import com.hartwig.hmftools.lilac.hla.HlaContext;
import com.hartwig.hmftools.lilac.seq.HlaSequenceLoci;

public final class Candidates
{
    private final double mMinEvidence;
    private final List<HlaSequenceLoci> mNucleotideSequences;
    private final List<HlaSequenceLoci> mAminoAcidSequences;

    public Candidates(
            double minEvidence, final List<HlaSequenceLoci> nucleotideSequences, final List<HlaSequenceLoci> aminoAcidSequences)
    {
        mMinEvidence = minEvidence;
        mNucleotideSequences = nucleotideSequences;
        mAminoAcidSequences = aminoAcidSequences;
    }

    public final List<HlaAllele> unphasedCandidates(final HlaContext context, final List<Fragment> fragments, final List<HlaAllele> commonAllles)
    {
        List<Integer> aminoAcidBoundary = context.AminoAcidBoundaries;

        LL_LOGGER.info("gene({}) determining un-phased candidates from frags({})", context.geneName(), fragments.size());

        SequenceCount aminoAcidCounts = SequenceCount.aminoAcids(mMinEvidence, fragments);

        List<HlaSequenceLoci> geneCandidates = mAminoAcidSequences.stream()
                .filter(x -> x.Allele.Gene.equals(context.Gene)).collect(Collectors.toList());

        LL_LOGGER.debug("  {} candidates before filtering", geneCandidates.size());

        // Amino acid filtering
        List<HlaSequenceLoci> aminoAcidCandidates = filterSequencesByMinSupport(geneCandidates, aminoAcidCounts, context.AminoAcidBoundaries);

        List<HlaAllele> aminoAcidCandidateAlleles = aminoAcidCandidates.stream().map(x -> x.Allele).collect(Collectors.toList());

        List<HlaAllele> aminoAcidSpecificAllelesCandidates = aminoAcidCandidateAlleles.stream()
                .map(x -> x.asFourDigit()).collect(Collectors.toList());

        if (aminoAcidSpecificAllelesCandidates.isEmpty())
        {
            LL_LOGGER.warn("  no candidates after amino acid filtering - reverting to common allele gene candidates");
            return commonAllles.stream().filter(x -> x.Gene.equals(context.Gene)).collect(Collectors.toList());
        }

        LL_LOGGER.info("  {} candidates after amino acid filtering", aminoAcidCandidates.size());

        // Nucleotide filtering
        NucleotideFiltering nucleotideFiltering = new NucleotideFiltering(mMinEvidence, aminoAcidBoundary);

        List<HlaSequenceLoci> nucleotideCandidatesAfterAminoAcidFiltering = mNucleotideSequences.stream()
                .filter(x -> aminoAcidSpecificAllelesCandidates.contains(x.Allele.asFourDigit()))
                .collect(Collectors.toList());

        List<HlaSequenceLoci> nucleotideSpecificSequences = nucleotideFiltering.filterCandidatesOnAminoAcidBoundaries(
                nucleotideCandidatesAfterAminoAcidFiltering, fragments);

        List<HlaAllele> nucleotideSpecificAllelesCandidates = HlaAllele.dedup
                (nucleotideSpecificSequences.stream().map(x -> x.Allele.asFourDigit()).collect(Collectors.toList()));

        if (nucleotideSpecificAllelesCandidates.isEmpty())
        {
            LL_LOGGER.warn("  0 candidates after exon boundary filtering - reverting to amino acid candidates");
            return aminoAcidCandidateAlleles;
        }

        LL_LOGGER.info("  {} candidates after exon boundary filtering", nucleotideSpecificAllelesCandidates.size());
        return nucleotideSpecificAllelesCandidates;
    }

    private List<HlaSequenceLoci> filterSequencesByMinSupport(
            final List<HlaSequenceLoci> candidates, final SequenceCount aminoAcidCount, final List<Integer> aminoAcidBoundaries)
    {
        // eliminate sequences without min support for their amino acid at each loco, ignoring exon boundaries
        List<HlaSequenceLoci> candidateSequences = Lists.newArrayList();
        candidateSequences.addAll(candidates);

        for(int locus = 0; locus < aminoAcidCount.getLength(); ++locus)
        {
            if(aminoAcidBoundaries.contains(locus))
                continue;

            List<String> expectedSequences = aminoAcidCount.getMinCountSequences(locus);

            if(expectedSequences.isEmpty())
                continue;

            int index = 0;
            // List<HlaAllele> removedAlleles = Lists.newArrayList();
            while(index < candidateSequences.size())
            {
                HlaSequenceLoci sequence = candidateSequences.get(index);

                if(locus < sequence.length())
                {
                    String locusSeq = sequence.sequence(locus);

                    if(locusSeq.equals(WILD_STR) || expectedSequences.contains(locusSeq))
                    {
                        ++index;
                        continue;
                    }
                }

                candidateSequences.remove(index);
                // removedAlleles.add(sequence.Allele);
            }

            /*
            if(!removedAlleles.isEmpty())
            {
                LL_LOGGER.debug("locus({}) seq({}) removed {} alleles: {}",
                        locus, expectedSequences, removedAlleles.size(), HlaAllele.toString(removedAlleles, 50));
            }
            */
        }

        return candidateSequences;
    }

    public List<HlaAllele> phasedCandidates(
            final HlaContext context, final List<HlaAllele> unphasedCandidateAlleles, final List<PhasedEvidence> phasedEvidence)
    {
        LL_LOGGER.debug("determining phased candidate set for gene {}", context.geneName());

        List<HlaSequenceLoci> unphasedCandidates = mAminoAcidSequences.stream()
                .filter(x -> unphasedCandidateAlleles.contains(x.Allele.asFourDigit())).collect(Collectors.toList());

        List<HlaSequenceLoci> phasedCandidates = filterCandidates(unphasedCandidates, phasedEvidence);
        List<HlaAllele> phasedAlleles = phasedCandidates.stream().map(x -> x.Allele).collect(Collectors.toList());

        LL_LOGGER.info("gene({}) has {} candidates after phasing: {}",
                context.geneName(), phasedCandidates.size(), HlaAllele.toString(phasedAlleles, 100));

        return phasedAlleles;
    }

    private List<HlaSequenceLoci> filterCandidates(final List<HlaSequenceLoci> initialCandidates, final List<PhasedEvidence> evidence)
    {
        List<HlaSequenceLoci> candidates = Lists.newArrayList();
        candidates.addAll(initialCandidates);

        for(int i = 0; i < evidence.size(); ++i)
        {
            PhasedEvidence newEvidence = evidence.get(i);

            candidates = candidates.stream()
                .filter(x -> x.consistentWithAny(
                        newEvidence.getEvidence().keySet().stream().collect(Collectors.toList()), newEvidence.getAminoAcidLoci()))
                .collect(Collectors.toList());
        }

        return candidates;
    }

    public static void addPhasedCandidates(
            final List<HlaAllele> allAlleles, List<HlaAllele> geneCandidates, final LilacConfig config, final ReferenceData refData)
    {
        if(geneCandidates.isEmpty())
            return;

        if(config.MaxEliminationCandidates == 0 || geneCandidates.size() <= config.MaxEliminationCandidates)
        {
            allAlleles.addAll(geneCandidates);
            return;
        }

        final String gene = geneCandidates.get(0).Gene;

        refData.getAlleleFrequencies().getAlleleFrequencies().keySet().stream()
                .filter(x -> x.Gene.equals(gene)).forEach(x -> allAlleles.add(x));
    }

}
