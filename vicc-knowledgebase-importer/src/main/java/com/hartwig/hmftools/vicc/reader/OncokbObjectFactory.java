package com.hartwig.hmftools.vicc.reader;

import static com.hartwig.hmftools.vicc.reader.JsonFunctions.nullableString;
import static com.hartwig.hmftools.vicc.reader.JsonFunctions.string;
import static com.hartwig.hmftools.vicc.reader.JsonFunctions.stringList;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hartwig.hmftools.vicc.datamodel.oncokb.ImmutableOncoKbBiological;
import com.hartwig.hmftools.vicc.datamodel.oncokb.ImmutableOncoKbClinical;
import com.hartwig.hmftools.vicc.datamodel.oncokb.ImmutableOncoKbConsequence;
import com.hartwig.hmftools.vicc.datamodel.oncokb.ImmutableOncoKbDrugAbstract;
import com.hartwig.hmftools.vicc.datamodel.oncokb.ImmutableOncokb;
import com.hartwig.hmftools.vicc.datamodel.oncokb.ImmutableOncokbGene;
import com.hartwig.hmftools.vicc.datamodel.oncokb.ImmutableOncokbVariant;
import com.hartwig.hmftools.vicc.datamodel.oncokb.OncoKbBiological;
import com.hartwig.hmftools.vicc.datamodel.oncokb.OncoKbClinical;
import com.hartwig.hmftools.vicc.datamodel.oncokb.OncoKbConsequence;
import com.hartwig.hmftools.vicc.datamodel.oncokb.OncoKbDrugAbstract;
import com.hartwig.hmftools.vicc.datamodel.oncokb.Oncokb;
import com.hartwig.hmftools.vicc.datamodel.oncokb.OncokbGene;
import com.hartwig.hmftools.vicc.datamodel.oncokb.OncokbVariant;

import org.jetbrains.annotations.NotNull;

final class OncokbObjectFactory {

    private OncokbObjectFactory() {
    }

    @NotNull
    static Oncokb create(@NotNull JsonObject oncoKbObject) {
        ViccDatamodelCheckerFactory.oncoKbEntryChecker().check(oncoKbObject);

        if (oncoKbObject.has("biological")) {
            return createOncoKbBiological(oncoKbObject.getAsJsonObject("biological"));
        } else if (oncoKbObject.has("clinical")) {
            return createOncoKbClinical(oncoKbObject.getAsJsonObject("clinical"));
        } else {
            throw new IllegalStateException("OncoKb object neither biological nor clinical: " + oncoKbObject);
        }
    }

    @NotNull
    private static Oncokb createOncoKbBiological(@NotNull JsonObject oncoKbBiologicalObject) {
        return ImmutableOncokb.builder().oncoKbBiological(createBiologicalOncoKb(oncoKbBiologicalObject)).build();
    }

    @NotNull
    private static Oncokb createOncoKbClinical(@NotNull JsonObject oncoKbClinicalObject) {
        return ImmutableOncokb.builder().oncoKbClinical(createClinicalOncoKb(oncoKbClinicalObject)).build();
    }

    @NotNull
    private static OncoKbClinical createClinicalOncoKb(@NotNull JsonObject clinicalObject) {
        ViccDatamodelCheckerFactory.oncoKbClinicalChecker().check(clinicalObject);

        return ImmutableOncoKbClinical.builder()
                .gene(string(clinicalObject, "gene"))
                .entrezGeneId(string(clinicalObject, "Entrez Gene ID"))
                .isoform(string(clinicalObject, "Isoform"))
                .refSeq(string(clinicalObject, "RefSeq"))
                .variant(createVariantOncoKb(clinicalObject.getAsJsonObject("variant")))
                .cancerType(string(clinicalObject, "cancerType"))
                .drug(string(clinicalObject, "drug"))
                .drugPmids(string(clinicalObject, "drugPmids"))
                .drugAbstracts(createDrugsAbstracts(clinicalObject.getAsJsonArray("drugAbstracts")))
                .level(string(clinicalObject, "level"))
                .levelLabel(string(clinicalObject, "level_label"))
                .build();
    }

