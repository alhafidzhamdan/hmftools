package com.hartwig.hmftools.patientreporter.loadStructuralVariants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

public class DisruptionFactory {

    private DisruptionFactory() {
    }

    private static final String DELIMITER = ",";


    @NotNull
    public static DisruptionAnalyzer readingDisruption(@NotNull String disruptionFile) throws IOException {
        final List<DisruptionReaderFile> disruptions = new ArrayList<>();

        final List<String> lineDisruptions = Files.readAllLines(new File(disruptionFile).toPath());

        // Skip header line
        for (String lineDisruption : lineDisruptions.subList(1, lineDisruptions.size())) {
            disruptions.add(fromLineVariants(lineDisruption));
        }
        return new DisruptionAnalyzer (disruptions);
    }

    @NotNull
    private static DisruptionReaderFile fromLineVariants(@NotNull String line) {
        final String[] values = line.split(DELIMITER);
       return ImmutableDisruptionReaderFile.builder()
               .reportable(Boolean.valueOf(values[0]))
               .svId(values[1])
               .chromosome(values[2])
               .position(values[3])
               .orientation(values[4])
               .type(values[5])
               .gene(values[6])
               .transcript(values[7])
               .strand(values[8])
               .regionType(values[9])
               .codingType(values[10])
               .biotype(values[11])
               .exon(values[12])
               .isDisruptive(Boolean.valueOf(values[13]))
               .build();
    }

}
