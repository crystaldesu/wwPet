package com.wwpet.service;

import com.wwpet.model.UiSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UiSettingsStore {
    private final Path settingsPath;

    public UiSettingsStore(Path settingsPath) {
        this.settingsPath = settingsPath;
    }

    public UiSettings load() {
        UiSettings settings = new UiSettings();
        if (!Files.exists(settingsPath)) {
            save(settings);
            return settings;
        }

        try {
            String json = Files.readString(settingsPath, StandardCharsets.UTF_8);
            settings.setFontFamily(readString(json, "fontFamily", settings.getFontFamily()));
            settings.setFontFile(readString(json, "fontFile", settings.getFontFile()));
            settings.setMenuFontSize(readInt(json, "menuFontSize", settings.getMenuFontSize(), 10, 36));
            settings.setBadgeFontSize(readInt(json, "badgeFontSize", settings.getBadgeFontSize(), 10, 36));
            settings.setDialogTitleFontSize(readInt(json, "dialogTitleFontSize", settings.getDialogTitleFontSize(), 12, 40));
            settings.setDialogTextFontSize(readInt(json, "dialogTextFontSize", settings.getDialogTextFontSize(), 10, 28));
            settings.setDialogInputFontSize(readInt(json, "dialogInputFontSize", settings.getDialogInputFontSize(), 10, 32));
            return settings;
        } catch (IOException ignored) {
            save(settings);
            return settings;
        }
    }

    public void save(UiSettings settings) {
        try {
            Files.createDirectories(settingsPath.getParent());
            Files.writeString(settingsPath, toJson(settings), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private String toJson(UiSettings settings) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        appendLine(builder, "fontFamily", quote(settings.getFontFamily()), true);
        appendLine(builder, "fontFile", quote(settings.getFontFile()), true);
        appendLine(builder, "menuFontSize", Integer.toString(settings.getMenuFontSize()), true);
        appendLine(builder, "badgeFontSize", Integer.toString(settings.getBadgeFontSize()), true);
        appendLine(builder, "dialogTitleFontSize", Integer.toString(settings.getDialogTitleFontSize()), true);
        appendLine(builder, "dialogTextFontSize", Integer.toString(settings.getDialogTextFontSize()), true);
        appendLine(builder, "dialogInputFontSize", Integer.toString(settings.getDialogInputFontSize()), false);
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
        return matcher.group(1)
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private int readInt(String json, String key, int fallback, int min, int max) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        if (!matcher.find()) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(matcher.group(1));
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
