package com.wwpet.service;

import com.wwpet.model.SpeechLines;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpeechLinesStore {
    private final Path speechLinesPath;

    public SpeechLinesStore(Path speechLinesPath) {
        this.speechLinesPath = speechLinesPath;
    }

    public SpeechLines load() {
        SpeechLines speechLines = new SpeechLines();
        if (!Files.exists(speechLinesPath)) {
            save(speechLines);
            return speechLines;
        }

        try {
            String json = Files.readString(speechLinesPath, StandardCharsets.UTF_8);
            speechLines.setFirstLaunch(readStringList(json, "firstLaunch", speechLines.getFirstLaunch()));
            speechLines.setRestInteractions(readStringList(json, "restInteractions", speechLines.getRestInteractions()));
            speechLines.setFocusInteractions(readStringList(json, "focusInteractions", speechLines.getFocusInteractions()));
            speechLines.setSnapToTaskbar(readStringList(json, "snapToTaskbar", speechLines.getSnapToTaskbar()));
            speechLines.setFocusStart(readStringList(json, "focusStart", speechLines.getFocusStart()));
            speechLines.setTimedFocusStart(readStringList(json, "timedFocusStart", speechLines.getTimedFocusStart()));
            speechLines.setFocusSwitchToRest(readStringList(json, "focusSwitchToRest", speechLines.getFocusSwitchToRest()));
            speechLines.setFocusExitEarly(readStringList(json, "focusExitEarly", speechLines.getFocusExitEarly()));
            speechLines.setFocusTimedComplete(readStringList(json, "focusTimedComplete", speechLines.getFocusTimedComplete()));
            speechLines.setFocusMilestone(readStringList(json, "focusMilestone", speechLines.getFocusMilestone()));
            return speechLines;
        } catch (IOException ignored) {
            save(speechLines);
            return speechLines;
        }
    }

    public void save(SpeechLines speechLines) {
        try {
            Files.createDirectories(speechLinesPath.getParent());
            Files.writeString(speechLinesPath, toJson(speechLines), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private String toJson(SpeechLines speechLines) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        appendArray(builder, "firstLaunch", speechLines.getFirstLaunch(), true);
        appendArray(builder, "restInteractions", speechLines.getRestInteractions(), true);
        appendArray(builder, "focusInteractions", speechLines.getFocusInteractions(), true);
        appendArray(builder, "snapToTaskbar", speechLines.getSnapToTaskbar(), true);
        appendArray(builder, "focusStart", speechLines.getFocusStart(), true);
        appendArray(builder, "timedFocusStart", speechLines.getTimedFocusStart(), true);
        appendArray(builder, "focusSwitchToRest", speechLines.getFocusSwitchToRest(), true);
        appendArray(builder, "focusExitEarly", speechLines.getFocusExitEarly(), true);
        appendArray(builder, "focusTimedComplete", speechLines.getFocusTimedComplete(), true);
        appendArray(builder, "focusMilestone", speechLines.getFocusMilestone(), false);
        builder.append("}\n");
        return builder.toString();
    }

    private void appendArray(StringBuilder builder, String key, List<String> values, boolean appendComma) {
        builder.append("  \"").append(key).append("\": [\n");
        for (int index = 0; index < values.size(); index++) {
            builder.append("    ").append(quote(values.get(index)));
            if (index < values.size() - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append("  ]");
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

    private List<String> readStringList(String json, String key, List<String> fallback) {
        String single = readOptionalString(json, key);
        if (single != null) {
            return List.of(single);
        }

        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL).matcher(json);
        if (!matcher.find()) {
            return new ArrayList<>(fallback);
        }

        String body = matcher.group(1);
        Matcher itemMatcher = Pattern.compile("\"((?:\\\\.|[^\\\\\"])*)\"").matcher(body);
        List<String> values = new ArrayList<>();
        while (itemMatcher.find()) {
            values.add(unescape(itemMatcher.group(1)));
        }
        return values;
    }

    private String readOptionalString(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\\\\\"])*)\"").matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return unescape(matcher.group(1));
    }

    private String unescape(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
