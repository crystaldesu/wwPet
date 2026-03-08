package com.wwpet.model;

public enum PetState {
    REST("休息中"),
    FOCUS("专注中");

    private final String displayName;

    PetState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
