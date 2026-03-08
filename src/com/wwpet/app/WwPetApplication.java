package com.wwpet.app;

import com.wwpet.service.AssetService;
import com.wwpet.service.PetController;
import com.wwpet.service.PetDataStore;
import com.wwpet.service.SpeechLinesStore;
import com.wwpet.service.UiSettingsStore;
import com.wwpet.ui.PetWindow;
import com.wwpet.ui.TrayManager;
import com.wwpet.ui.UiTheme;

import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class WwPetApplication {
    private WwPetApplication() {
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            installLookAndFeel();
            ToolTipManager.sharedInstance().setInitialDelay(120);
            ToolTipManager.sharedInstance().setDismissDelay(8000);

            Path rootPath = resolveAppRootPath();
            UiSettingsStore settingsStore = new UiSettingsStore(rootPath.resolve("data").resolve("settings.json"));
            SpeechLinesStore speechLinesStore = new SpeechLinesStore(rootPath.resolve("data").resolve("speech-lines.json"));
            UiTheme uiTheme = UiTheme.from(settingsStore.load(), rootPath);
            UIManager.put("ToolTip.font", uiTheme.getDialogTextFont());

            AssetService assetService = new AssetService(rootPath.resolve("data").resolve("assets"));
            PetDataStore dataStore = new PetDataStore(rootPath.resolve("data").resolve("pet-data.json"));
            PetController controller = new PetController(dataStore, speechLinesStore.load());
            PetWindow petWindow = new PetWindow(controller, assetService, uiTheme);
            TrayManager trayManager = new TrayManager(assetService, controller, petWindow, uiTheme);
            boolean startHidden = Boolean.getBoolean("wwpet.start.hidden");

            controller.setUiHooks(petWindow::refreshView, petWindow::showSpeechBubble);
            petWindow.setExitHandler(() -> {
                trayManager.remove();
                petWindow.shutdownWindow();
                controller.shutdown();
                System.exit(0);
            });

            controller.start();
            boolean trayInstalled = trayManager.install();
            if (startHidden && trayInstalled) {
                petWindow.prepareWindow();
                petWindow.hideToTray();
            } else {
                petWindow.showWindow();
                controller.onWindowShown();
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                trayManager.remove();
                controller.shutdown();
            }, "wwPet-shutdown"));
        });
    }

    private static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private static Path resolveAppRootPath() {
        String packagedLauncherPath = System.getProperty("jpackage.app-path");
        if (packagedLauncherPath != null && !packagedLauncherPath.isBlank()) {
            Path launcherPath = Paths.get(packagedLauncherPath).toAbsolutePath();
            Path launcherDir = launcherPath.getParent();
            if (launcherDir != null) {
                Path appDir = launcherDir.resolve("app");
                if (Files.isDirectory(appDir)) {
                    return appDir;
                }
                return launcherDir;
            }
        }
        return Paths.get(System.getProperty("user.dir")).toAbsolutePath();
    }
}
