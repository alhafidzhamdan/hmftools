package com.hartwig.hmftools.neo.utils;

import static com.hartwig.hmftools.common.utils.ConfigUtils.addLoggingOptions;
import static com.hartwig.hmftools.common.utils.ConfigUtils.setLogLevel;
import static com.hartwig.hmftools.common.utils.FileWriterUtils.OUTPUT_DIR;
import static com.hartwig.hmftools.neo.NeoCommon.NE_LOGGER;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.stats.AucCalc;
import com.hartwig.hmftools.common.stats.AucData;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.NotNull;

public class AucCalcTester
{
    private static final String DATA_FILE = "data_file";
    private static final String IS_PERCENTILE = "percentile";

    public static void main(@NotNull final String[] args) throws ParseException
    {
        final Options options = new Options();
        options.addOption(DATA_FILE, true, "Input filename");
        options.addOption(IS_PERCENTILE, false, "In percentile or rank form");
        options.addOption(OUTPUT_DIR, true, "Output directory");
        addLoggingOptions(options);

        final CommandLine cmd = createCommandLine(args, options);

        setLogLevel(cmd);

        String dataFile = cmd.getOptionValue(DATA_FILE);
        boolean isPercentile = cmd.hasOption(IS_PERCENTILE);

        try
        {
            List<String> lines = Files.readAllLines(Paths.get(dataFile));

            lines.remove(0);

            List<AucData> aucDataList = Lists.newArrayList();

            for(String line : lines)
            {
                String[] items = line.split(",");
                boolean isPositive = Boolean.parseBoolean(items[0]);
                double value = Double.parseDouble(items[1]);

                aucDataList.add(new AucData(isPositive, value, isPercentile));
            }

            NE_LOGGER.info("loaded {} AUC items from file({})", aucDataList.size(), dataFile);

            double auc = isPercentile ?
                    AucCalc.calcPercentilesAuc(aucDataList, Level.DEBUG) : AucCalc.calcScoresAuc(aucDataList, Level.DEBUG);

            NE_LOGGER.info(String.format("AUC from %s = %.4f",
                    isPercentile ? "percentiles" : "values", auc));
        }
        catch(IOException e)
        {
            NE_LOGGER.error("file load failed: {}", e.toString());
        }
    }

    @NotNull
    public static CommandLine createCommandLine(@NotNull final String[] args, @NotNull final Options options) throws ParseException
    {
        final CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

}
