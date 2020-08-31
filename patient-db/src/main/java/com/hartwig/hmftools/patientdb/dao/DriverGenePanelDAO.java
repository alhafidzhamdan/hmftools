package com.hartwig.hmftools.patientdb.dao;

import static com.hartwig.hmftools.patientdb.database.hmfpatients.Tables.DRIVERGENEPANEL;

import java.sql.Timestamp;
import java.util.Date;

import com.hartwig.hmftools.common.drivercatalog.panel.DriverGene;
import com.hartwig.hmftools.common.drivercatalog.panel.DriverGenePanel;

import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep11;

public class DriverGenePanelDAO {

    @NotNull
    private final DSLContext context;

    public DriverGenePanelDAO(@NotNull final DSLContext context) {
        this.context = context;
    }

    void writePanel(@NotNull DriverGenePanel genePanel) {
        context.truncate(DRIVERGENEPANEL).execute();
        Timestamp timestamp = new Timestamp(new Date().getTime());
        InsertValuesStep11 inserter = context.insertInto(DRIVERGENEPANEL,
                DRIVERGENEPANEL.MODIFIED,
                DRIVERGENEPANEL.GENE,
                DRIVERGENEPANEL.DELETIONBAND,
                DRIVERGENEPANEL.REPORTMISSENSE,
                DRIVERGENEPANEL.REPORTNONSENSE,
                DRIVERGENEPANEL.REPORTSPLICE,
                DRIVERGENEPANEL.REPORTDELETION,
                DRIVERGENEPANEL.REPORTDISRUPTION,
                DRIVERGENEPANEL.REPORTAMPLIFICATION,
                DRIVERGENEPANEL.REPORTHOTSPOT,
                DRIVERGENEPANEL.LIKELIHOODTYPE);

        for (DriverGene driverGene : genePanel.driverGenes()) {
            inserter.values(timestamp,
                    driverGene.gene(),
                    driverGene.deletionBand(),
                    driverGene.reportMissenseAndInframe(),
                    driverGene.reportNonsenseAndFrameshift(),
                    driverGene.reportSplice(),
                    driverGene.reportDeletion(),
                    driverGene.reportDisruption(),
                    driverGene.reportAmplification(),
                    driverGene.reportHotspot(),
                    driverGene.likelihoodType().toString());
        }

        inserter.execute();
    }
}