package com.wwpet.service;

import com.wwpet.model.PetProfile;
import com.wwpet.model.PetState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PetDataStore {
    private final Path dataPath;

    public PetDataStore(Path dataPath) {
        this.dataPath = dataPath;
    }

    public LoadResult load() {
        boolean firstLaunch = !Files.exists(dataPath);
        if (firstLaunch) {
            return new LoadResult(new PetProfile(), true);
        }

        try {
            String json = Files.readString(dataPath, StandardCharsets.UTF_8);
            PetProfile profile = new PetProfile();
            profile.setName(readString(json, "name", "wwPet"));
            profile.setLevel(readInt(json, "level", 1));
            profile.setTotalFocusSeconds(readLong(json, "totalFocusSeconds", 0L));
            profile.setCompanionDays(readLong(json, "companionDays", 1L));
            profile.setAlwaysOnTop(readBoolean(json, "alwaysOnTop", true));
            profile.setWindowX(readInt(json, "windowX", -1));
            profile.setWindowY(readInt(json, "windowY", -1));
            profile.setFirstSeenDate(readString(json, "firstSeenDate", ""));
            profile.setLastOpenedDate(readString(json, "lastOpenedDate", ""));
            profile.setFocusDeadlineEpochMillis(readNullableLong(json, "focusDeadlineEpochMillis"));
            profile.setLastMilestoneStep(readLong(json, "lastMilestoneStep", 0L));

            String stateValue = readString(json, "state", PetState.REST.name());
            try {
                profile.setState(PetState.valueOf(stateValue));
            } catch (IllegalArgumentException ignored) {
                profile.setState(PetState.REST);
            }
            return new LoadResult(profile, false);
        } catch (IOException ignored) {
            return new LoadResult(new PetProfile(), true);
        }
    }

    public void save(PetProfile profile) {
        try {
            Files.createDirectories(dataPath.getParent());
            Files.writeString(dataPath, toJson(profile), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private String toJson(PetProfile profile) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        appendLine(builder, "name", quote(profile.getName()), true);
        appendLine(builder, "level", Integer.toString(profile.getLevel()), true);
        appendLine(builder, "totalFocusSeconds", Long.toString(profile.getTotalFocusSeconds()), true);
        appendLine(builder, "companionDays", Long.toString(profile.getCompanionDays()), true);
        appendLine(builder, "state", quote(profile.getState().name()), true);
        appendLine(builder, "alwaysOnTop", Boolean.toString(profile.isAlwaysOnTop()), true);
        appendLine(builder, "windowX", Integer.toString(profile.getWindowX()), true);
        appendLine(builder, "windowY", Integer.toString(profile.getWindowY()), true);
        appendLine(builder, "firstSeenDate", quote(profile.getFirstSeenDate()), true);
        appendLine(builder, "lastOpenedDate", quote(profile.getLastOpenedDate()), true);
        appendLine(builder, "focusDeadlineEpochMillis", profile.getFocusDeadlineEpochMillis() == null ? "null" : Long.toString(profile.getFocusDeadlineEpochMillis()), true);
        appendLine(builder, "lastMilestoneStep", Long.toString(profile.getLastMilestoneStep()), false);
        builder.append("}\n");
        return builder.toString();
    }

    private void appendLine(StringBuilder builder, String key, String value, boolean appendComma) {
        builder.append("  \"")
                .append(key)
                .append("\": ")
                .append(value);
        if (appendComma) {
            builder.append(",");
        }
        builder.append("\n");
    }

    private String quote(String value) {
        String escaped = value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }

    private String readString(String json, String key, String fallback) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\\\\\"])*)\"").matcher(json);
        if (!matcher.find()) {
            return fallback;
        }
        String rawValue = matcher.group(1);
        return rawValue
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private int readInt(String json, String key, int fallback) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        if (!matcher.find()) {
            return fallback;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private long readLong(String json, String key, long fallback) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        if (!matcher.find()) {
            return fallback;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private Long readNullableLong(String json, String key) {
        Matcher nullMatcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*null").matcher(json);
        if (nullMatcher.find()) {
            return null;
        }
        Matcher valueMatcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        if (!valueMatcher.find()) {
            return null;
        }
        try {
            return Long.parseLong(valueMatcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean readBoolean(String json, String key, boolean fallback) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)").matcher(json);
        if (!matcher.find()) {
            return fallback;
        }
        return Boolean.parseBoolean(matcher.group(1));
    }

    public record LoadResult(PetProfile profile, boolean firstLaunch) {
    }
}
