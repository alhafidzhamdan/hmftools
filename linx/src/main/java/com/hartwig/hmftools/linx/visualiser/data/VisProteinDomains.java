package com.hartwig.hmftools.linx.visualiser.data;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.linx.visualiser.file.VisProteinDomainFile;

import org.jetbrains.annotations.NotNull;

public class VisProteinDomains
{

    public static final String UTR = "UTR/Non-coding";

    @NotNull
    public static List<ProteinDomain> readProteinDomains(@NotNull final String fileName, @NotNull final List<Fusion> fusions)
            throws IOException
    {
        final List<ProteinDomain> all =
                VisProteinDomainFile.read(fileName).stream().map(VisProteinDomains::fromFile).collect(Collectors.toList());
        return proteinDomainsInFusionGenes(fusions, all);
    }

    @NotNull
    public static List<ProteinDomain> exonicProteinDomains(@NotNull final List<ProteinDomain> proteinDomains,
            @NotNull final List<Exon> exons)
    {

        final List<ProteinDomain> result = Lists.newArrayList();
        for (ProteinDomain proteinDomain : proteinDomains)
        {
            if (proteinDomain.name().equals(UTR))
            {

                final List<Exon> proteinDomainExons =
                        exons.stream().filter(x -> x.transcript().equals(proteinDomain.transcript())).sorted().collect(Collectors.toList());

                for (Exon exon : proteinDomainExons)
                {
                    if (proteinDomain.overlaps(exon))
                    {
                        final ProteinDomain exonicProteinDomain = ImmutableProteinDomain.builder().from(proteinDomain)
                                .start(Math.max(proteinDomain.start(), exon.start()))
                                .end(Math.min(proteinDomain.end(), exon.end())).build();

                        result.add(exonicProteinDomain);
                    }
                }
            }
            else
            {
                result.add(proteinDomain);
            }

        }

        return result;
    }

    @NotNull
    private static ProteinDomain fromFile(@NotNull final VisProteinDomainFile file)
    {
        return ImmutableProteinDomain.builder()
                .sampleId(file.SampleId)
                .clusterId(file.ClusterId)
                .chromosome(file.Chromosome)
                .start(file.Start)
                .end(file.End)
                .name(utr(file.Info))
                .transcript(file.Transcript)
                .build();
    }

    @NotNull
    private static List<ProteinDomain> proteinDomainsInFusionGenes(@NotNull final List<Fusion> fusions,
            @NotNull final List<ProteinDomain> proteinDomains)
    {
        final Set<String> transcripts = Sets.newHashSet();
        fusions.forEach(x ->
        {
            transcripts.add(x.transcriptUp());
            transcripts.add(x.transcriptDown());
        });

        return proteinDomains.stream().filter(x -> transcripts.contains(x.transcript())).collect(Collectors.toList());
    }

    @NotNull
    private static String utr(@NotNull final String utr)
    {
        switch (utr)
        {
            case "Non Coding":
            case "5-Prime UTR":
            case "3-Prime UTR":
                return UTR;
        }

        return utr;
    }

}
