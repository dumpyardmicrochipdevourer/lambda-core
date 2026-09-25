package com.microchip.lambda_core.service.util;

import java.util.Collection;
import java.util.Locale;

public final class FileNames {

    private static final int MAX_LENGTH = 255;

    private FileNames() {}

    public static String validate(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_LENGTH
                || name.equals(".") || name.equals("..")
                || name.chars().anyMatch(c -> c < 32 || c == '/' || c == '\\')) {
            throw new IllegalArgumentException("недопустимое имя файла");
        }
        return name.strip();
    }

    // report.pdf -> report (1).pdf -> report (2).pdf, names compared case-insensitively
    public static String free(String name, Collection<String> takenLower) {
        if (!takenLower.contains(name.toLowerCase(Locale.ROOT))) {
            return name;
        }
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        for (int n = 1; ; n++) {
            String candidate = stem + " (" + n + ")" + ext;
            if (!takenLower.contains(candidate.toLowerCase(Locale.ROOT))) {
                return candidate;
            }
        }
    }
}
