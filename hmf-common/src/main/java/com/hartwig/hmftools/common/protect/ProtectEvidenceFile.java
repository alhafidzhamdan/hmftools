package com.hartwig.hmftools.common.protect;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;

public final class ProtectEvidenceFile {

    private static final String EXTENSION = ".protect.tsv";
    private static final String FIELD_DELIMITER = "\t";
    private static final String URL_DELIMITER = ",";

    private ProtectEvidenceFile() {
    }

    @NotNull
    public static String generateFilename(@NotNull String basePath, @NotNull String sample) {
        return basePath + File.separator + sample + EXTENSION;
    }

    public static void write(@NotNull String file, @NotNull List<ProtectEvidence> evidence) throws IOException {
        List<String> lines = Lists.newArrayList();
        lines.add(header());
        lines.addAll(evidence.stream().map(ProtectEvidenceFile::toLine).collect(Collectors.toList()));
        Files.write(new File(file).toPath(), lines);
    }

    @NotNull
    private static String header() {
        return new StringJoiner(FIELD_DELIMITER).add("event")
                .add("germline")
                .add("source")
                .add("reported")
                .add("treatment")
                .add("onLabel")
                .add("level")
                .add("direction")
                .add("urls")
                .toString();
    }

    @NotNull
    private static String toLine(@NotNull ProtectEvidence evidence) {
        StringJoiner urlJoiner = new StringJoiner(URL_DELIMITER);
        for (String url : evidence.urls()) {
            urlJoiner.add(url);
        }

        return new StringJoiner(FIELD_DELIMITER).add(evidence.genomicEvent())
                .add(String.valueOf(evidence.germline()))
                .add(evidence.source().toString())
                .add(String.valueOf(evidence.reported()))
                .add(evidence.treatment())
                .add(String.valueOf(evidence.onLabel()))
                .add(evidence.level().toString())
                .add(evidence.direction().toString())
                .add(urlJoiner.toString())
                .toString();
    }
}