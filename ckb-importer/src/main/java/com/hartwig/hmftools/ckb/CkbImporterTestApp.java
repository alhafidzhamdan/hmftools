package com.hartwig.hmftools.ckb;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import com.hartwig.hmftools.ckb.datamodel.CkbEntry;
import com.hartwig.hmftools.ckb.datamodel.ImmutableCkbEntry;
import com.hartwig.hmftools.ckb.datamodel.clinicaltrial.ClinicalTrial;
import com.hartwig.hmftools.ckb.reader.clinicaltrial.ClinicalTrialFactory;
import com.hartwig.hmftools.ckb.datamodel.drugclass.DrugClass;
import com.hartwig.hmftools.ckb.reader.drugclass.DrugClassFactory;
import com.hartwig.hmftools.ckb.datamodel.drug.Drug;
import com.hartwig.hmftools.ckb.reader.drug.DrugFactory;
import com.hartwig.hmftools.ckb.datamodel.gene.Gene;
import com.hartwig.hmftools.ckb.reader.gene.GeneFactory;
import com.hartwig.hmftools.ckb.datamodel.globaltherapyapprovalstatus.GlobalTherapyApprovalStatus;
import com.hartwig.hmftools.ckb.reader.globaltherapyapprovalstatus.GlobalTherapyApprovalStatusFactory;
import com.hartwig.hmftools.ckb.datamodel.indication.Indication;
import com.hartwig.hmftools.ckb.reader.indication.IndicationFactory;
import com.hartwig.hmftools.ckb.datamodel.molecularprofile.MolecularProfile;
import com.hartwig.hmftools.ckb.reader.molecularprofile.MolecularprofileFactory;
import com.hartwig.hmftools.ckb.datamodel.reference.Reference;
import com.hartwig.hmftools.ckb.reader.reference.ReferenceFactory;
import com.hartwig.hmftools.ckb.datamodel.therapy.Therapy;
import com.hartwig.hmftools.ckb.reader.therapy.TherapyFactory;
import com.hartwig.hmftools.ckb.datamodel.treatmentapproach.TreatmentApproach;
import com.hartwig.hmftools.ckb.reader.treatmentapproch.TreatmentApproachFactory;
import com.hartwig.hmftools.ckb.datamodel.variant.Variant;
import com.hartwig.hmftools.ckb.reader.variant.VariantFactory;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.jetbrains.annotations.NotNull;

public class CkbImporterTestApp {

    private static final Logger LOGGER = LogManager.getLogger(CkbImporterTestApp.class);

    private static final String CLINICAL_TRIALS = "clinicalTrials";
    private static final String DRUG_CLASSES = "drugclasses";
    private static final String DRUGS = "drugs";
    private static final String GENES = "genes";
    private static final String GLOBAL_THERAPY_APPROVAL_STATUSES = "globalTherapyApprovalStatuses";
    private static final String INDICATIONS = "indications";
    private static final String MOLECULAR_PROFILES = "molecularProfiles";
    private static final String REFERENCES = "references";
    private static final String THERAPIES = "therapies";
    private static final String TREATMENT_APPROACHES = "treatmentApproaches";
    private static final String VARIANTS = "variants";

    public static void main(String[] args) throws IOException {
        Configurator.setRootLevel(Level.DEBUG);

        String hostname = InetAddress.getLocalHost().getHostName();
        LOGGER.debug("Running on '{}'", hostname);

        String ckbPath;

        if (hostname.toLowerCase().contains("datastore")) {
            ckbPath = "/data/common/dbs/ckb/";
        } else {
            ckbPath = System.getProperty("user.home") + "/hmf/projects/serve/ckb/";
        }

        CkbEntry ckbEntry = readJsonData(ckbPath);
        writingDataToDatabase(ckbEntry);

        LOGGER.info("Complete!");

    }

    @NotNull
    private static CkbEntry readJsonData(@NotNull String ckbPath) throws IOException {

        List<ClinicalTrial> clinicalTrials = ClinicalTrialFactory.readingClinicalTrial(ckbPath + CLINICAL_TRIALS);
        List<Drug> drugs = DrugFactory.readingDrugs(ckbPath + DRUGS);
        List<DrugClass> drugClasses = DrugClassFactory.readingDrugClasses(ckbPath + DRUG_CLASSES);
        List<Gene> genes = GeneFactory.readingGenes(ckbPath + GENES);
        List<GlobalTherapyApprovalStatus> globalTherapyApprovalStatuses =
                GlobalTherapyApprovalStatusFactory.readingGlobalTherapyApprovalStatus(ckbPath + GLOBAL_THERAPY_APPROVAL_STATUSES);
        List<Indication> indications = IndicationFactory.readingIndication(ckbPath + INDICATIONS);
        List<MolecularProfile> molecularProfiles = MolecularprofileFactory.readingMolecularprofile(ckbPath + MOLECULAR_PROFILES);
        List<Reference> references = ReferenceFactory.readingReference(ckbPath + REFERENCES);
        List<Therapy> therapies = TherapyFactory.readingTherapy(ckbPath + THERAPIES);
        List<TreatmentApproach> treatmentApproaches = TreatmentApproachFactory.readingTreatmentApproch(ckbPath + TREATMENT_APPROACHES);
        List<Variant> variants = VariantFactory.readingVariant(ckbPath + VARIANTS);

        return ImmutableCkbEntry.builder()
                .clinicalTrial(clinicalTrials)
                .drug(drugs)
                .drugClass(drugClasses)
                .gene(genes)
                .globalTherapyApprovalStatus(globalTherapyApprovalStatuses)
                .indication(indications)
                .molecularProfile(molecularProfiles)
                .reference(references)
                .therapy(therapies)
                .treatmentApproach(treatmentApproaches)
                .variant(variants)
                .build();
    }

    private static void writingDataToDatabase(@NotNull CkbEntry ckbEntry) {
        LOGGER.info("ClinicalTrial {}", ckbEntry.clinicalTrial().get(0));
        LOGGER.info("Drug {}", ckbEntry.drug().get(0));
        LOGGER.info("DrugClass {}", ckbEntry.drugClass().get(0));
        LOGGER.info("Gene {}", ckbEntry.gene().get(0));
        LOGGER.info("GlobalTherapyApprovalStatus {}", ckbEntry.globalTherapyApprovalStatus().get(0));
        LOGGER.info("Indication {}", ckbEntry.indication().get(0));
        LOGGER.info("MolecularProfile {}", ckbEntry.molecularProfile().get(0));
        LOGGER.info("Reference {}", ckbEntry.reference().get(0));
        LOGGER.info("Therapy {}", ckbEntry.therapy().get(0));
        LOGGER.info("TreatmentApproachInfo {}", ckbEntry.treatmentApproach().get(0));
        LOGGER.info("Variant {}", ckbEntry.variant().get(0));
    }

}
