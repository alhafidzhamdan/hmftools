package com.hartwig.hmftools.virusinterpreter;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.hartwig.hmftools.common.virus.AnnotatedVirus;
import com.hartwig.hmftools.common.virus.AnnotatedVirusFile;
import com.hartwig.hmftools.common.virus.VirusBreakend;
import com.hartwig.hmftools.common.virus.VirusBreakendFile;
import com.hartwig.hmftools.virusinterpreter.algo.VirusBlacklistFile;
import com.hartwig.hmftools.virusinterpreter.algo.VirusBlacklistModel;
import com.hartwig.hmftools.virusinterpreter.algo.VirusWhitelistFile;
import com.hartwig.hmftools.virusinterpreter.algo.VirusWhitelistModel;
import com.hartwig.hmftools.virusinterpreter.taxonomy.TaxonomyDb;
import com.hartwig.hmftools.virusinterpreter.taxonomy.TaxonomyDbFile;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VirusInterpreterApplication {

    private static final Logger LOGGER = LogManager.getLogger(VirusInterpreterApplication.class);

    private static final String VERSION = VirusInterpreterApplication.class.getPackage().getImplementationVersion();

    public static void main(String[] args) throws IOException {
        LOGGER.info("Running Virus Interpreter v{}", VERSION);

        Options options = VirusInterpreterConfig.createOptions();

        VirusInterpreterConfig config = null;
        try {
            config = VirusInterpreterConfig.createConfig(new DefaultParser().parse(options, args));
        } catch (ParseException exception) {
            LOGGER.warn(exception);
            new HelpFormatter().printHelp("VirusInterpreter", options);
            System.exit(1);
        }

        LOGGER.info("Loading taxonomy db from {}", config.taxonomyDbTsv());
        TaxonomyDb taxonomyDb = TaxonomyDbFile.loadFromTsv(config.taxonomyDbTsv());

        LOGGER.info("Building virus interpretation model from {}", config.virusWhitelistTsv());
        VirusWhitelistModel virusWhitelistModel = VirusWhitelistFile.buildFromTsv(config.virusWhitelistTsv());

        LOGGER.info("Building virus blacklist model from {}", config.virusBlacklistTsv());
        VirusBlacklistModel virusBlacklistModel = VirusBlacklistFile.buildFromTsv(config.virusBlacklistTsv());

        LOGGER.info("Loading virus breakends from {}", new File(config.virusBreakendTsv()).getParent());
        List<VirusBreakend> virusBreakends = VirusBreakendFile.read(config.virusBreakendTsv());
        LOGGER.info(" Loaded {} virus breakends from {}", virusBreakends.size(), config.virusBreakendTsv());

        VirusInterpreterAlgo algo = new VirusInterpreterAlgo(taxonomyDb, virusWhitelistModel, virusBlacklistModel);

        List<AnnotatedVirus> annotatedViruses =
                algo.analyze(virusBreakends, config.purplePurityTsv(), config.purpleQcFile(), config.tumorSampleWGSMetricsFile());
        LOGGER.info("Interpreter classified {} viruses as reportable", annotatedViruses.stream().filter(x -> x.reported()).count());

        String annotatedVirusTsv = AnnotatedVirusFile.generateFileName(config.outputDir(), config.sampleId());
        AnnotatedVirusFile.write(annotatedVirusTsv, annotatedViruses);
        LOGGER.info("Written {} annotated viruses to {}", annotatedViruses.size(), annotatedVirusTsv);
    }
}
