package com.wwpet.ui;

import com.wwpet.model.PetState;
import com.wwpet.service.AssetService;
import com.wwpet.service.PetController;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public final class TrayManager {
    private final AssetService assetService;
    private final PetController controller;
    private final PetWindow petWindow;
    private final java.awt.Font menuFont;

    private PopupActionMenu trayMenu;
    private TrayIcon trayIcon;
    private Runnable exitHandler = this::defaultExit;

    public TrayManager(AssetService assetService, PetController controller, PetWindow petWindow, UiTheme uiTheme) {
        this.assetService = assetService;
        this.controller = controller;
        this.petWindow = petWindow;
        this.menuFont = uiTheme.getMenuFont();
    }

    public boolean install() {
        if (!SystemTray.isSupported()) {
            return false;
        }
        if (trayIcon != null) {
            return true;
        }

        trayIcon = new TrayIcon(createTrayImage(), "wwPet");
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                handleTrayMouse(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                handleTrayMouse(event);
            }
        });

        try {
            SystemTray.getSystemTray().add(trayIcon);
            trayIcon.displayMessage("wwPet", "wwPet 已驻留在系统托盘。", TrayIcon.MessageType.NONE);
            return true;
        } catch (Exception ignored) {
            trayIcon = null;
            return false;
        }
    }

    public void setExitHandler(Runnable exitHandler) {
        this.exitHandler = exitHandler == null ? this::defaultExit : exitHandler;
    }

    public void remove() {
        if (trayMenu != null) {
            trayMenu.hideMenu();
            trayMenu.dispose();
            trayMenu = null;
        }
        if (trayIcon == null) {
            return;
        }
        try {
            SystemTray.getSystemTray().remove(trayIcon);
        } catch (Exception ignored) {
        }
        trayIcon = null;
    }

    private void handleTrayMouse(MouseEvent event) {
        if (event.isPopupTrigger() || event.getButton() == MouseEvent.BUTTON3) {
            getOrCreateTrayMenu().showMenu(buildTrayEntries(), resolveCursorLocation());
            return;
        }
        if (event.getButton() == MouseEvent.BUTTON1) {
            if (trayMenu != null) {
                trayMenu.hideMenu();
            }
            petWindow.showFromTray();
        }
    }

    private List<PopupActionMenu.MenuEntry> buildTrayEntries() {
        List<PopupActionMenu.MenuEntry> entries = new ArrayList<>();
        entries.add(PopupActionMenu.MenuEntry.action("显示桌宠", petWindow::showFromTray));
        entries.add(PopupActionMenu.MenuEntry.action("隐藏到托盘", petWindow::hideToTray));
        entries.add(PopupActionMenu.MenuEntry.separator());
        entries.add(PopupActionMenu.MenuEntry.action("切换到专注状态", () -> {
            controller.startFocusUntimed();
            petWindow.showFromTray();
        }));
        entries.add(PopupActionMenu.MenuEntry.action("切换到休息状态", () -> {
            controller.switchToRest();
            petWindow.showFromTray();
        }));
        entries.add(PopupActionMenu.MenuEntry.separator());
        entries.add(PopupActionMenu.MenuEntry.selectedAction(
                "窗口最前",
                controller.isAlwaysOnTop(),
                () -> controller.toggleAlwaysOnTop(!controller.isAlwaysOnTop())
        ));
        entries.add(PopupActionMenu.MenuEntry.separator());
        entries.add(PopupActionMenu.MenuEntry.action("退出", exitHandler));
        return entries;
    }

    private Point resolveCursorLocation() {
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo != null) {
            return pointerInfo.getLocation();
        }
        return new Point(120, 120);
    }

    private Image createTrayImage() {
        BufferedImage appIcon = assetService.loadAppIcon(16, 16);
        if (appIcon != null) {
            return appIcon;
        }

        BufferedImage source = assetService.loadPetImage(PetState.REST, 16, 16);
        if (source != null) {
            return source;
        }

        BufferedImage fallback = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = fallback.createGraphics();
        g2.setColor(new Color(255, 183, 110));
        g2.fillRoundRect(1, 1, 14, 14, 6, 6);
        g2.setColor(new Color(69, 76, 89));
        g2.drawRoundRect(1, 1, 14, 14, 6, 6);
        g2.fillOval(4, 5, 2, 2);
        g2.fillOval(10, 5, 2, 2);
        g2.drawArc(4, 7, 8, 5, 200, 140);
        g2.dispose();
        return fallback;
    }

    private PopupActionMenu getOrCreateTrayMenu() {
        if (trayMenu == null) {
            trayMenu = new PopupActionMenu(null, 154, 30, menuFont);
        }
        return trayMenu;
    }

    private void defaultExit() {
        remove();
        petWindow.shutdownWindow();
        controller.shutdown();
        System.exit(0);
    }
}
