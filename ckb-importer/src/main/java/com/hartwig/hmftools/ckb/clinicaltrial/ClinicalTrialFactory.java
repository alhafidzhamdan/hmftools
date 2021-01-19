package com.hartwig.hmftools.ckb.clinicaltrial;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import com.hartwig.hmftools.common.utils.json.JsonDatamodelChecker;
import com.hartwig.hmftools.common.utils.json.JsonFunctions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ClinicalTrialFactory {

    private static final Logger LOGGER = LogManager.getLogger(ClinicalTrialFactory.class);

    private ClinicalTrialFactory() {

    }

    @NotNull
    public static List<ClinicalTrial> readingClinicalTrial(@NotNull String clinicalTrialDir) throws IOException {
        LOGGER.info("Start reading clinical trials");

        List<ClinicalTrial> clinicalTrials = Lists.newArrayList();
        File[] filesClinicalTrials = new File(clinicalTrialDir).listFiles();

        if (filesClinicalTrials != null) {
            LOGGER.info("The total files in the clinical trial dir is {}", filesClinicalTrials.length);

            for (File clinicalTrial : filesClinicalTrials) {
                JsonParser parser = new JsonParser();
                JsonReader reader = new JsonReader(new FileReader(clinicalTrial));
                reader.setLenient(true);

                while (reader.peek() != JsonToken.END_DOCUMENT) {
                    JsonObject clinicalTrialsEntryObject = parser.parse(reader).getAsJsonObject();
                    JsonDatamodelChecker clinicalTrailChecker = ClinicalTrialDataModelChecker.clinicalTrialObjectChecker();
                    clinicalTrailChecker.check(clinicalTrialsEntryObject);

                    clinicalTrials.add(ImmutableClinicalTrial.builder()
                            .nctId(JsonFunctions.string(clinicalTrialsEntryObject, "nctId"))
                            .title(JsonFunctions.string(clinicalTrialsEntryObject, "title"))
                            .phase(JsonFunctions.string(clinicalTrialsEntryObject, "phase"))
                            .recruitment(JsonFunctions.string(clinicalTrialsEntryObject, "recruitment"))
                            .therapies(retrieveClinicalTrialsTherapies(clinicalTrialsEntryObject.getAsJsonArray("therapies")))
                            .ageGroups(JsonFunctions.stringList(clinicalTrialsEntryObject, "ageGroups"))
                            .gender(JsonFunctions.optionalNullableString(clinicalTrialsEntryObject, "gender"))
                            .variantRequirements(JsonFunctions.string(clinicalTrialsEntryObject, "variantRequirements"))
                            .sponsors(JsonFunctions.optionalNullableString(clinicalTrialsEntryObject, "sponsors"))
                            .updateDate(JsonFunctions.string(clinicalTrialsEntryObject, "updateDate"))
                            .indications(retrieveClinicalTrialsIndications(clinicalTrialsEntryObject.getAsJsonArray("indications")))
                            .variantRequirementDetails(retrieveClinicalTrialsVariantRequirementDetails(clinicalTrialsEntryObject.getAsJsonArray(
                                    "variantRequirementDetails")))
                            .clinicalTrialLocations(retrieveClinicalTrialsLocations(clinicalTrialsEntryObject.getAsJsonArray(
                                    "clinicalTrialLocations")))
                            .build());
                }
            }
        }
        LOGGER.info("Finished reading clinical trials");

        return clinicalTrials;
    }

    @NotNull
    private static List<ClinicalTrialVariantRequirementDetail> retrieveClinicalTrialsVariantRequirementDetails(
            @NotNull JsonArray jsonArray) {
        List<ClinicalTrialVariantRequirementDetail> variantRequirementDetails = Lists.newArrayList();
        JsonDatamodelChecker clinicalTrailVariantRequirementDetailChecker =
                ClinicalTrialDataModelChecker.clinicalTrialVariantRequirementDetailsObjectChecker();

        for (JsonElement variantDetail : jsonArray) {
            JsonObject variantDetailObject = variantDetail.getAsJsonObject();
            clinicalTrailVariantRequirementDetailChecker.check(variantDetailObject);

            variantRequirementDetails.add(ImmutableClinicalTrialVariantRequirementDetail.builder()
                    .molecularProfile(retrieveClinicalTrialsMolecularProfile(variantDetailObject.getAsJsonObject("molecularProfile")))
                    .requirementType(JsonFunctions.string(variantDetailObject, "requirementType"))
                    .build());
        }
        return variantRequirementDetails;
    }

    @NotNull
    private static ClinicalTrialMolecularProfile retrieveClinicalTrialsMolecularProfile(@NotNull JsonObject jsonObject) {
        JsonDatamodelChecker clinicalTrailMolecularProfileChecker =
                ClinicalTrialDataModelChecker.clinicalTrialMolecularProfileObjectChecker();
        clinicalTrailMolecularProfileChecker.check(jsonObject);
        return ImmutableClinicalTrialMolecularProfile.builder()
                .id(JsonFunctions.nullableString(jsonObject, "id"))
                .profileName(JsonFunctions.string(jsonObject, "profileName"))
                .build();
    }

    @NotNull
    private static List<ClinicalTrialTherapy> retrieveClinicalTrialsTherapies(@NotNull JsonArray jsonArray) {
        List<ClinicalTrialTherapy> therapies = Lists.newArrayList();
        JsonDatamodelChecker clinicalTrailTherapiesChecker = ClinicalTrialDataModelChecker.clinicalTrialTherapiesObjectChecker();
        for (JsonElement therapy : jsonArray) {
            JsonObject therapyObject = therapy.getAsJsonObject();
            clinicalTrailTherapiesChecker.check(therapyObject);

            therapies.add(ImmutableClinicalTrialTherapy.builder()
                    .id(JsonFunctions.string(therapyObject, "id"))
                    .therapyName(JsonFunctions.string(therapyObject, "therapyName"))
                    .synonyms(JsonFunctions.optionalNullableString(therapyObject, "synonyms"))
                    .build());
        }
        return therapies;
    }

    @NotNull
    private static List<ClinicalTrialIndication> retrieveClinicalTrialsIndications(@NotNull JsonArray jsonArray) {
        List<ClinicalTrialIndication> indications = Lists.newArrayList();
        JsonDatamodelChecker clinicalTrailIndicationsChecker = ClinicalTrialDataModelChecker.clinicalTrialIndicationsObjectChecker();

        for (JsonElement indication : jsonArray) {
            JsonObject indicationObject = indication.getAsJsonObject();
            clinicalTrailIndicationsChecker.check(indicationObject);
            indications.add(ImmutableClinicalTrialIndication.builder()
                    .id(JsonFunctions.string(indicationObject, "id"))
                    .name(JsonFunctions.string(indicationObject, "name"))
                    .source(JsonFunctions.string(indicationObject, "source"))
                    .build());
        }
        return indications;
    }

    @NotNull
    private static List<ClinicalTrialLocation> retrieveClinicalTrialsLocations(@NotNull JsonArray jsonArray) {
        List<ClinicalTrialLocation> locations = Lists.newArrayList();
        JsonDatamodelChecker clinicalTrailLocationsChecker = ClinicalTrialDataModelChecker.clinicalTrialLocationsObjectChecker();

        for (JsonElement location : jsonArray) {
            JsonObject locationObject = location.getAsJsonObject();
            clinicalTrailLocationsChecker.check(locationObject);
            locations.add(ImmutableClinicalTrialLocation.builder()
                    .nctId(JsonFunctions.string(locationObject, "nctId"))
                    .facility(JsonFunctions.optionalNullableString(locationObject, "facility"))
                    .city(JsonFunctions.string(locationObject, "city"))
                    .country(JsonFunctions.string(locationObject, "country"))
                    .status(JsonFunctions.optionalNullableString(locationObject, "status"))
                    .state(JsonFunctions.optionalNullableString(locationObject, "state"))
                    .zip(JsonFunctions.optionalNullableString(locationObject, "zip"))
                    .clinicalTrialContacts(retrieveClinicalTrialsContact(locationObject))
                    .build());
        }
        return locations;
    }

    @NotNull
    private static List<ClinicalTrialContact> retrieveClinicalTrialsContact(@NotNull JsonObject jsonObject) {
        List<ClinicalTrialContact> contacts = Lists.newArrayList();

        JsonDatamodelChecker clinicalTrailContactChecker = ClinicalTrialDataModelChecker.clinicalTrialContactObjectChecker();
        JsonArray arrayContact = jsonObject.getAsJsonArray("clinicalTrialContacts");

        for (JsonElement contact : arrayContact) {
            JsonObject contactObject = contact.getAsJsonObject();
            clinicalTrailContactChecker.check(contactObject);
            contacts.add(ImmutableClinicalTrialContact.builder()
                    .name(JsonFunctions.optionalNullableString(contactObject, "name"))
                    .email(JsonFunctions.optionalNullableString(contactObject, "email"))
                    .phone(JsonFunctions.optionalNullableString(contactObject, "phone"))
                    .phoneExt(JsonFunctions.optionalNullableString(contactObject, "phoneExt"))
                    .role(JsonFunctions.string(contactObject, "role"))
                    .build());

        }
        return contacts;

    }
}