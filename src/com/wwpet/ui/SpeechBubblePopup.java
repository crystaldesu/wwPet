package com.wwpet.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;

public final class SpeechBubblePopup {
    private final JWindow owner;
    private final JWindow bubbleWindow;
    private final JLabel messageLabel;
    private Timer hideTimer;

    public SpeechBubblePopup(JWindow owner, UiTheme uiTheme) {
        this.owner = owner;
        this.bubbleWindow = new JWindow(owner);
        this.bubbleWindow.setBackground(new Color(0, 0, 0, 0));

        BubblePanel panel = new BubblePanel();
        this.messageLabel = new JLabel("", SwingConstants.CENTER);
        this.messageLabel.setFont(uiTheme.getDialogTextFont());
        this.messageLabel.setForeground(new Color(56, 63, 75));
        this.messageLabel.setBorder(BorderFactory.createEmptyBorder(10, 14, 16, 14));
        panel.add(messageLabel);
        bubbleWindow.setContentPane(panel);
    }

    public void showMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        if (hideTimer != null && hideTimer.isRunning()) {
            hideTimer.stop();
        }

        messageLabel.setText("<html><div style='text-align:center;'>" + escape(message) + "</div></html>");
        bubbleWindow.pack();
        Point point = owner.getLocationOnScreen();
        int x = point.x + owner.getWidth() / 2 - bubbleWindow.getWidth() / 2;
        int y = point.y - bubbleWindow.getHeight() + 14;
        Rectangle bounds = getVisibleBounds();
        x = Math.max(bounds.x + 8, Math.min(x, bounds.x + bounds.width - bubbleWindow.getWidth() - 8));
        y = Math.max(bounds.y + 8, y);
        bubbleWindow.setLocation(x, y);
        bubbleWindow.setAlwaysOnTop(owner.isAlwaysOnTop());
        bubbleWindow.setVisible(true);

        int visibleMillis = Math.max(2200, Math.min(4200, 1800 + message.length() * 60));
        hideTimer = new Timer(visibleMillis, event -> bubbleWindow.setVisible(false));
        hideTimer.setRepeats(false);
        hideTimer.start();
    }

    public void syncAlwaysOnTop(boolean alwaysOnTop) {
        bubbleWindow.setAlwaysOnTop(alwaysOnTop);
        if (alwaysOnTop && bubbleWindow.isVisible()) {
            bubbleWindow.toFront();
        }
    }

    public void hideMessage() {
        if (hideTimer != null) {
            hideTimer.stop();
        }
        bubbleWindow.setVisible(false);
    }

    public void dispose() {
        if (hideTimer != null) {
            hideTimer.stop();
        }
        bubbleWindow.dispose();
    }

    private String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private Rectangle getVisibleBounds() {
        GraphicsConfiguration configuration = owner.getGraphicsConfiguration();
        Rectangle screenBounds = configuration.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        return new Rectangle(
                screenBounds.x + insets.left,
                screenBounds.y + insets.top,
                screenBounds.width - insets.left - insets.right,
                screenBounds.height - insets.top - insets.bottom
        );
    }

    private static final class BubblePanel extends JPanel {
        private BubblePanel() {
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension preferred = super.getPreferredSize();
            return new Dimension(preferred.width + 8, preferred.height + 8);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            g2.setColor(new Color(0, 0, 0, 28));
            g2.fillRoundRect(4, 4, width - 8, height - 12, 20, 20);

            g2.setColor(new Color(255, 255, 255, 236));
            g2.fillRoundRect(0, 0, width - 8, height - 14, 20, 20);

            int tailX = width / 2 - 10;
            int[] xPoints = {tailX, tailX + 10, tailX + 20};
            int[] yPoints = {height - 14, height - 2, height - 14};
            g2.fillPolygon(xPoints, yPoints, 3);

            g2.setColor(new Color(225, 230, 240));
            g2.drawRoundRect(0, 0, width - 9, height - 15, 20, 20);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }
}
