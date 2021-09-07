package com.hartwig.hmftools.patientdb.clinical.readers.cpct;

import java.time.LocalDate;
import java.util.Map;

import com.hartwig.hmftools.patientdb.clinical.curators.PrimaryTumorCurator;
import com.hartwig.hmftools.patientdb.clinical.datamodel.BaselineData;
import com.hartwig.hmftools.patientdb.clinical.datamodel.ImmutableBaselineData;
import com.hartwig.hmftools.patientdb.clinical.datamodel.ImmutableCuratedPrimaryTumor;
import com.hartwig.hmftools.patientdb.clinical.ecrf.datamodel.EcrfForm;
import com.hartwig.hmftools.patientdb.clinical.ecrf.datamodel.EcrfItemGroup;
import com.hartwig.hmftools.patientdb.clinical.ecrf.datamodel.EcrfPatient;
import com.hartwig.hmftools.patientdb.clinical.ecrf.datamodel.EcrfStudyEvent;
import com.hartwig.hmftools.patientdb.clinical.ecrf.formstatus.FormStatus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class BaselineReader {

    private static final Logger LOGGER = LogManager.getLogger(BaselineReader.class);

    private static final String STUDY_BASELINE = "SE.BASELINE";
    private static final String STUDY_ENDSTUDY = "SE.ENDSTUDY";

    private static final String FORM_DEMOGRAPHY = "FRM.DEMOGRAPHY";
    private static final String FORM_INFORMED_CONSENT = "FRM.INFORMEDCONSENT";
    private static final String FORM_CARCINOMA = "FRM.CARCINOMA";
    private static final String FORM_ELIGIBILITY = "FRM.ELIGIBILITY";
    private static final String FORM_SELCRIT = "FRM.SELCRIT";
    private static final String FORM_DEATH = "FRM.DEATH";

    private static final String ITEMGROUP_DEMOGRAPHY = "GRP.DEMOGRAPHY.DEMOGRAPHY";
    private static final String ITEMGROUP_INFORMED_CONSENT = "GRP.INFORMEDCONSENT.INFORMEDCONSENT";
    private static final String ITEMGROUP_CARCINOMA = "GRP.CARCINOMA.CARCINOMA";
    private static final String ITEMGROUP_ELIGIBILITY = "GRP.ELIGIBILITY.ELIGIBILITY";
    private static final String ITEMGROUP_SELCRIT = "GRP.SELCRIT.SELCRIT";
    private static final String ITEMGROUP_DEATH = "GRP.DEATH.DEATH";

    private static final String FIELD_GENDER = "FLD.DEMOGRAPHY.SEX";
    private static final String FIELD_INFORMED_CONSENT_DATE = "FLD.INFORMEDCONSENT.ICDTC";
    private static final String FIELD_REGISTRATION_DATE1 = "FLD.ELIGIBILITY.REGDTC";
    private static final String FIELD_REGISTRATION_DATE2 = "FLD.SELCRIT.NREGDTC";
    private static final String FIELD_BIRTH_YEAR1 = "FLD.SELCRIT.NBIRTHYEAR";
    private static final String FIELD_BIRTH_YEAR2 = "FLD.ELIGIBILITY.BIRTHYEAR";
    private static final String FIELD_BIRTH_YEAR3 = "FLD.ELIGIBILITY.BIRTHDTCES";

    private static final String FIELD_PRIMARY_TUMOR_LOCATION = "FLD.CARCINOMA.PTUMLOC";
    private static final String FIELD_PRIMARY_TUMOR_LOCATION_OTHER = "FLD.CARCINOMA.PTUMLOCS";
    private static final String FIELD_PRIMARY_TUMOR_LOCATION_SELCRIT = "FLD.SELCRIT.SELPTM";

    private static final String FIELD_DEATH_DATE = "FLD.DEATH.DDEATHDTC";

    @NotNull
    private final PrimaryTumorCurator primaryTumorCurator;
    @NotNull
    private final Map<Integer, String> hospitals;

    public BaselineReader(@NotNull final PrimaryTumorCurator primaryTumorCurator, @NotNull final Map<Integer, String> hospitals) {
        this.primaryTumorCurator = primaryTumorCurator;
        this.hospitals = hospitals;
    }

    @NotNull
    BaselineData read(@NotNull EcrfPatient patient) {
        ImmutableBaselineData.Builder baselineBuilder = ImmutableBaselineData.builder()
                .demographyStatus(FormStatus.undefined())
                .primaryTumorStatus(FormStatus.undefined())
                .curatedPrimaryTumor(ImmutableCuratedPrimaryTumor.builder().searchTerm(Strings.EMPTY).isOverridden(false).build())
                .eligibilityStatus(FormStatus.undefined())
                .selectionCriteriaStatus(FormStatus.undefined())
                .informedConsentStatus(FormStatus.undefined())
                .deathStatus(FormStatus.undefined())
                .hospital(lookupHospital(patient, hospitals));

        for (EcrfStudyEvent studyEvent : patient.studyEventsPerOID(STUDY_BASELINE)) {
            setDemographyData(baselineBuilder, studyEvent);
            setPrimaryTumorData(patient.patientId(), baselineBuilder, studyEvent);
            setRegistrationAndBirthData(baselineBuilder, studyEvent);
            setInformedConsent(baselineBuilder, studyEvent);
        }

        setDeathData(baselineBuilder, patient);
        return baselineBuilder.build();
    }

    @Nullable
    private static String lookupHospital(@NotNull EcrfPatient patient, @NotNull Map<Integer, String> hospitals) {
        if (patient.patientId().length() >= 8) {
            Integer hospitalCode = Integer.parseInt(patient.patientId().substring(6, 8));
            String hospital = hospitals.get(hospitalCode);
            if (hospital == null) {
                LOGGER.warn("Could not find entry for hospital with code {}", hospitalCode);
            }
            return hospital;
        } else {
            LOGGER.warn("Could not extract hospital code for patient: {}", patient.patientId());
            return null;
        }
    }

    private void setDemographyData(@NotNull ImmutableBaselineData.Builder builder, @NotNull EcrfStudyEvent studyEvent) {
        for (EcrfForm demographyForm : studyEvent.nonEmptyFormsPerOID(FORM_DEMOGRAPHY)) {
            for (EcrfItemGroup demographyItemGroup : demographyForm.nonEmptyItemGroupsPerOID(ITEMGROUP_DEMOGRAPHY)) {
                builder.gender(demographyItemGroup.readItemString(FIELD_GENDER));
                builder.demographyStatus(demographyForm.status());
            }
        }
    }

    private void setPrimaryTumorData(@NotNull String patientIdentifier, @NotNull ImmutableBaselineData.Builder builder,
            @NotNull EcrfStudyEvent studyEvent) {
        String primaryTumorLocationSelcritForm = null;
        FormStatus primaryTumorLocationSelcritStatus = null;
        for (EcrfForm selcritForm : studyEvent.nonEmptyFormsPerOID(FORM_SELCRIT)) {
            for (EcrfItemGroup selcritItemGroup : selcritForm.nonEmptyItemGroupsPerOID(ITEMGROUP_SELCRIT)) {
                primaryTumorLocationSelcritForm = selcritItemGroup.readItemString(FIELD_PRIMARY_TUMOR_LOCATION_SELCRIT);
                primaryTumorLocationSelcritStatus = selcritForm.status();
            }
        }

        String primaryTumorLocationCarcinomaForm = null;
        FormStatus primaryTumorLocationCarcinomaStatus = null;
        for (EcrfForm carcinomaForm : studyEvent.nonEmptyFormsPerOID(FORM_CARCINOMA)) {
            for (EcrfItemGroup carcinomaItemGroup : carcinomaForm.nonEmptyItemGroupsPerOID(ITEMGROUP_CARCINOMA)) {
                String primaryTumorLocationCarcinoma = carcinomaItemGroup.readItemString(FIELD_PRIMARY_TUMOR_LOCATION);
                String primaryTumorLocationOther = carcinomaItemGroup.readItemString(FIELD_PRIMARY_TUMOR_LOCATION_OTHER);
                primaryTumorLocationCarcinomaForm =
                        determineCarcinomaPrimaryTumorLocation(primaryTumorLocationCarcinoma, primaryTumorLocationOther);
                primaryTumorLocationCarcinomaStatus = carcinomaForm.status();
            }
        }

        // We prefer carcinoma form over sel crit form. See also DEV-540
        boolean useCarcinomaForm = primaryTumorLocationCarcinomaForm != null && !primaryTumorLocationCarcinomaForm.isEmpty();
        FormStatus primaryTumorFormStatus = useCarcinomaForm ? primaryTumorLocationCarcinomaStatus : primaryTumorLocationSelcritStatus;
        String finalPrimaryTumor = useCarcinomaForm ? primaryTumorLocationCarcinomaForm : primaryTumorLocationSelcritForm;

        builder.curatedPrimaryTumor(primaryTumorCurator.search(patientIdentifier, finalPrimaryTumor));
        builder.primaryTumorStatus(primaryTumorFormStatus != null ? primaryTumorFormStatus : FormStatus.undefined());
    }

    @Nullable
    private static String determineCarcinomaPrimaryTumorLocation(@Nullable String primaryTumorLocationCarcinoma,
            @Nullable String primaryTumorLocationOther) {
        // We always read additional info if there is any, see also DEV-1713
        if (primaryTumorLocationCarcinoma == null) {
            return primaryTumorLocationOther;
        } else if (primaryTumorLocationCarcinoma != null && primaryTumorLocationOther != null && !primaryTumorLocationOther.isEmpty()) {
            return primaryTumorLocationCarcinoma + " + " + primaryTumorLocationOther;
        } else {
            return primaryTumorLocationCarcinoma;
        }
    }

    private static void setRegistrationAndBirthData(@NotNull ImmutableBaselineData.Builder builder, @NotNull EcrfStudyEvent studyEvent) {
        LocalDate registrationDate1 = null;
        LocalDate registrationDate2 = null;
        String birthYear1 = null;
        String birthYear2 = null;
        LocalDate birthYear3 = null;

        for (EcrfForm eligibilityForm : studyEvent.nonEmptyFormsPerOID(FORM_ELIGIBILITY)) {
            for (EcrfItemGroup eligibilityItemGroup : eligibilityForm.nonEmptyItemGroupsPerOID(ITEMGROUP_ELIGIBILITY)) {
                registrationDate1 = eligibilityItemGroup.readItemDate(FIELD_REGISTRATION_DATE1);
                birthYear2 = eligibilityItemGroup.readItemString(FIELD_BIRTH_YEAR2);
                birthYear3 = eligibilityItemGroup.readItemDate(FIELD_BIRTH_YEAR3);
                builder.eligibilityStatus(eligibilityForm.status());
            }
        }

        for (EcrfForm selcritForm : studyEvent.nonEmptyFormsPerOID(FORM_SELCRIT)) {
            for (EcrfItemGroup selcritItemGroup : selcritForm.nonEmptyItemGroupsPerOID(ITEMGROUP_SELCRIT)) {
                birthYear1 = selcritItemGroup.readItemString(FIELD_BIRTH_YEAR1);
                if (registrationDate1 == null) {
                    registrationDate2 = selcritItemGroup.readItemDate(FIELD_REGISTRATION_DATE2);
                    builder.selectionCriteriaStatus(selcritForm.status());
                }
            }
        }

        LocalDate registrationDate = registrationDate2 == null ? registrationDate1 : registrationDate2;
        Integer birthYear = determineBirthYear(birthYear1, birthYear2, birthYear3);
        builder.registrationDate(registrationDate);
        builder.birthYear(birthYear);
    }

    @Nullable
    private static Integer determineBirthYear(@Nullable String birthYear1, @Nullable String birthYear2, @Nullable LocalDate birthYear3) {
        if (birthYear1 != null) {
            return Integer.parseInt(birthYear1);
        }
        if (birthYear2 != null) {
            return Integer.parseInt(birthYear2);
        }
        if (birthYear3 != null) {
            return birthYear3.getYear();
        }
        return null;
    }

    private void setInformedConsent(@NotNull ImmutableBaselineData.Builder builder, @NotNull EcrfStudyEvent studyEvent) {
        for (EcrfForm informedConsentForm : studyEvent.nonEmptyFormsPerOID(FORM_INFORMED_CONSENT)) {
            for (EcrfItemGroup informedConsentItemGroup : informedConsentForm.nonEmptyItemGroupsPerOID(ITEMGROUP_INFORMED_CONSENT)) {
                builder.informedConsentDate(informedConsentItemGroup.readItemDate(FIELD_INFORMED_CONSENT_DATE));
                builder.informedConsentStatus(informedConsentForm.status());
            }
        }
    }

    private static void setDeathData(@NotNull ImmutableBaselineData.Builder builder, @NotNull EcrfPatient patient) {
        for (EcrfStudyEvent endStudyEvent : patient.studyEventsPerOID(STUDY_ENDSTUDY)) {
            for (EcrfForm deathForm : endStudyEvent.nonEmptyFormsPerOID(FORM_DEATH)) {
                for (EcrfItemGroup deathItemGroup : deathForm.nonEmptyItemGroupsPerOID(ITEMGROUP_DEATH)) {
                    builder.deathDate(deathItemGroup.readItemDate(FIELD_DEATH_DATE));
                    builder.deathStatus(deathForm.status());
                }
            }
        }
    }
}
