package com.hartwig.hmftools.lilac.qc;

import static java.lang.Math.max;

import static com.hartwig.hmftools.lilac.LilacConfig.LL_LOGGER;
import static com.hartwig.hmftools.lilac.LilacConstants.LOG_UNMATCHED_HAPLOTYPE_SUPPORT;
import static com.hartwig.hmftools.lilac.LilacConstants.WARN_UNMATCHED_HAPLOTYPE_SUPPORT;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.lilac.evidence.PhasedEvidence;
import com.hartwig.hmftools.lilac.seq.HlaSequenceLoci;
import com.hartwig.hmftools.lilac.SequenceCount;
import org.apache.commons.math3.util.Pair;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class HaplotypeQC
{
    public final int UnusedHaplotypes;
    public final int UnusedHaplotypeMaxSupport;
    public final int UnusedHaplotypeMaxLength;
    public final int UnusedHaplotypesPon;

    public final List<Haplotype> UnmatchedHaplotypes;

    private static final List<Haplotype> PON_HAPLOTYPES = Lists.newArrayList();

    public final List<String> header()
    {
        return Lists.newArrayList(
                "unusedHaplotypes", "unusedHaplotypeMaxSupport", "unusedHaplotypeMaxLength", "unusedHaplotypesPon");
    }

    public final List<String> body()
    {
        return Lists.newArrayList(
                String.valueOf(UnusedHaplotypes),
                String.valueOf(UnusedHaplotypeMaxSupport),
                String.valueOf(UnusedHaplotypeMaxLength),
                String.valueOf(UnusedHaplotypesPon));
    }

    public HaplotypeQC(
            int unusedHaplotypes, int unusedHaplotypeMaxSupport, int unusedHaplotypeMaxLength, int unusedHaplotypesPon,
            final List<Haplotype> haplotypes)
    {
        UnusedHaplotypes = unusedHaplotypes;
        UnusedHaplotypeMaxSupport = unusedHaplotypeMaxSupport;
        UnusedHaplotypeMaxLength = unusedHaplotypeMaxLength;
        UnusedHaplotypesPon = unusedHaplotypesPon;
        UnmatchedHaplotypes = haplotypes;
    }

    private static void loadPonHaplotypes()
    {
        if(!PON_HAPLOTYPES.isEmpty())
            return;

        final List<String> ponHaplotypes = new BufferedReader(new InputStreamReader(
                HaplotypeQC.class.getResourceAsStream("/pon/haplotypes.txt")))
                .lines().collect(Collectors.toList());

        ponHaplotypes.forEach(x -> PON_HAPLOTYPES.add(Haplotype.fromString(x)));
    }

    private static boolean inPon(final Haplotype haplotype)
    {
        return PON_HAPLOTYPES.stream().anyMatch(x -> x.contains(haplotype));
    }

    public static HaplotypeQC create(
            int minEvidence, final List<HlaSequenceLoci> winners, final List<PhasedEvidence> evidence, final SequenceCount aminoAcidCount)
    {
        loadPonHaplotypes();

        List<Haplotype> allUnmatched = Lists.newArrayList();
        evidence.stream().map(x -> unmatchedHaplotype(x, minEvidence, winners, aminoAcidCount)).forEach(x -> allUnmatched.addAll(x));

        Collections.sort(allUnmatched, new Haplotype.HaplotypeFragmentSorter());

        Set<String> haplotypeKeys = Sets.newHashSet();
        List<Haplotype> distinctHaplotypes = Lists.newArrayList();

        for(Haplotype haplotype : allUnmatched)
        {
            String key = String.format("%d-%d-%s", haplotype.StartLocus, haplotype.EndLocus, haplotype.Haplotype);
            if(haplotypeKeys.contains(key))
                continue;

            haplotypeKeys.add(key);
            distinctHaplotypes.add(haplotype);
        }

        Collections.sort(distinctHaplotypes, new Haplotype.HaplotypeStartLocusSorter());

        int pon = 0;
        int unusedCount = 0;
        int maxSupport = 0;
        int maxLength = 0;

        for (Haplotype unmatched : distinctHaplotypes)
        {
            if(unmatched.SupportingFragments < LOG_UNMATCHED_HAPLOTYPE_SUPPORT)
                continue;

            boolean exceedsWarnThreshold = unmatched.SupportingFragments >= WARN_UNMATCHED_HAPLOTYPE_SUPPORT;

            if (inPon(unmatched))
            {
                if(exceedsWarnThreshold)
                    pon++;

                LL_LOGGER.info("  UNMATCHED_PON_HAPLTOYPE - {}", unmatched);
            }
            else
            {
                if(exceedsWarnThreshold)
                {
                    maxSupport = max(maxSupport, unmatched.SupportingFragments);
                    maxLength = max(maxLength, unmatched.Haplotype.length());
                    unusedCount++;
                }

                LL_LOGGER.warn("  UNMATCHED_HAPLTOYPE {}", unmatched);
            }
        }

        return new HaplotypeQC(unusedCount, maxSupport, maxLength, pon, distinctHaplotypes);
    }

    private static boolean consistentWithAny(final PhasedEvidence phasedEvidence, final List<HlaSequenceLoci> winners, final String sequence)
    {
        return winners.stream().anyMatch(x -> x.consistentWith(sequence, phasedEvidence.getAminoAcidIndices()));
    }

    public static List<Haplotype> unmatchedHaplotype(
            final PhasedEvidence evidence, int minEvidence,
            final List<HlaSequenceLoci> winners, final SequenceCount aminoAcidCount)
    {
        // look through all phased evidence for AA sequences which are not supported by the winning alleles
        // ignore any wildcard sections
        Map<String,Integer> unmatched = evidence.getEvidence().entrySet().stream()
                .filter(x -> !consistentWithAny(evidence, winners, x.getKey()))
                .filter(x -> x.getValue() >= minEvidence)
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

        if(unmatched.isEmpty())
            return Lists.newArrayList();

        List<Haplotype> haplotypes = Lists.newArrayList();
        for(Map.Entry<String,Integer> entry : unmatched.entrySet())
        {
            Haplotype haplotype = Haplotype.create(evidence.getAminoAcidIndexList(), new Pair(entry.getKey(), entry.getValue()), aminoAcidCount);
            haplotypes.add(haplotype);
        }

        return haplotypes;
    }
}