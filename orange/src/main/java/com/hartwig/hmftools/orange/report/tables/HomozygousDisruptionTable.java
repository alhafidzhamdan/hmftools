package com.hartwig.hmftools.orange.report.tables;

import java.util.List;
import java.util.stream.Collectors;

import com.hartwig.hmftools.common.linx.ReportableHomozygousDisruption;
import com.hartwig.hmftools.orange.report.util.ChromosomeUtil;
import com.hartwig.hmftools.orange.report.util.TableUtil;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Table;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

public final class HomozygousDisruptionTable {

    private HomozygousDisruptionTable() {
    }

    @NotNull
    public static Table build(@NotNull String title, float width, @NotNull List<ReportableHomozygousDisruption> homozygousDisruptions) {
        if (homozygousDisruptions.isEmpty()) {
            return TableUtil.createEmptyTable(title, width);
        }

        Table table = TableUtil.createReportContentTable(width,
                new float[] { 1, 1, 4 },
                new Cell[] { TableUtil.createHeaderCell("Location"), TableUtil.createHeaderCell("Gene"),
                        TableUtil.createHeaderCell(Strings.EMPTY) });

        for (ReportableHomozygousDisruption homozygousDisruption : sort(homozygousDisruptions)) {
            table.addCell(TableUtil.createContentCell(homozygousDisruption.chromosome() + homozygousDisruption.chromosomeBand()));
            table.addCell(TableUtil.createContentCell(homozygousDisruption.gene()));
            table.addCell(TableUtil.createContentCell(Strings.EMPTY));
        }

        return TableUtil.createWrappingReportTable(table, title);
    }

    @NotNull
    private static List<ReportableHomozygousDisruption> sort(@NotNull List<ReportableHomozygousDisruption> homozygousDisruptions) {
        return homozygousDisruptions.stream().sorted((disruption1, disruption2) -> {
            String location1 = ChromosomeUtil.zeroPrefixed(disruption1.chromosome() + disruption1.chromosomeBand());
            String location2 = ChromosomeUtil.zeroPrefixed(disruption2.chromosome() + disruption2.chromosomeBand());

            if (location1.equals(location2)) {
                return disruption1.gene().compareTo(disruption2.gene());
            } else {
                return location1.compareTo(location2);
            }
        }).collect(Collectors.toList());
    }
}
