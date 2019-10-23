package com.hartwig.hmftools.linx.neoepitope;

import static com.hartwig.hmftools.common.io.FileWriterUtils.closeBufferedWriter;
import static com.hartwig.hmftools.common.io.FileWriterUtils.createBufferedWriter;
import static com.hartwig.hmftools.linx.LinxConfig.DATA_OUTPUT_DIR;
import static com.hartwig.hmftools.linx.LinxConfig.GENE_TRANSCRIPTS_DIR;
import static com.hartwig.hmftools.linx.LinxConfig.LOG_DEBUG;
import static com.hartwig.hmftools.linx.LinxConfig.REF_GENOME_FILE;
import static com.hartwig.hmftools.linx.LinxConfig.formOutputPath;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;

public class KmerGenerator
{
    private RefGenomeInterface mRefGenome;
    private final String mOutputDir;
    private final String mKmerInputFile;

    private static final Logger LOGGER = LogManager.getLogger(KmerGenerator.class);

    public KmerGenerator(final String refGenomeFile, final String kmerInputFile, final String outputDir)
    {
        try
        {
            IndexedFastaSequenceFile refGenome =
                    new IndexedFastaSequenceFile(new File(refGenomeFile));
            mRefGenome = new RefGenomeSource(refGenome);
        }
        catch(IOException e)
        {
            LOGGER.error("failed to load ref genome: {}", e.toString());
        }

        mKmerInputFile = kmerInputFile;
        mOutputDir = outputDir;
    }

    private static final int COL_CHR = 0;
    private static final int COL_POS_START = 1;
    private static final int COL_ORIENT_START = 2;
    private static final int COL_POS_END = 3;
    private static final int COL_ORIENT_END = 4;
    private static final int COL_BASE_LENGTH = 5;
    private static final int COL_INSERT_SEQ = 6;

    public void generateKmerData()
    {
        if(mOutputDir.isEmpty() || mKmerInputFile.isEmpty())
            return;

        try
        {
            BufferedWriter writer;

            final String outputFileName = mOutputDir + "LNX_KMER_STRINGS.csv";

            writer = createBufferedWriter(outputFileName, false);
            writer.write("Chromosome,PosStart,OrientStart,PosEnd,OrientEnd,KmerPosStrand,KmerNegStrand");
            writer.newLine();

            BufferedReader fileReader = new BufferedReader(new FileReader(mKmerInputFile));

            String line = fileReader.readLine(); // skip header

            while ((line = fileReader.readLine()) != null)
            {
                // parse CSV data
                String[] items = line.split(",", -1);

                if(items.length < COL_INSERT_SEQ+1)
                    continue;

                final String chromosome = items[COL_CHR];
                long posStart = Long.parseLong(items[COL_POS_START]);
                int orientStart = Integer.parseInt(items[COL_ORIENT_START]);
                long posEnd = Long.parseLong(items[COL_POS_END]);
                int orientEnd = Integer.parseInt(items[COL_ORIENT_END]);
                int baseLength = Integer.parseInt(items[COL_BASE_LENGTH]);
                final String insertSeq = items[COL_INSERT_SEQ];

                LOGGER.debug("producing KMER for chr({}) pos({} -> {})", chromosome, posStart, posEnd);

                final String kmerStringStart = getBaseString(chromosome, posStart, orientStart, baseLength);
                final String kmerStringEnd = getBaseString(chromosome, posEnd, orientEnd, baseLength);
                final String kmerPosStrand = kmerStringStart + insertSeq + kmerStringEnd;
                final String kmerNegStrand = reverseString(kmerPosStrand);

                writer.write(String.format("%s,%d,%d,%d,%d,%s,%s",
                        chromosome, posStart, orientStart, posEnd, orientEnd, kmerPosStrand, kmerNegStrand));
                writer.newLine();
            }

            closeBufferedWriter(writer);

        }
        catch(IOException exception)
        {
            LOGGER.error("Failed to read kataegis CSV file({})", mKmerInputFile);
        }
    }

    private final String getBaseString(final String chromosome, long position, int orientation, int length)
    {
        if(orientation == 1)
            return mRefGenome.getBaseString(chromosome, position - length, position);
        else
            return mRefGenome.getBaseString(chromosome, position, position + length);
    }

    private final String reverseString(final String str)
    {
        String reverse = "";

        for(int i = str.length() - 1; i >= 0; --i)
        {
            reverse += str.charAt(i);
        }

        return reverse;
    }

    private static final String KMER_INPUT_FILE = "kmer_input_file";

    public static void main(@NotNull final String[] args) throws ParseException
    {
        final Options options = new Options();
        options.addOption(DATA_OUTPUT_DIR, true, "Output directory");
        options.addOption(GENE_TRANSCRIPTS_DIR, true, "Ensembl gene transcript data cache directory");
        options.addOption(KMER_INPUT_FILE, true, "File specifying locations to produce K-mers for");
        options.addOption(REF_GENOME_FILE, true, "Ref genome file");

        final CommandLineParser parser = new DefaultParser();
        final CommandLine cmd = parser.parse(options, args);

        Configurator.setRootLevel(Level.DEBUG);

        final String outputDir = formOutputPath(cmd.getOptionValue(DATA_OUTPUT_DIR));
        final String kmerInputFile = formOutputPath(cmd.getOptionValue(KMER_INPUT_FILE));
        final String refGenomeFile = formOutputPath(cmd.getOptionValue(REF_GENOME_FILE));

        KmerGenerator kmerGenerator = new KmerGenerator(refGenomeFile, kmerInputFile, outputDir);
        kmerGenerator.generateKmerData();
    }


}
