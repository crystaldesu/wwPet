package com.wwpet.ui;

import com.wwpet.model.PetProfile;
import com.wwpet.model.PetState;
import com.wwpet.service.AssetService;
import com.wwpet.service.PetController;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class PetWindow extends JWindow {
    private static final int WINDOW_WIDTH = 176;
    private static final int WINDOW_HEIGHT = 160;
    private static final int SNAP_DISTANCE = 54;
    private static final int DRAG_THRESHOLD = 5;
    private static final int INFO_POPUP_DELAY_MS = 1000;
    private static final int PET_IMAGE_X = 6;
    private static final int PET_IMAGE_Y = 26;
    private static final int PET_IMAGE_SIZE = 108;

    private final PetController controller;
    private final AssetService assetService;
    private final UiTheme uiTheme;
    private final SpeechBubblePopup speechBubblePopup;
    private final PetInfoPopup petInfoPopup;
    private final PetCanvas petCanvas = new PetCanvas();
    private final PopupActionMenu contextMenu;
    private final javax.swing.Timer hoverInfoTimer;
    private final javax.swing.Timer animationTimer;

    private Point pressedScreenPoint;
    private Point pressedWindowPoint;
    private boolean dragging;
    private Boolean appliedAlwaysOnTop;
    private Runnable exitHandler = this::defaultExit;

    public PetWindow(PetController controller, AssetService assetService, UiTheme uiTheme) {
        this.controller = controller;
        this.assetService = assetService;
        this.uiTheme = uiTheme;
        this.speechBubblePopup = new SpeechBubblePopup(this, uiTheme);
        this.petInfoPopup = new PetInfoPopup(this, uiTheme);
        this.contextMenu = new PopupActionMenu(this, 176, 30, uiTheme.getMenuFont());
        this.hoverInfoTimer = new javax.swing.Timer(INFO_POPUP_DELAY_MS, event -> showPetInfoPopupNow());
        this.hoverInfoTimer.setRepeats(false);

        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setBackground(new Color(0, 0, 0, 0));
        setAlwaysOnTop(controller.snapshot().isAlwaysOnTop());

        JPanel content = new JPanel(null);
        content.setOpaque(false);
        petCanvas.setBounds(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        content.add(petCanvas);
        setContentPane(content);

        bindMouseEvents();

        animationTimer = new javax.swing.Timer(90, event -> petCanvas.repaint());
        animationTimer.start();
    }

    public void showWindow() {
        prepareWindow();
        setVisible(true);
        syncAlwaysOnTop(controller.snapshot().isAlwaysOnTop(), true);
        petCanvas.repaint();
    }

    public void prepareWindow() {
        applyInitialLocation();
    }

    public void showFromTray() {
        contextMenu.hideMenu();
        if (!isVisible()) {
            setVisible(true);
        }
        syncAlwaysOnTop(controller.snapshot().isAlwaysOnTop(), true);
        toFront();
        repaint();
    }

    public void hideToTray() {
        contextMenu.hideMenu();
        speechBubblePopup.hideMessage();
        hidePetInfoPopup();
        setVisible(false);
    }

    public void refreshView() {
        syncAlwaysOnTop(controller.snapshot().isAlwaysOnTop(), false);
        if (petInfoPopup.isVisible()) {
            petInfoPopup.showPopup(controller.buildTooltipText());
        }
        petCanvas.repaint();
    }

    public void showSpeechBubble(String message) {
        if (!isVisible()) {
            return;
        }
        speechBubblePopup.showMessage(message);
    }

    public void setExitHandler(Runnable exitHandler) {
        this.exitHandler = exitHandler == null ? this::defaultExit : exitHandler;
    }

    public void shutdownWindow() {
        hoverInfoTimer.stop();
        contextMenu.dispose();
        animationTimer.stop();
        petInfoPopup.dispose();
        speechBubblePopup.dispose();
        dispose();
    }

    private void bindMouseEvents() {
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                schedulePetInfoPopup();
            }

            @Override
            public void mousePressed(MouseEvent event) {
                hidePetInfoPopup();
                maybeShowContextMenu(event);
                if (SwingUtilities.isLeftMouseButton(event)) {
                    pressedScreenPoint = event.getLocationOnScreen();
                    pressedWindowPoint = getLocation();
                    dragging = false;
                    toFront();
                }
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                hidePetInfoPopup();
                if (!SwingUtilities.isLeftMouseButton(event) || pressedScreenPoint == null || pressedWindowPoint == null) {
                    return;
                }
                Point screenPoint = event.getLocationOnScreen();
                int deltaX = screenPoint.x - pressedScreenPoint.x;
                int deltaY = screenPoint.y - pressedScreenPoint.y;
                if (Math.abs(deltaX) > DRAG_THRESHOLD || Math.abs(deltaY) > DRAG_THRESHOLD) {
                    dragging = true;
                }
                int newX = pressedWindowPoint.x + deltaX;
                int newY = pressedWindowPoint.y + deltaY;
                setLocation(newX, newY);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                maybeShowContextMenu(event);
                if (SwingUtilities.isLeftMouseButton(event)) {
                    if (dragging) {
                        boolean snapped = snapToTaskbarIfNeeded();
                        controller.updateWindowLocation(getLocation());
                        if (snapped) {
                            controller.markSnappedToTaskbar();
                        }
                    } else {
                        controller.interact();
                    }
                    pressedScreenPoint = null;
                    pressedWindowPoint = null;
                    dragging = false;
                }
            }

            @Override
            public void mouseMoved(MouseEvent event) {
                if (!petInfoPopup.isVisible()) {
                    schedulePetInfoPopup();
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hidePetInfoPopup();
            }
        };
        petCanvas.addMouseListener(adapter);
        petCanvas.addMouseMotionListener(adapter);
    }

    private void maybeShowContextMenu(MouseEvent event) {
        if (!event.isPopupTrigger()) {
            return;
        }
        hidePetInfoPopup();
        contextMenu.showMenu(buildContextEntries(), event.getLocationOnScreen());
    }

    private void schedulePetInfoPopup() {
        if (!isVisible() || contextMenu.isVisible()) {
            return;
        }
        if (petInfoPopup.isVisible()) {
            return;
        }
        hoverInfoTimer.restart();
    }

    private void showPetInfoPopupNow() {
        hoverInfoTimer.stop();
        if (!isVisible() || contextMenu.isVisible()) {
            return;
        }
        petInfoPopup.showPopup(controller.buildTooltipText());
    }

    private void hidePetInfoPopup() {
        hoverInfoTimer.stop();
        petInfoPopup.hidePopup();
    }

    private List<PopupActionMenu.MenuEntry> buildContextEntries() {
        List<PopupActionMenu.MenuEntry> entries = new ArrayList<>();
        if (controller.isTimedFocusActive()) {
            entries.add(PopupActionMenu.MenuEntry.action("提前退出专注状态", controller::exitFocusEarly));
        } else if (controller.getState() == PetState.REST) {
            entries.add(PopupActionMenu.MenuEntry.action("切换到专注状态", controller::startFocusUntimed));
        } else {
            entries.add(PopupActionMenu.MenuEntry.action("切换到休息状态", controller::switchToRest));
        }

        entries.add(PopupActionMenu.MenuEntry.action(
                controller.isTimedFocusActive() ? "重新设置专注定时..." : "专注定时...",
                () -> {
                    Duration duration = FocusTimerDialog.showDialog(this, uiTheme);
                    if (duration != null) {
                        controller.startTimedFocus(duration);
                    }
                }
        ));
        entries.add(PopupActionMenu.MenuEntry.separator());
        entries.add(PopupActionMenu.MenuEntry.selectedAction(
                "窗口最前",
                controller.snapshot().isAlwaysOnTop(),
                () -> controller.toggleAlwaysOnTop(!controller.snapshot().isAlwaysOnTop())
        ));
        entries.add(PopupActionMenu.MenuEntry.action("隐藏到托盘", this::hideToTray));
        entries.add(PopupActionMenu.MenuEntry.separator());
        entries.add(PopupActionMenu.MenuEntry.action("退出", exitHandler));
        return entries;
    }

    private void syncAlwaysOnTop(boolean alwaysOnTop, boolean forceFront) {
        boolean changed = appliedAlwaysOnTop == null || appliedAlwaysOnTop != alwaysOnTop;
        appliedAlwaysOnTop = alwaysOnTop;

        Runnable applyTask = () -> {
            if (isAlwaysOnTop() != alwaysOnTop) {
                setAlwaysOnTop(alwaysOnTop);
            }
            petInfoPopup.syncAlwaysOnTop(alwaysOnTop);
            speechBubblePopup.syncAlwaysOnTop(alwaysOnTop);
            if (alwaysOnTop && (changed || forceFront)) {
                toFront();
                repaint();
            }
        };
        SwingUtilities.invokeLater(applyTask);
    }

    private void defaultExit() {
        shutdownWindow();
        controller.shutdown();
        System.exit(0);
    }

    private void applyInitialLocation() {
        PetProfile profile = controller.snapshot();
        Rectangle workArea = getWorkArea();
        int x = profile.getWindowX();
        int y = profile.getWindowY();

        if (x < 0 || y < 0) {
            x = workArea.x + workArea.width - getWidth() - 24;
            y = workArea.y + workArea.height - getHeight() - 12;
        }

        x = clamp(x, workArea.x, workArea.x + workArea.width - getWidth());
        y = clamp(y, workArea.y, workArea.y + workArea.height - getHeight());
        setLocation(x, y);
        controller.updateWindowLocation(getLocation());
    }

    private boolean snapToTaskbarIfNeeded() {
        GraphicsConfiguration configuration = getGraphicsConfiguration();
        Rectangle screenBounds = configuration.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        Rectangle workArea = new Rectangle(
                screenBounds.x + insets.left,
                screenBounds.y + insets.top,
                screenBounds.width - insets.left - insets.right,
                screenBounds.height - insets.top - insets.bottom
        );

        int x = getX();
        int y = getY();
        boolean snapped = false;

        if (insets.bottom > 0 && Math.abs((y + getHeight()) - (workArea.y + workArea.height)) <= SNAP_DISTANCE) {
            y = workArea.y + workArea.height - getHeight();
            snapped = true;
        } else if (insets.top > 0 && Math.abs(y - workArea.y) <= SNAP_DISTANCE) {
            y = workArea.y;
            snapped = true;
        }

        if (insets.left > 0 && Math.abs(x - workArea.x) <= SNAP_DISTANCE) {
            x = workArea.x;
            snapped = true;
        } else if (insets.right > 0 && Math.abs((x + getWidth()) - (workArea.x + workArea.width)) <= SNAP_DISTANCE) {
            x = workArea.x + workArea.width - getWidth();
            snapped = true;
        }

        x = clamp(x, workArea.x, workArea.x + workArea.width - getWidth());
        y = clamp(y, workArea.y, workArea.y + workArea.height - getHeight());
        setLocation(x, y);
        return snapped;
    }

    private Rectangle getWorkArea() {
        GraphicsConfiguration configuration = getGraphicsConfiguration();
        Rectangle screenBounds = configuration.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        return new Rectangle(
                screenBounds.x + insets.left,
                screenBounds.y + insets.top,
                screenBounds.width - insets.left - insets.right,
                screenBounds.height - insets.top - insets.bottom
        );
    }

    private int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private final class PetCanvas extends JComponent {
        private PetCanvas() {
            setOpaque(false);
            setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            PetProfile profile = controller.snapshot();
            PetState state = profile.getState();
            BufferedImage petImage = assetService.loadPetImage(state);
            int offsetY = (int) Math.round(Math.sin(System.currentTimeMillis() / 240.0) * 4.0);

            g2.setColor(new Color(0, 0, 0, 28));
            g2.fill(new Ellipse2D.Double(18, 124, 84, 18));

            if (petImage != null) {
                int drawSize = PET_IMAGE_SIZE;
                int x = PET_IMAGE_X;
                int y = PET_IMAGE_Y + offsetY;
                g2.drawImage(petImage, x, y, drawSize, drawSize, null);
            } else {
                paintPlaceholderPet(g2, state, profile.getLevel(), offsetY);
            }

            paintStackedBadges(g2, profile.getLevel(), state, controller.isFocusStateActive() ? controller.getCurrentFocusClock() : null);
            g2.dispose();
        }

        private void paintPlaceholderPet(Graphics2D g2, PetState state, int level, int offsetY) {
            Color bodyTop = state == PetState.FOCUS ? new Color(111, 181, 255) : new Color(255, 199, 126);
            Color bodyBottom = state == PetState.FOCUS ? new Color(74, 136, 241) : new Color(255, 157, 93);
            int x = 16;
            int y = 30 + offsetY;
            int bodyWidth = 96;
            int bodyHeight = 98;

            g2.setPaint(new GradientPaint(0, y, bodyTop, 0, y + bodyHeight, bodyBottom));
            g2.fillRoundRect(x, y, bodyWidth, bodyHeight, 54, 54);

            g2.setColor(new Color(255, 255, 255, 70));
            g2.fillOval(x + 16, y + 10, 28, 16);

            g2.setColor(new Color(45, 50, 60));
            g2.fillOval(x + 24, y + 38, 10, 14);
            g2.fillOval(x + 62, y + 38, 10, 14);

            g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (state == PetState.FOCUS) {
                g2.drawLine(x + 18, y + 30, x + 38, y + 28);
                g2.drawLine(x + 58, y + 28, x + 78, y + 30);
                g2.drawArc(x + 38, y + 50, 20, 12, 180, 180);
            } else {
                g2.drawArc(x + 38, y + 50, 20, 16, 180, -180);
            }

            g2.setColor(new Color(255, 255, 255, 160));
            g2.fillOval(x + 10, y + 78, 16, 10);
            g2.fillOval(x + 70, y + 78, 16, 10);

            g2.setColor(new Color(255, 255, 255, 220));
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawRoundRect(x, y, bodyWidth, bodyHeight, 54, 54);

            if (level >= 5) {
                g2.setColor(new Color(255, 245, 160, 180));
                g2.fillOval(x + 74, y + 12, 10, 10);
            }
        }

        private void paintStackedBadges(Graphics2D g2, int level, PetState state, String timerText) {
            int topY = 2;
            int rightX = getWidth() - 6;
            int gap = 2;

            int currentY = topY;
            currentY = paintBadge(g2, rightX, currentY, "Lv." + level);
            currentY += gap;
            currentY = paintBadge(g2, rightX, currentY, state == PetState.FOCUS ? "Focus" : "Rest");
            if (timerText != null) {
                currentY += gap;
                paintBadge(g2, rightX, currentY, timerText);
            }
        }

        private int paintBadge(Graphics2D g2, int rightX, int topY, String text) {
            Font badgeFont = uiTheme.getBadgeFont();
            g2.setFont(badgeFont);
            FontMetrics metrics = g2.getFontMetrics();
            int badgeWidth = Math.max(42, metrics.stringWidth(text) + 14);
            int badgeHeight = 18;
            int badgeX = rightX - badgeWidth;

            g2.setColor(new Color(32, 38, 47, 180));
            g2.fillRoundRect(badgeX, topY, badgeWidth, badgeHeight, 12, 12);
            g2.setColor(Color.WHITE);
            g2.drawString(text, badgeX + (badgeWidth - metrics.stringWidth(text)) / 2, topY + 13);
            return topY + badgeHeight;
        }
    }
}
