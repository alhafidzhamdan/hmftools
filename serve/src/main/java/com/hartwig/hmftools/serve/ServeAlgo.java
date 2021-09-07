package com.hartwig.hmftools.serve;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.ckb.classification.CkbClassificationConfig;
import com.hartwig.hmftools.ckb.datamodel.CkbEntry;
import com.hartwig.hmftools.common.genome.refgenome.RefGenomeVersion;
import com.hartwig.hmftools.common.serve.Knowledgebase;
import com.hartwig.hmftools.common.serve.classification.EventClassifierConfig;
import com.hartwig.hmftools.iclusion.classification.IclusionClassificationConfig;
import com.hartwig.hmftools.iclusion.datamodel.IclusionTrial;
import com.hartwig.hmftools.serve.curation.DoidLookup;
import com.hartwig.hmftools.serve.extraction.ExtractionFunctions;
import com.hartwig.hmftools.serve.extraction.ExtractionResult;
import com.hartwig.hmftools.serve.refgenome.RefGenomeManager;
import com.hartwig.hmftools.serve.sources.ckb.CkbExtractor;
import com.hartwig.hmftools.serve.sources.ckb.CkbExtractorFactory;
import com.hartwig.hmftools.serve.sources.ckb.CkbReader;
import com.hartwig.hmftools.serve.sources.docm.DocmEntry;
import com.hartwig.hmftools.serve.sources.docm.DocmExtractor;
import com.hartwig.hmftools.serve.sources.docm.DocmReader;
import com.hartwig.hmftools.serve.sources.hartwig.HartwigEntry;
import com.hartwig.hmftools.serve.sources.hartwig.HartwigExtractor;
import com.hartwig.hmftools.serve.sources.hartwig.HartwigFileReader;
import com.hartwig.hmftools.serve.sources.iclusion.IclusionExtractor;
import com.hartwig.hmftools.serve.sources.iclusion.IclusionExtractorFactory;
import com.hartwig.hmftools.serve.sources.iclusion.IclusionReader;
import com.hartwig.hmftools.serve.sources.vicc.ViccExtractor;
import com.hartwig.hmftools.serve.sources.vicc.ViccExtractorFactory;
import com.hartwig.hmftools.serve.sources.vicc.ViccReader;
import com.hartwig.hmftools.vicc.annotation.ViccClassificationConfig;
import com.hartwig.hmftools.vicc.datamodel.ViccEntry;
import com.hartwig.hmftools.vicc.datamodel.ViccSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ServeAlgo {

    private static final Logger LOGGER = LogManager.getLogger(ServeAlgo.class);

    @NotNull
    private final RefGenomeManager refGenomeManager;
    @NotNull
    private final DoidLookup missingDoidLookup;

    public ServeAlgo(@NotNull final RefGenomeManager refGenomeManager, @NotNull final DoidLookup missingDoidLookup) {
        this.refGenomeManager = refGenomeManager;
        this.missingDoidLookup = missingDoidLookup;
    }

    @NotNull
    public Map<RefGenomeVersion, ExtractionResult> run(@NotNull ServeConfig config) throws IOException {
        List<ExtractionResult> extractions = Lists.newArrayList();
        if (config.useVicc()) {
            extractions.add(extractViccKnowledge(config.viccJson(), config.viccSources()));
        }

        if (config.useIclusion()) {
            extractions.add(extractIclusionKnowledge(config.iClusionTrialTsv()));
        }

        if (config.useCkb()) {
            extractions.add(extractCkbKnowledge(config.ckbDir(), config.ckbFilterTsv()));
        }

        if (config.useDocm()) {
            extractions.add(extractDocmKnowledge(config.docmTsv()));
        }

        if (config.useHartwigCohort()) {
            extractions.add(extractHartwigCohortKnowledge(config.hartwigCohortTsv(), !config.skipHotspotResolving()));
        }

        if (config.useHartwigCurated()) {
            extractions.add(extractHartwigCuratedKnowledge(config.hartwigCuratedTsv(), !config.skipHotspotResolving()));
        }

        Map<RefGenomeVersion, List<ExtractionResult>> versionedMap = refGenomeManager.makeVersioned(extractions);

        Map<RefGenomeVersion, ExtractionResult> refDependentExtractionMap = Maps.newHashMap();
        for (Map.Entry<RefGenomeVersion, List<ExtractionResult>> entry : versionedMap.entrySet()) {
            refDependentExtractionMap.put(entry.getKey(), ExtractionFunctions.merge(entry.getValue()));
        }

        missingDoidLookup.evaluate();
        refGenomeManager.evaluate();

        return refDependentExtractionMap;
    }

    @NotNull
    private ExtractionResult extractViccKnowledge(@NotNull String viccJson, @NotNull Set<ViccSource> viccSources) throws IOException {
        List<ViccEntry> entries = ViccReader.readAndCurateRelevantEntries(viccJson, viccSources, null);

        EventClassifierConfig config = ViccClassificationConfig.build();
        // Assume all VICC sources share the same ref genome version
        ViccExtractor extractor = ViccExtractorFactory.buildViccExtractor(config,
                refGenomeManager.pickResourceForKnowledgebase(Knowledgebase.VICC_CIVIC),
                missingDoidLookup);

        LOGGER.info("Running VICC knowledge extraction");
        return extractor.extract(entries);
    }

    @NotNull
    private ExtractionResult extractIclusionKnowledge(@NotNull String iClusionTrialTsv) throws IOException {
        List<IclusionTrial> trials = IclusionReader.readAndCurate(iClusionTrialTsv);

        EventClassifierConfig config = IclusionClassificationConfig.build();
        IclusionExtractor extractor = IclusionExtractorFactory.buildIclusionExtractor(config,
                refGenomeManager.pickResourceForKnowledgebase(Knowledgebase.ICLUSION),
                missingDoidLookup);

        LOGGER.info("Running iClusion knowledge extraction");
        return extractor.extract(trials);
    }

    @NotNull
    private ExtractionResult extractCkbKnowledge(@NotNull String ckbDir, @NotNull String ckbFilterTsv) throws IOException {
        List<CkbEntry> ckbEntries = CkbReader.readAndCurate(ckbDir, ckbFilterTsv);

        EventClassifierConfig config = CkbClassificationConfig.build();
        CkbExtractor extractor = CkbExtractorFactory.buildCkbExtractor(config,
                refGenomeManager.pickResourceForKnowledgebase(Knowledgebase.CKB));

        LOGGER.info("Running CKB knowledge extraction");
        return extractor.extract(ckbEntries);
    }

    @NotNull
    private ExtractionResult extractDocmKnowledge(@NotNull String docmTsv) throws IOException {
        List<DocmEntry> entries = DocmReader.readAndCurate(docmTsv);

        DocmExtractor extractor = new DocmExtractor(refGenomeManager.pickResourceForKnowledgebase(Knowledgebase.DOCM).proteinResolver());
        LOGGER.info("Running DoCM knowledge extraction");
        return extractor.extract(entries);
    }

    @NotNull
    private ExtractionResult extractHartwigCohortKnowledge(@NotNull String hartwigCohortTsv, boolean addExplicitHotspots)
            throws IOException {
        LOGGER.info("Reading Hartwig Cohort TSV from '{}'", hartwigCohortTsv);
        List<HartwigEntry> entries = HartwigFileReader.read(hartwigCohortTsv);
        LOGGER.info(" Read {} entries", entries.size());

        HartwigExtractor extractor = new HartwigExtractor(Knowledgebase.HARTWIG_COHORT,
                refGenomeManager.pickResourceForKnowledgebase(Knowledgebase.HARTWIG_COHORT).proteinResolver(),
                addExplicitHotspots);
        LOGGER.info("Running Hartwig Cohort knowledge extraction");
        return extractor.extract(entries);
    }

    @NotNull
    private ExtractionResult extractHartwigCuratedKnowledge(@NotNull String hartwigCuratedTsv, boolean addExplicitHotspots)
            throws IOException {
        LOGGER.info("Reading Hartwig Curated TSV from '{}'", hartwigCuratedTsv);
        List<HartwigEntry> entries = HartwigFileReader.read(hartwigCuratedTsv);
        LOGGER.info(" Read {} entries", entries.size());

        HartwigExtractor extractor = new HartwigExtractor(Knowledgebase.HARTWIG_CURATED,
                refGenomeManager.pickResourceForKnowledgebase(Knowledgebase.HARTWIG_CURATED).proteinResolver(),
                addExplicitHotspots);
        LOGGER.info("Running Hartwig Curated knowledge extraction");
        return extractor.extract(entries);
    }
}
