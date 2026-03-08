package com.wwpet.model;

public final class UiSettings {
    private String fontFamily = "Microsoft YaHei UI";
    private String fontFile = "";
    private int menuFontSize = 13;
    private int badgeFontSize = 12;
    private int dialogTitleFontSize = 18;
    private int dialogTextFontSize = 13;
    private int dialogInputFontSize = 15;

    public UiSettings copy() {
        UiSettings copy = new UiSettings();
        copy.fontFamily = fontFamily;
        copy.fontFile = fontFile;
        copy.menuFontSize = menuFontSize;
        copy.badgeFontSize = badgeFontSize;
        copy.dialogTitleFontSize = dialogTitleFontSize;
        copy.dialogTextFontSize = dialogTextFontSize;
        copy.dialogInputFontSize = dialogInputFontSize;
        return copy;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public String getFontFile() {
        return fontFile;
    }

    public void setFontFile(String fontFile) {
        this.fontFile = fontFile;
    }

    public int getMenuFontSize() {
        return menuFontSize;
    }

    public void setMenuFontSize(int menuFontSize) {
        this.menuFontSize = menuFontSize;
    }

    public int getBadgeFontSize() {
        return badgeFontSize;
    }

    public void setBadgeFontSize(int badgeFontSize) {
        this.badgeFontSize = badgeFontSize;
    }

    public int getDialogTitleFontSize() {
        return dialogTitleFontSize;
    }

    public void setDialogTitleFontSize(int dialogTitleFontSize) {
        this.dialogTitleFontSize = dialogTitleFontSize;
    }

    public int getDialogTextFontSize() {
        return dialogTextFontSize;
    }

    public void setDialogTextFontSize(int dialogTextFontSize) {
        this.dialogTextFontSize = dialogTextFontSize;
    }

    public int getDialogInputFontSize() {
        return dialogInputFontSize;
    }

    public void setDialogInputFontSize(int dialogInputFontSize) {
        this.dialogInputFontSize = dialogInputFontSize;
    }
}
