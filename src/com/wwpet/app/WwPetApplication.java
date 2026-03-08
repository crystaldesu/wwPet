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
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WwPetApplication {
    private static final long FORCE_EXIT_DELAY_MILLIS = 1500L;
    private static final AtomicBoolean EXITING = new AtomicBoolean(false);

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
            Runnable exitHandler = () -> shutdownApplication(trayManager, petWindow, controller);
            petWindow.setExitHandler(exitHandler);
            trayManager.setExitHandler(exitHandler);

            controller.start();
            boolean trayInstalled = trayManager.install();
            if (startHidden && trayInstalled) {
                petWindow.prepareWindow();
                petWindow.hideToTray();
            } else {
                petWindow.showWindow();
                controller.onWindowShown();
            }
        });
    }

    private static void shutdownApplication(TrayManager trayManager, PetWindow petWindow, PetController controller) {
        if (!EXITING.compareAndSet(false, true)) {
            return;
        }

        Runnable cleanupTask = () -> {
            safeRun(trayManager::remove);
            safeRun(petWindow::shutdownWindow);
            safeRun(controller::shutdown);
        };

        if (SwingUtilities.isEventDispatchThread()) {
            cleanupTask.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(cleanupTask);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                cleanupTask.run();
            } catch (InvocationTargetException ignored) {
                cleanupTask.run();
            }
        }

        Thread forceExitThread = new Thread(() -> {
            try {
                Thread.sleep(FORCE_EXIT_DELAY_MILLIS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            Runtime.getRuntime().halt(0);
        }, "wwPet-force-exit");
        forceExitThread.setDaemon(true);
        forceExitThread.start();
        System.exit(0);
    }

    private static void safeRun(Runnable task) {
        try {
            task.run();
        } catch (Exception ignored) {
        }
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
