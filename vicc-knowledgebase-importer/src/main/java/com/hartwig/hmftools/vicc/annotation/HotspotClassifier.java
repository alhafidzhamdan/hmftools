package com.hartwig.hmftools.vicc.annotation;

import static com.hartwig.hmftools.common.variant.hgvs.HgvsConstants.HGVS_DELETION;
import static com.hartwig.hmftools.common.variant.hgvs.HgvsConstants.HGVS_DUPLICATION;
import static com.hartwig.hmftools.common.variant.hgvs.HgvsConstants.HGVS_FRAMESHIFT_SUFFIX;
import static com.hartwig.hmftools.common.variant.hgvs.HgvsConstants.HGVS_FRAMESHIFT_SUFFIX_WITH_STOP_GAINED;
import static com.hartwig.hmftools.common.variant.hgvs.HgvsConstants.HGVS_INSERTION;
import static com.hartwig.hmftools.common.variant.hgvs.HgvsConstants.HGVS_RANGE_INDICATOR;

import java.util.Set;

import com.google.common.collect.Sets;

import org.jetbrains.annotations.NotNull;

final class HotspotClassifier {

    private static final Set<String> CAPITALIZED_STRINGS_TO_UNCAPITALIZE = Sets.newHashSet("DELINS", "DEL", "INS", "DUP", "FS");

    private static final int MAX_INFRAME_BASE_LENGTH = 50;

    private HotspotClassifier() {
    }

    public static boolean isHotspot(@NotNull String featureName) {
        String proteinAnnotation = extractProteinAnnotation(featureName);

        boolean isHotspot;
        if (isFrameshift(proteinAnnotation)) {
            isHotspot = isValidFrameshift(proteinAnnotation);
        } else if (proteinAnnotation.contains(HGVS_RANGE_INDICATOR)) {
            isHotspot = isValidRangeMutation(proteinAnnotation);
        } else if (proteinAnnotation.contains(HGVS_DELETION + HGVS_INSERTION)) {
            isHotspot = isValidComplexDeletionInsertion(proteinAnnotation);
        } else if (proteinAnnotation.startsWith("*")) {
            isHotspot = true;
        } else {
            isHotspot = isValidSingleCodonMutation(proteinAnnotation);
        }

        if (isHotspot) {
            return !isHotspotOnFusionGene(featureName);
        }

        return false;
    }

    @NotNull
    public static String extractProteinAnnotation(@NotNull String featureName) {
        String trimmedName = featureName.trim();
        // Many KBs include the gene in the feature name in some form (eg "EGFR E709K" or "EGFR:E709K").
        // Other KBs put the coding info behind the protein annotation ("V130L (c.388G>C)" rather than the gene in front of it)
        String proteinAnnotation;
        if (trimmedName.contains(" ")) {
            String[] trimmedParts = trimmedName.split(" ");
            if (trimmedParts[1].contains("(c.")) {
                proteinAnnotation = trimmedParts[0];
            } else {
                proteinAnnotation = trimmedParts[1];
            }
        } else if (trimmedName.contains(":")) {
            proteinAnnotation = trimmedName.split(":")[1];
        } else {
            proteinAnnotation = trimmedName;
        }

        // Some KBs include "p." in front of the protein annotation
        proteinAnnotation = proteinAnnotation.startsWith("p.") ? proteinAnnotation.substring(2) : proteinAnnotation;

        // Some KBs use DEL/INS/FS rather than del/ins/fs
        for (String stringToLookFor : CAPITALIZED_STRINGS_TO_UNCAPITALIZE) {
            int position = proteinAnnotation.indexOf(stringToLookFor);
            if (position > 0 && Character.isDigit(proteinAnnotation.charAt(position - 1))) {
                proteinAnnotation = proteinAnnotation.substring(0, position) + stringToLookFor.toLowerCase() + proteinAnnotation.substring(
                        position + stringToLookFor.length());
            }
        }

        // Cut out the trailing stop gained in case a stop gained is following on a frameshift
        int trailingStopGained = proteinAnnotation.indexOf("fs*");
        if (trailingStopGained > 0) {
            proteinAnnotation = proteinAnnotation.substring(0, trailingStopGained + 2);
        }

        return proteinAnnotation;
    }

    private static boolean isFrameshift(@NotNull String proteinAnnotation) {
        return proteinAnnotation.endsWith(HGVS_FRAMESHIFT_SUFFIX) || proteinAnnotation.endsWith(HGVS_FRAMESHIFT_SUFFIX_WITH_STOP_GAINED);
    }

