package com.wwpet.service;

import com.wwpet.model.PetProfile;
import com.wwpet.model.PetState;
import com.wwpet.model.SpeechLines;

import javax.swing.Timer;
import java.awt.Point;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class PetController {
    private static final int LEVEL_SECONDS_PER_LEVEL = 3600;
    private static final int FOCUS_MILESTONE_HOURS = 2;
    private static final int AUTOSAVE_INTERVAL_SECONDS = 15;

    private final PetDataStore dataStore;
    private final SpeechLines speechLines;
    private final PetProfile profile;
    private final boolean firstLaunch;
    private final Timer tickTimer;

    private Runnable refreshHook = () -> {
    };
    private Consumer<String> speechHook = message -> {
    };
    private int autosaveCounter = 0;
    private long currentFocusSessionSeconds = 0L;

    public PetController(PetDataStore dataStore, SpeechLines speechLines) {
        this.dataStore = dataStore;
        this.speechLines = speechLines == null ? new SpeechLines() : speechLines.copy();
        PetDataStore.LoadResult loadResult = dataStore.load();
        this.profile = loadResult.profile();
        this.firstLaunch = loadResult.firstLaunch();
        refreshCompanionDays();
        normalizeProfileAfterLoad();
        syncLevel();
        this.tickTimer = new Timer(1000, event -> onTick());
        this.tickTimer.setRepeats(true);
    }

    public void setUiHooks(Runnable refreshHook, Consumer<String> speechHook) {
        this.refreshHook = refreshHook == null ? () -> {
        } : refreshHook;
        this.speechHook = speechHook == null ? message -> {
        } : speechHook;
    }

    public void start() {
        tickTimer.start();
        save();
        notifyRefresh();
    }

    public void onWindowShown() {
        if (firstLaunch) {
            say(randomFrom(speechLines.getFirstLaunch()));
        }
    }

    public PetProfile snapshot() {
        return profile.copy();
    }

    public PetState getState() {
        return profile.getState();
    }

    public boolean isTimedFocusActive() {
        return profile.getState() == PetState.FOCUS && profile.getFocusDeadlineEpochMillis() != null;
    }

    public boolean isFocusStateActive() {
        return profile.getState() == PetState.FOCUS;
    }

    public String getCurrentFocusClock() {
        return formatClock(currentFocusSessionSeconds);
    }

    public void interact() {
        List<String> pool = profile.getState() == PetState.FOCUS ? speechLines.getFocusInteractions() : speechLines.getRestInteractions();
        say(randomFrom(pool));
        notifyRefresh();
    }

    public void startFocusUntimed() {
        currentFocusSessionSeconds = 0L;
        profile.setState(PetState.FOCUS);
        profile.setFocusDeadlineEpochMillis(null);
        syncLevel();
        save();
        say(randomFrom(speechLines.getFocusStart()));
        notifyRefresh();
    }

    public void startTimedFocus(Duration duration) {
        long millis = Math.max(1000L, duration.toMillis());
        currentFocusSessionSeconds = 0L;
        profile.setState(PetState.FOCUS);
        profile.setFocusDeadlineEpochMillis(System.currentTimeMillis() + millis);
        syncLevel();
        save();
        say(randomFrom(speechLines.getTimedFocusStart()));
        notifyRefresh();
    }

    public void switchToRest() {
        switchToRest(randomFrom(speechLines.getFocusSwitchToRest()));
    }

    public void exitFocusEarly() {
        switchToRest(randomFrom(speechLines.getFocusExitEarly()));
    }

    public void toggleAlwaysOnTop(boolean alwaysOnTop) {
        profile.setAlwaysOnTop(alwaysOnTop);
        save();
        notifyRefresh();
    }

    public void updateWindowLocation(Point location) {
        profile.setWindowX(location.x);
        profile.setWindowY(location.y);
        save();
    }

    public void markSnappedToTaskbar() {
        say(randomFrom(speechLines.getSnapToTaskbar()));
    }

    public String buildTooltipText() {
        StringBuilder builder = new StringBuilder("<html><div>");
        builder.append("<b>").append(escape(profile.getName())).append("</b><br/>");
        builder.append("状态：").append(profile.getState().getDisplayName()).append("<br/>");
        builder.append("等级：Lv.").append(profile.getLevel()).append("<br/>");
        builder.append("专注时长：").append(formatDuration(profile.getTotalFocusSeconds())).append("<br/>");
        builder.append("陪伴天数：").append(profile.getCompanionDays()).append(" 天");
        if (isFocusStateActive()) {
            builder.append("<br/>本轮专注：").append(getCurrentFocusClock());
        }
        if (isTimedFocusActive()) {
            long remainingSeconds = Math.max(0L, (profile.getFocusDeadlineEpochMillis() - System.currentTimeMillis()) / 1000L);
            builder.append("<br/>剩余专注：").append(formatClock(remainingSeconds));
        }
        builder.append("</div></html>");
        return builder.toString();
    }

    public void shutdown() {
        if (tickTimer.isRunning()) {
            tickTimer.stop();
        }
        currentFocusSessionSeconds = 0L;
        profile.setState(PetState.REST);
        profile.setFocusDeadlineEpochMillis(null);
        refreshCompanionDays();
        syncLevel();
        save();
    }

    private void onTick() {
        refreshCompanionDays();
        if (profile.getState() == PetState.FOCUS) {
            currentFocusSessionSeconds++;
            profile.setTotalFocusSeconds(profile.getTotalFocusSeconds() + 1);
            syncLevel();
            handleFocusMilestone();
            if (profile.getFocusDeadlineEpochMillis() != null && System.currentTimeMillis() >= profile.getFocusDeadlineEpochMillis()) {
                switchToRest(randomFrom(speechLines.getFocusTimedComplete()));
            }
        }

        autosaveCounter++;
        if (autosaveCounter >= AUTOSAVE_INTERVAL_SECONDS) {
            autosaveCounter = 0;
            save();
        }
        notifyRefresh();
    }

    private void handleFocusMilestone() {
        long stepSizeSeconds = FOCUS_MILESTONE_HOURS * 3600L;
        long currentStep = profile.getTotalFocusSeconds() / stepSizeSeconds;
        if (currentStep > profile.getLastMilestoneStep()) {
            profile.setLastMilestoneStep(currentStep);
            say(applySpeechPlaceholders(randomFrom(speechLines.getFocusMilestone()), currentStep * FOCUS_MILESTONE_HOURS));
            save();
        }
    }

    private void switchToRest(String message) {
        currentFocusSessionSeconds = 0L;
        profile.setState(PetState.REST);
        profile.setFocusDeadlineEpochMillis(null);
        syncLevel();
        save();
        say(message);
        notifyRefresh();
    }

    private void normalizeProfileAfterLoad() {
        if (profile.getName() == null || profile.getName().isBlank()) {
            profile.setName("wwPet");
        }
        if (profile.getState() == null) {
            profile.setState(PetState.REST);
        }
        if (profile.getTotalFocusSeconds() < 0L) {
            profile.setTotalFocusSeconds(0L);
        }
        if (profile.getCompanionDays() < 1L) {
            profile.setCompanionDays(1L);
        }
        if (profile.getWindowX() < -1) {
            profile.setWindowX(-1);
        }
        if (profile.getWindowY() < -1) {
            profile.setWindowY(-1);
        }
        currentFocusSessionSeconds = 0L;
        profile.setState(PetState.REST);
        profile.setFocusDeadlineEpochMillis(null);
    }

    private void refreshCompanionDays() {
        LocalDate today = LocalDate.now();
        if (profile.getFirstSeenDate() == null || profile.getFirstSeenDate().isBlank()) {
            profile.setFirstSeenDate(today.toString());
        }
        LocalDate firstSeen;
        try {
            firstSeen = LocalDate.parse(profile.getFirstSeenDate());
        } catch (Exception ignored) {
            firstSeen = today;
            profile.setFirstSeenDate(today.toString());
        }
        long days = ChronoUnit.DAYS.between(firstSeen, today) + 1L;
        profile.setCompanionDays(Math.max(1L, days));
        profile.setLastOpenedDate(today.toString());
    }

    private void syncLevel() {
        int level = (int) Math.max(1L, 1L + profile.getTotalFocusSeconds() / LEVEL_SECONDS_PER_LEVEL);
        profile.setLevel(level);
    }

    private void save() {
        dataStore.save(profile.copy());
    }

    private void notifyRefresh() {
        refreshHook.run();
    }

    private void say(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        speechHook.accept(message);
    }

    private String randomFrom(List<String> pool) {
        if (pool == null || pool.isEmpty()) {
            return null;
        }
        int index = ThreadLocalRandom.current().nextInt(pool.size());
        return applySpeechPlaceholders(pool.get(index), null);
    }

    private String applySpeechPlaceholders(String message, Long hours) {
        if (message == null) {
            return null;
        }
        String result = message
                .replace("{name}", profile.getName())
                .replace("{level}", Integer.toString(profile.getLevel()));
        if (hours != null) {
            result = result.replace("{hours}", Long.toString(hours));
        }
        return result;
    }

    private String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return hours + "小时 " + minutes + "分 " + seconds + "秒";
        }
        if (minutes > 0L) {
            return minutes + "分 " + seconds + "秒";
        }
        return seconds + "秒";
    }

    private String formatClock(long totalSeconds) {
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
