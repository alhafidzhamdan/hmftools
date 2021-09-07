package com.hartwig.hmftools.geneutils.common;

import static java.lang.Math.max;
import static java.lang.Math.min;

import static com.hartwig.hmftools.common.ensemblcache.EnsemblDataCache.ENSEMBL_DATA_DIR;
import static com.hartwig.hmftools.common.ensemblcache.EnsemblDataCache.addEnsemblDir;
import static com.hartwig.hmftools.common.ensemblcache.EnsemblDataLoader.ENSEMBL_TRANS_AMINO_ACIDS_FILE;
import static com.hartwig.hmftools.common.fusion.FusionCommon.POS_STRAND;
import static com.hartwig.hmftools.common.gene.CodingBaseData.PHASE_0;
import static com.hartwig.hmftools.common.gene.CodingBaseData.PHASE_2;
import static com.hartwig.hmftools.common.gene.CodingBaseData.PHASE_NONE;
import static com.hartwig.hmftools.common.gene.TranscriptUtils.calcExonicCodingPhase;
import static com.hartwig.hmftools.common.genome.refgenome.RefGenomeSource.REF_GENOME;
import static com.hartwig.hmftools.common.genome.refgenome.RefGenomeSource.REF_GENOME_CFG_DESC;
import static com.hartwig.hmftools.common.genome.refgenome.RefGenomeSource.loadRefGenome;
import static com.hartwig.hmftools.common.utils.ConfigUtils.setLogLevel;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.OUTPUT_DIR;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.closeBufferedWriter;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.createBufferedWriter;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.parseOutputDir;
import static com.hartwig.hmftools.geneutils.common.CommonUtils.GU_LOGGER;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.hartwig.hmftools.common.codon.Codons;
import com.hartwig.hmftools.common.codon.Nucleotides;
import com.hartwig.hmftools.common.ensemblcache.EnsemblDataCache;
import com.hartwig.hmftools.common.gene.ExonData;
import com.hartwig.hmftools.common.gene.GeneData;
import com.hartwig.hmftools.common.gene.TranscriptAminoAcids;
import com.hartwig.hmftools.common.gene.TranscriptData;
import com.hartwig.hmftools.common.genome.refgenome.RefGenomeInterface;
import com.hartwig.hmftools.common.genome.refgenome.RefGenomeVersion;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jetbrains.annotations.NotNull;

public class ProteomeWriter
{
    private final EnsemblDataCache mEnsemblDataCache;
    private final RefGenomeInterface mRefGenome;
    private final boolean mCanonicalOnly;

    private static final String CANONICAL_ONLY = "canonical_only";

    private BufferedWriter mWriter;

    public ProteomeWriter(final CommandLine cmd)
    {
        mCanonicalOnly = cmd.hasOption(CANONICAL_ONLY);

        mEnsemblDataCache = new EnsemblDataCache(cmd.getOptionValue(ENSEMBL_DATA_DIR), RefGenomeVersion.V37);
        mEnsemblDataCache.setRequiredData(true, false, false, mCanonicalOnly);

        mRefGenome = loadRefGenome(cmd.getOptionValue(REF_GENOME));

        mWriter = initialiseWriter(parseOutputDir(cmd));
    }

    public void run()
    {
        mEnsemblDataCache.load(false);

        GU_LOGGER.info("writing proteome to file");

        int geneCount = 0;

        for(Map.Entry<String,List<GeneData>> entry : mEnsemblDataCache.getChrGeneDataMap().entrySet())
        {
            for(GeneData geneData : entry.getValue())
            {
                List<TranscriptData> transDataList = mEnsemblDataCache.getTranscripts(geneData.GeneId);

                if(transDataList == null)
                    continue;

                for(TranscriptData transData : transDataList)
                {
                    if(mCanonicalOnly && !transData.IsCanonical)
                        continue;

                    processTranscript(geneData, transData);

                    if(mCanonicalOnly)
                        break;
                }

                ++geneCount;
            }
        }

        closeBufferedWriter(mWriter);

        GU_LOGGER.info("wrote {} gene amino-acids sequences", geneCount);

        GU_LOGGER.info("proteome write complete");
    }

