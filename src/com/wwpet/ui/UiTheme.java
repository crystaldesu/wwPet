package com.wwpet.ui;

import com.wwpet.model.UiSettings;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class UiTheme {
    private final Font menuFont;
    private final Font badgeFont;
    private final Font dialogTitleFont;
    private final Font dialogTextFont;
    private final Font dialogInputFont;

    private UiTheme(Font menuFont, Font badgeFont, Font dialogTitleFont, Font dialogTextFont, Font dialogInputFont) {
        this.menuFont = menuFont;
        this.badgeFont = badgeFont;
        this.dialogTitleFont = dialogTitleFont;
        this.dialogTextFont = dialogTextFont;
        this.dialogInputFont = dialogInputFont;
    }

    public static UiTheme from(UiSettings settings, Path rootPath) {
        Font baseFont = resolveBaseFont(settings, rootPath);
        return new UiTheme(
                baseFont.deriveFont((float) settings.getMenuFontSize()),
                baseFont.deriveFont((float) settings.getBadgeFontSize()),
                baseFont.deriveFont(Font.BOLD, (float) settings.getDialogTitleFontSize()),
                baseFont.deriveFont((float) settings.getDialogTextFontSize()),
                baseFont.deriveFont((float) settings.getDialogInputFontSize())
        );
    }

    private static Font resolveBaseFont(UiSettings settings, Path rootPath) {
        String fontFile = settings.getFontFile() == null ? "" : settings.getFontFile().trim();
        if (!fontFile.isBlank()) {
            Path fontPath = rootPath.resolve(fontFile).normalize();
            if (Files.exists(fontPath)) {
                try (InputStream inputStream = Files.newInputStream(fontPath)) {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, inputStream);
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                    return font.deriveFont(Font.PLAIN, 13f);
                } catch (Exception ignored) {
                }
            }
        }

        String fontFamily = settings.getFontFamily();
        if (fontFamily == null || fontFamily.isBlank()) {
            fontFamily = "Microsoft YaHei UI";
        }
        return new Font(fontFamily, Font.PLAIN, 13);
    }

    public Font getMenuFont() {
        return menuFont;
    }

    public Font getBadgeFont() {
        return badgeFont;
    }

    public Font getDialogTitleFont() {
        return dialogTitleFont;
    }

    public Font getDialogTextFont() {
        return dialogTextFont;
    }

    public Font getDialogInputFont() {
        return dialogInputFont;
    }
}
