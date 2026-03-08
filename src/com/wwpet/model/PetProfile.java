package com.wwpet.model;

public final class PetProfile {
    private String name = "wwPet";
    private int level = 1;
    private long totalFocusSeconds = 0L;
    private long companionDays = 1L;
    private PetState state = PetState.REST;
    private boolean alwaysOnTop = true;
    private int windowX = -1;
    private int windowY = -1;
    private String firstSeenDate = "";
    private String lastOpenedDate = "";
    private Long focusDeadlineEpochMillis;
    private long lastMilestoneStep = 0L;

    public PetProfile copy() {
        PetProfile copy = new PetProfile();
        copy.name = name;
        copy.level = level;
        copy.totalFocusSeconds = totalFocusSeconds;
        copy.companionDays = companionDays;
        copy.state = state;
        copy.alwaysOnTop = alwaysOnTop;
        copy.windowX = windowX;
        copy.windowY = windowY;
        copy.firstSeenDate = firstSeenDate;
        copy.lastOpenedDate = lastOpenedDate;
        copy.focusDeadlineEpochMillis = focusDeadlineEpochMillis;
        copy.lastMilestoneStep = lastMilestoneStep;
        return copy;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getTotalFocusSeconds() {
        return totalFocusSeconds;
    }

    public void setTotalFocusSeconds(long totalFocusSeconds) {
        this.totalFocusSeconds = totalFocusSeconds;
    }

    public long getCompanionDays() {
        return companionDays;
    }

    public void setCompanionDays(long companionDays) {
        this.companionDays = companionDays;
    }

    public PetState getState() {
        return state;
    }

    public void setState(PetState state) {
        this.state = state;
    }

    public boolean isAlwaysOnTop() {
        return alwaysOnTop;
    }

    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.alwaysOnTop = alwaysOnTop;
    }

    public int getWindowX() {
        return windowX;
    }

    public void setWindowX(int windowX) {
        this.windowX = windowX;
    }

    public int getWindowY() {
        return windowY;
    }

    public void setWindowY(int windowY) {
        this.windowY = windowY;
    }

    public String getFirstSeenDate() {
        return firstSeenDate;
    }

    public void setFirstSeenDate(String firstSeenDate) {
        this.firstSeenDate = firstSeenDate;
    }

    public String getLastOpenedDate() {
        return lastOpenedDate;
    }

    public void setLastOpenedDate(String lastOpenedDate) {
        this.lastOpenedDate = lastOpenedDate;
    }

    public Long getFocusDeadlineEpochMillis() {
        return focusDeadlineEpochMillis;
    }

    public void setFocusDeadlineEpochMillis(Long focusDeadlineEpochMillis) {
        this.focusDeadlineEpochMillis = focusDeadlineEpochMillis;
    }

    public long getLastMilestoneStep() {
        return lastMilestoneStep;
    }

    public void setLastMilestoneStep(long lastMilestoneStep) {
        this.lastMilestoneStep = lastMilestoneStep;
    }
}
