package com.hartwig.hmftools.common.doid;

import java.util.Map;

import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.utils.json.JsonDatamodelChecker;

import org.jetbrains.annotations.NotNull;

final class DoidDatamodelCheckerFactory {

    @NotNull
    static JsonDatamodelChecker doidEntryChecker() {
        Map<String, Boolean> map = Maps.newHashMap();
        map.put("graphs", true);

        return new JsonDatamodelChecker("DoidEntry", map);
    }

    @NotNull
    //TODO
    static JsonDatamodelChecker doidEntryGraphChecker() {
        Map<String, Boolean> map = Maps.newHashMap();
        map.put("nodes", true);
        map.put("edges", false); //TODO
        map.put("id", true); //TODO
        map.put("meta", true); //TODO
        map.put("equivalentNodesSets", true); //TODO
        map.put("logicalDefinitionAxioms", true); //TODO
        map.put("domainRangeAxioms", true); //TODO
        map.put("propertyChainAxioms", true); //TODO

        return new JsonDatamodelChecker("DoidEntry", map);
    }

    @NotNull
    static JsonDatamodelChecker doidEntryNodesChecker() {
        Map<String, Boolean> map = Maps.newHashMap();
        map.put("type", false);
        map.put("lbl", false);
        map.put("id", true);
        map.put("meta", false);

        return new JsonDatamodelChecker("DoidEntry", map);
    }

    @NotNull
    static JsonDatamodelChecker doidMetadataChecker() {
        Map<String, Boolean> map = Maps.newHashMap();
        map.put("xrefs", false);
        map.put("synonyms", false);
        map.put("basicPropertyValues", false);
        map.put("definition", false);
        map.put("subsets", false);


        return new JsonDatamodelChecker("DoidMetadata", map);
    }

    @NotNull
    static JsonDatamodelChecker doidMetadataXrefChecker() {
        Map<String, Boolean> map = Maps.newHashMap();
        map.put("val", true);

        return new JsonDatamodelChecker("DoidMetadataXref", map);
    }

    @NotNull

    static JsonDatamodelChecker doidSynonymsChecker() {
        Map<String, Boolean> map = Maps.newHashMap();
        map.put("pred", true);
        map.put("val", true);
        map.put("xrefs", true);
        return new JsonDatamodelChecker("DoidSynonyms", map);
    }

    @NotNull
    static JsonDatamodelChecker doidBasicPropertyValuesChecker() {
        Map<String, Boolean> map = Maps.newHashMap();
        map.put("pred", true);
        map.put("val", true);
        return new JsonDatamodelChecker("DoidBasicPropertyValues", map);
    }

    @NotNull
    static JsonDatamodelChecker doidDefinitionChecker() {
        Map<String, Boolean> map = Maps.newHashMap();
        map.put("xrefs", true);
        map.put("val", true);
        return new JsonDatamodelChecker("DoidDefinition", map);
    }
}
