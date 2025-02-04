package com.hartwig.hmftools.patientreporter.panel;

import java.util.Optional;
import com.hartwig.hmftools.patientreporter.PatientReport;
import com.hartwig.hmftools.patientreporter.SampleReport;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
public abstract class PanelReport implements com.hartwig.hmftools.patientreporter.PanelReport {

    @Override
    @NotNull
    public abstract SampleReport sampleReport();

    @Override
    @NotNull
    public abstract String qsFormNumber();

    @Nullable
    public abstract String pipelineVersion();

    @NotNull
    public abstract String  VCFFilename();

    @Override
    @NotNull
    public abstract Optional<String> comments();

    @Override
    public abstract boolean isCorrectedReport();

    @Override
    public abstract boolean isCorrectedReportExtern();

    @Override
    @NotNull
    public abstract String signaturePath();

    @Override
    @NotNull
    public abstract String logoCompanyPath();

    @NotNull
    @Override
    public abstract String reportDate();
}
