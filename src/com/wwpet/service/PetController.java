package com.wwpet.service;

import com.wwpet.model.PetProfile;
import com.wwpet.model.PetState;

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
    private static final List<String> REST_INTERACTIONS = List.of(
            "摸摸头，我今天也会陪着你。",
            "休息也很重要呀。",
            "我已经准备好继续陪你啦。"
    );
    private static final List<String> FOCUS_INTERACTIONS = List.of(
            "继续冲，我在旁边给你打气。",
            "专注中的你超厉害。",
            "再坚持一下，我们离目标更近啦。"
    );

    private final PetDataStore dataStore;
    private final PetProfile profile;
    private final boolean firstLaunch;
    private final Timer tickTimer;

    private Runnable refreshHook = () -> {
    };
    private Consumer<String> speechHook = message -> {
    };
    private int autosaveCounter = 0;
    private long currentFocusSessionSeconds = 0L;

    public PetController(PetDataStore dataStore) {
        this.dataStore = dataStore;
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
            say("你好，我是 wwPet，以后一起专注吧。");
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
        List<String> pool = profile.getState() == PetState.FOCUS ? FOCUS_INTERACTIONS : REST_INTERACTIONS;
        say(randomFrom(pool));
        notifyRefresh();
    }

    public void startFocusUntimed() {
        currentFocusSessionSeconds = 0L;
        profile.setState(PetState.FOCUS);
        profile.setFocusDeadlineEpochMillis(null);
        syncLevel();
        save();
        say("进入专注状态，我会安静陪着你。");
        notifyRefresh();
    }

    public void startTimedFocus(Duration duration) {
        long millis = Math.max(1000L, duration.toMillis());
        currentFocusSessionSeconds = 0L;
        profile.setState(PetState.FOCUS);
        profile.setFocusDeadlineEpochMillis(System.currentTimeMillis() + millis);
        syncLevel();
        save();
        say("专注计时开始啦，我们一起认真一会儿。");
        notifyRefresh();
    }

    public void switchToRest() {
        switchToRest("从专注切回休息状态啦，辛苦啦。");
    }

    public void exitFocusEarly() {
        switchToRest("提前退出专注也没关系，我们稍后再继续。");
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
        say("我已经稳稳停靠在任务栏边上啦。");
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
                switchToRest("专注计时完成，先休息一下吧。");
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
            say("我们累计专注 " + (currentStep * FOCUS_MILESTONE_HOURS) + " 小时啦。");
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
        speechHook.accept(message);
    }

    private String randomFrom(List<String> pool) {
        int index = ThreadLocalRandom.current().nextInt(pool.size());
        return pool.get(index);
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