    private static boolean isValidFrameshift(@NotNull String proteinAnnotation) {
        int frameshiftPosition = proteinAnnotation.indexOf(HGVS_FRAMESHIFT_SUFFIX);
        if (frameshiftPosition > 1) {
            return isInteger(proteinAnnotation.substring(frameshiftPosition - 1, frameshiftPosition));
        }

        return false;
    }

    private static boolean isValidRangeMutation(@NotNull String proteinAnnotation) {
        assert proteinAnnotation.contains(HGVS_RANGE_INDICATOR);

        // Features could be ranges such as E102_I103del. We whitelist specific feature types when analyzing a range.
        String[] annotationParts = proteinAnnotation.split(HGVS_RANGE_INDICATOR);
        String annotationStartPart = annotationParts[0];
        String annotationEndPart = annotationParts[1];
        if (annotationEndPart.contains(HGVS_INSERTION) || annotationEndPart.contains(HGVS_DUPLICATION) || annotationEndPart.contains(
                HGVS_DELETION)) {
            int indexOfEvent;
            // Keep in mind that 'del' always comes prior to 'ins' in situations of complex inframes.
            if (annotationEndPart.contains(HGVS_DELETION)) {
                indexOfEvent = annotationEndPart.indexOf(HGVS_DELETION);
            } else if (annotationEndPart.contains(HGVS_DUPLICATION)) {
                indexOfEvent = annotationEndPart.indexOf(HGVS_DUPLICATION);
            } else {
                indexOfEvent = annotationEndPart.indexOf(HGVS_INSERTION);
            }

            String startRange = annotationStartPart.substring(1);
            String endRange = annotationEndPart.substring(1, indexOfEvent);

            if (isLong(startRange) && isLong(endRange)) {
                return 3 * (1 + Long.parseLong(endRange) - Long.parseLong(startRange)) <= MAX_INFRAME_BASE_LENGTH;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private static boolean isValidComplexDeletionInsertion(@NotNull String proteinAnnotation) {
        String[] annotationParts = proteinAnnotation.split(HGVS_DELETION + HGVS_INSERTION);

        return isInteger(annotationParts[0].substring(1)) && (3 * annotationParts[1].length()) <= MAX_INFRAME_BASE_LENGTH;
    }

    private static boolean isValidSingleCodonMutation(@NotNull String proteinAnnotation) {
        if (proteinAnnotation.contains(HGVS_INSERTION)) {
            // Insertions are only allowed in a range, since we need to know where to insert the sequence exactly.
            return false;
        }

        // Features are expected to look something like V600E (1 char - N digits - M chars)
        if (proteinAnnotation.length() < 3) {
            return false;
        }

        if (!Character.isLetter(proteinAnnotation.charAt(0))) {
            return false;
        }

        if (!Character.isDigit(proteinAnnotation.charAt(1))) {
            return false;
        }

        boolean haveObservedNonDigit = !Character.isDigit(proteinAnnotation.charAt(2));
        int firstNotDigit = haveObservedNonDigit ? 2 : -1;
        for (int i = 3; i < proteinAnnotation.length(); i++) {
            char charToEvaluate = proteinAnnotation.charAt(i);
            if (haveObservedNonDigit && Character.isDigit(charToEvaluate)) {
                return false;
            }
            boolean isDigit = Character.isDigit(charToEvaluate);
            if (!isDigit && firstNotDigit == -1) {
                firstNotDigit = i;
            }

            haveObservedNonDigit = haveObservedNonDigit || !isDigit;
        }

        if (!haveObservedNonDigit) {
            return false;
        }

        String newAminoAcid = proteinAnnotation.substring(firstNotDigit);
        // X is a wildcard that we don't support, and "/" indicates logical OR that we don't support.
        return !newAminoAcid.equals("X") && !newAminoAcid.contains("/");
    }

    private static boolean isHotspotOnFusionGene(@NotNull String featureName) {
        String trimmedName = featureName.trim();
        if (trimmedName.contains(" ")) {
            String[] parts = trimmedName.split(" ");
            return FusionClassifier.extractFusionEvent(parts[0]) != null;
        }
        return false;
    }

    private static boolean isLong(@NotNull String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException exp) {
            return false;
        }
    }

    private static boolean isInteger(@NotNull String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException exp) {
            return false;
        }
    }
}