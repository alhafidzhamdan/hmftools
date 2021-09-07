package com.hartwig.hmftools.geneutils.ensembl;

import static com.hartwig.hmftools.common.ensemblcache.EnsemblDataCache.ENSEMBL_DATA_DIR;
import static com.hartwig.hmftools.common.utils.ConfigUtils.LOG_DEBUG;
import static com.hartwig.hmftools.common.utils.ConfigUtils.setLogLevel;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.OUTPUT_DIR;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.parseOutputDir;
import static com.hartwig.hmftools.geneutils.common.CommonUtils.GU_LOGGER;
import static com.hartwig.hmftools.geneutils.common.CommonUtils.readQueryString;
import static com.hartwig.hmftools.geneutils.common.CommonUtils.writeRecordsAsTsv;
import static com.hartwig.hmftools.geneutils.ensembl.EnsemblDAO.createEnsemblDbConnection;

import java.io.IOException;
import java.sql.SQLException;

import com.hartwig.hmftools.common.genome.refgenome.RefGenomeVersion;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;

public class RunAdHocQuery
{
    private static final String QUERY_FILE = "query_file";
    private static final String OUTPUT_FILE = "output_file";

    public static void main(String[] args) throws ParseException, IOException, SQLException
    {
        final Options options = createOptions();
        final CommandLine cmd = new DefaultParser().parse(options, args);

        setLogLevel(cmd);

        RefGenomeVersion refGenomeVersion = RefGenomeVersion.from(cmd.getOptionValue(RefGenomeVersion.REF_GENOME_VERSION));
        String outputFile = cmd.getOptionValue(OUTPUT_FILE);

        if(refGenomeVersion == null || outputFile == null)
        {
            GU_LOGGER.error("missing config");
            System.exit(1);
        }

        DSLContext context = createEnsemblDbConnection(cmd);

        if(context == null)
            System.exit(1);

        GU_LOGGER.debug("database connection established");

        String query = readQueryString(cmd.getOptionValue(QUERY_FILE));

        final Result<Record> results = context.fetch(query);

        GU_LOGGER.info("query returned {} entries", results.size());
        writeRecordsAsTsv(outputFile, results);

        GU_LOGGER.info("complete");
    }

    private static Options createOptions()
    {
        final Options options = new Options();
        options.addOption(RefGenomeVersion.REF_GENOME_VERSION, true, "Ref genome version (V37 or V38))");
        EnsemblDAO.addCmdLineArgs(options);
        options.addOption(QUERY_FILE, true, "Ad-hoc query SQL file");
        options.addOption(OUTPUT_FILE, true, "Output file for test query results");
        options.addOption(LOG_DEBUG, false, "Log verbose");
        return options;
    }

}