    @NotNull
    private static List<OncoKbDrugAbstract> createDrugsAbstracts(@NotNull JsonArray drugAbstractArray) {
        List<OncoKbDrugAbstract> drugAbstractList = Lists.newArrayList();
        ViccDatamodelChecker drugAbstractChecker = ViccDatamodelCheckerFactory.oncoKbDrugsAbstractChecker();

        for (JsonElement drugAbstractElement : drugAbstractArray) {
            JsonObject drugAbstractObject = drugAbstractElement.getAsJsonObject();

            drugAbstractChecker.check(drugAbstractObject);
            drugAbstractList.add(ImmutableOncoKbDrugAbstract.builder()
                    .text(string(drugAbstractObject, "text"))
                    .link(string(drugAbstractObject, "link"))
                    .build());
        }

        return drugAbstractList;
    }

    @NotNull
    private static OncoKbBiological createBiologicalOncoKb(@NotNull JsonObject biologicalObject) {
        ViccDatamodelCheckerFactory.oncoKbBiologicalChecker().check(biologicalObject);

        return ImmutableOncoKbBiological.builder()
                .gene(string(biologicalObject, "gene"))
                .entrezGeneId(string(biologicalObject, "Entrez Gene ID"))
                .isoform(string(biologicalObject, "Isoform"))
                .refSeq(string(biologicalObject, "RefSeq"))
                .oncokbVariant(createVariantOncoKb(biologicalObject.getAsJsonObject("variant")))
                .oncogenic(string(biologicalObject, "oncogenic"))
                .mutationEffect(string(biologicalObject, "mutationEffect"))
                .mutationEffectPmids(string(biologicalObject, "mutationEffectPmids"))
                .mutationEffectAbstracts(string(biologicalObject, "mutationEffectAbstracts"))
                .build();
    }

    @NotNull
    private static OncokbVariant createVariantOncoKb(@NotNull JsonObject variantObject) {
        ViccDatamodelCheckerFactory.oncoKbVariantChecker().check(variantObject);

        return ImmutableOncokbVariant.builder()
                .name(string(variantObject, "name"))
                .alteration(string(variantObject, "alteration"))
                .consequence(createConsequenceOncokb(variantObject.getAsJsonObject("consequence")))
                .gene(createGeneOncoKb(variantObject.getAsJsonObject("gene")))
                .proteinStart(string(variantObject, "proteinStart"))
                .proteinEnd(string(variantObject, "proteinEnd"))
                .refResidues(nullableString(variantObject, "refResidues"))
                .variantResidues(nullableString(variantObject, "variantResidues"))
                .build();
    }

    @NotNull
    private static OncoKbConsequence createConsequenceOncokb(@NotNull JsonObject consequenceObject) {
        ViccDatamodelCheckerFactory.oncoKbConsequenceChecker().check(consequenceObject);

        return ImmutableOncoKbConsequence.builder()
                .term(string(consequenceObject, "term"))
                .description(string(consequenceObject, "description"))
                .isGenerallyTruncating(string(consequenceObject, "isGenerallyTruncating"))
                .build();
    }

    @NotNull
    private static OncokbGene createGeneOncoKb(@NotNull JsonObject geneObject) {
        ViccDatamodelCheckerFactory.oncoKbGeneChecker().check(geneObject);

        return ImmutableOncokbGene.builder()
                .hugoSymbol(string(geneObject, "hugoSymbol"))
                .geneAliases(stringList(geneObject, "geneAliases"))
                .name(string(geneObject, "name"))
                .entrezGeneId(string(geneObject, "entrezGeneId"))
                .curatedIsoform(nullableString(geneObject, "curatedIsoform"))
                .curatedRefSeq(nullableString(geneObject, "curatedRefSeq"))
                .oncogene(string(geneObject, "oncogene"))
                .tsg(string(geneObject, "tsg"))
                .build();
    }
}
