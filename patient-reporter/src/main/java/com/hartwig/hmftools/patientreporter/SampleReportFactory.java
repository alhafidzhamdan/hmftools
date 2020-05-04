package com.hartwig.hmftools.patientreporter;

import java.time.LocalDate;

import com.hartwig.hmftools.common.ecrf.projections.PatientTumorLocation;
import com.hartwig.hmftools.common.hospital.HospitalModel;
import com.hartwig.hmftools.common.hospital.HospitalQuery;
import com.hartwig.hmftools.common.lims.Lims;
import com.hartwig.hmftools.common.lims.LimsStudy;
import com.hartwig.hmftools.common.lims.LimsWide;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SampleReportFactory {

    private static final Logger LOGGER = LogManager.getLogger(SampleReportFactory.class);

    private SampleReportFactory() {
    }

    @NotNull
    public static SampleReport fromLimsAndHospitalModel(@NotNull SampleMetadata sampleMetadata, @NotNull Lims lims,
            @NotNull LimsWide limsWide, @NotNull HospitalModel hospitalModel, @Nullable PatientTumorLocation patientTumorLocation) {
        String refSampleBarcode = sampleMetadata.refSampleBarcode();
        String refSampleId = sampleMetadata.refSampleId();
        String tumorSampleBarcode = sampleMetadata.tumorSampleBarcode();
        String tumorSampleId = sampleMetadata.tumorSampleId();

        lims.validateSampleBarcodeCombination(refSampleBarcode, refSampleId, tumorSampleBarcode, tumorSampleId);

        LocalDate arrivalDateRefSample = lims.arrivalDate(refSampleBarcode, refSampleId);
        if (arrivalDateRefSample == null) {
            LOGGER.warn("Could not find arrival date for ref sample: {}", refSampleId);
        }

        LocalDate arrivalDateTumorSample = lims.arrivalDate(tumorSampleBarcode, tumorSampleId);
        if (arrivalDateTumorSample == null) {
            LOGGER.warn("Could not find arrival date for tumor sample: {}", tumorSampleId);
        }

        HospitalQuery hospitalQuery = hospitalModel.queryHospitalDataForSample(tumorSampleId);

        return ImmutableSampleReport.builder()
                .sampleMetadata(sampleMetadata)
                .patientTumorLocation(patientTumorLocation)
                .refArrivalDate(arrivalDateRefSample)
                .tumorArrivalDate(arrivalDateTumorSample)
                .purityShallowSeq(lims.purityShallowSeq(tumorSampleBarcode))
                .labProcedures(lims.labProcedures(tumorSampleBarcode))
                .cohort(lims.cohort(tumorSampleBarcode))
                .projectName(lims.projectName(tumorSampleBarcode))
                .hospitalQuery(lims.hospitalQuery(tumorSampleId))
                .submissionId(lims.submissionId(tumorSampleBarcode))
                .hospitalPatientId(lims.hospitalPatientId(tumorSampleBarcode))
                .hospitalPathologySampleId(reportHospitalTissueIdPA(lims.hospitalPathologySampleId(tumorSampleBarcode), tumorSampleId)
                        ? lims.hospitalPathologySampleId(tumorSampleBarcode)
                        : null)
                .build();
    }

    public static boolean reportHospitalTissueIdPA(@NotNull String hospitalPaId, @NotNull String tumorSampleId) {
        LimsStudy study = LimsStudy.fromSampleId(tumorSampleId);

        if (!hospitalPaId.equals("N/A")) {
            LOGGER.info(hospitalPaId.substring(4, 9));

            if (hospitalPaId.startsWith("T") && hospitalPaId.substring(1, 3).matches("[0-9]+") && hospitalPaId.substring(3, 4).equals("-")
                    && hospitalPaId.substring(4, 9).matches("[0-9]+")) {
                return true;
            } else if (hospitalPaId.startsWith("C") && hospitalPaId.substring(1, 3).matches("[0-9]+") && hospitalPaId.substring(3, 4)
                    .equals("-") && hospitalPaId.substring(4, 9).matches("[0-9]+")) {
                return true;
            } else {
                if (study == LimsStudy.WIDE) {
                    LOGGER.warn("This is a WIDE sample. Solve pathology tissue ID");
                }
                LOGGER.warn("Wrong hospital tissue ID");
                return false;
            }
        } else {
            if (study == LimsStudy.WIDE) {
                LOGGER.warn("This is a WIDE sample. Solve pathology tissue ID");
            }
            LOGGER.warn("No hospital tissue ID");
            return false;
        }
    }
}
