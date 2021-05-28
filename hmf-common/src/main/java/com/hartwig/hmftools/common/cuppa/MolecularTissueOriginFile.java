package com.hartwig.hmftools.common.cuppa;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MolecularTissueOriginFile {

    private static final Logger LOGGER = LogManager.getLogger(MolecularTissueOriginFile.class);

    private MolecularTissueOriginFile(){
    }

    @NotNull
    public static String read(@NotNull String molecularTissueOriginTxt) throws IOException {
        String origin = fromLines(Files.readAllLines(new File(molecularTissueOriginTxt).toPath()));
        if (origin == null) {
            LOGGER.warn("No molecular tissue origin could be read from {}!", molecularTissueOriginTxt);
            origin = Strings.EMPTY;
        }
        return origin;
    }

    @Nullable
    private static String fromLines(@NotNull List<String> lines) {
        if (lines.size() == 1) {
            return lines.get(0).split(" - ")[1];
        } else {
            return null;
        }
    }
}