    private void processTranscript(final GeneData geneData, final TranscriptData transData)
    {
        if(transData.CodingStart == null)
            return;

        boolean inCoding = false;
        String aminoAcids = "";

        if(transData.Strand == POS_STRAND)
        {
            StringBuilder codingBases = new StringBuilder();

            for(int i = 0; i < transData.exons().size(); ++i)
            {
                ExonData exon = transData.exons().get(i);

                if(exon.End < transData.CodingStart)
                    continue;

                if(exon.Start > transData.CodingEnd)
                    break;

                int exonCodingStart = max(transData.CodingStart, exon.Start);
                int exonCodingEnd = min(transData.CodingEnd, exon.End);

                if(!inCoding)
                {
                    inCoding = true;

                    if(exon.Start == transData.CodingStart)
                    {
                        int startPhase = exon.PhaseStart == PHASE_NONE ? PHASE_0 : exon.PhaseStart;

                        if(startPhase == PHASE_2)
                            exonCodingStart += 2;
                        else if(startPhase == PHASE_0)
                            exonCodingStart += 1;
                    }
                }

                codingBases.append(mRefGenome.getBaseString(geneData.Chromosome, exonCodingStart, exonCodingEnd));
            }

            aminoAcids = Codons.aminoAcidFromBases(codingBases.toString());
        }
        else
        {
            String codingBases = "";
            for(int i = transData.exons().size() - 1; i >= 0; --i)
            {
                ExonData exon = transData.exons().get(i);

                if(exon.Start > transData.CodingEnd)
                    continue;

                if(exon.End < transData.CodingStart)
                    break;

                int exonCodingStart = max(transData.CodingStart, exon.Start);
                int exonCodingEnd = min(transData.CodingEnd, exon.End);

                if(!inCoding)
                {
                    inCoding = true;

                    if(exon.End == transData.CodingEnd)
                    {
                        int startPhase = exon.PhaseStart == PHASE_NONE ? PHASE_0 : exon.PhaseStart;

                        if(startPhase == PHASE_2)
                            exonCodingStart -= 2;
                        else if(startPhase == PHASE_0)
                            exonCodingStart -= 1;
                    }
                }

                codingBases = mRefGenome.getBaseString(geneData.Chromosome, exonCodingStart, exonCodingEnd) + codingBases;
            }

            aminoAcids = Codons.aminoAcidFromBases(Nucleotides.reverseStrandBases(codingBases));
        }

        writeData(geneData.GeneId, geneData.GeneName, transData.TransName, transData.IsCanonical, aminoAcids);
    }

    private static BufferedWriter initialiseWriter(final String outputDir)
    {
        try
        {
            String filename = outputDir + ENSEMBL_TRANS_AMINO_ACIDS_FILE;
            BufferedWriter writer = createBufferedWriter(filename, false);

            writer.write(TranscriptAminoAcids.csvHeader());
            writer.newLine();
            return writer;
        }
        catch(IOException e)
        {
            GU_LOGGER.error("failed to initialise CSV file output: {}", e.toString());
            return null;
        }
    }

    private void writeData(final String geneId, final String geneName, final String transName, boolean isCanonical, final String aminoAcids)
    {
        try
        {
            mWriter.write(String.format("%s,%s,%s,%s,%s", geneId, geneName, transName, isCanonical, aminoAcids));
            mWriter.newLine();
        }
        catch(IOException e)
        {
            GU_LOGGER.error("failed to write peptide data: {}", e.toString());
        }
    }

    public static void main(@NotNull final String[] args) throws ParseException
    {
        final Options options = new Options();
        addEnsemblDir(options);
        options.addOption(REF_GENOME, true, REF_GENOME_CFG_DESC);
        options.addOption(OUTPUT_DIR, true, "Output directory");
        options.addOption(CANONICAL_ONLY, false, "Only write canonical proteome");

        final CommandLine cmd = createCommandLine(args, options);

        setLogLevel(cmd);

        ProteomeWriter proteomeWriter = new ProteomeWriter(cmd);
        proteomeWriter.run();
    }

    @NotNull
    public static CommandLine createCommandLine(@NotNull final String[] args, @NotNull final Options options) throws ParseException
    {
        final CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

}
