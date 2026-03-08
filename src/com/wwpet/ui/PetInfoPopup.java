package com.wwpet.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
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

public final class PetInfoPopup {
    private final JWindow owner;
    private final JWindow popupWindow;
    private final JLabel contentLabel;

    public PetInfoPopup(JWindow owner, UiTheme uiTheme) {
        this.owner = owner;
        this.popupWindow = new JWindow(owner);
        this.popupWindow.setBackground(new Color(0, 0, 0, 0));
        this.popupWindow.setFocusableWindowState(false);

        InfoPanel panel = new InfoPanel();
        this.contentLabel = new JLabel("", SwingConstants.LEFT);
        this.contentLabel.setFont(uiTheme.getDialogTextFont().deriveFont(Math.max(11f, uiTheme.getDialogTextFont().getSize2D() - 1f)));
        this.contentLabel.setForeground(new Color(51, 59, 73));
        this.contentLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        panel.add(contentLabel);
        popupWindow.setContentPane(panel);
    }

    public void showPopup(String content) {
        if (content == null || content.isBlank() || !owner.isVisible()) {
            return;
        }

        contentLabel.setText(content);
        popupWindow.pack();

        Point ownerPoint = owner.getLocationOnScreen();
        Rectangle bounds = getVisibleBounds();

        int x = ownerPoint.x + owner.getWidth() - popupWindow.getWidth() + 8;
        int y = ownerPoint.y - popupWindow.getHeight() + 18;

        x = Math.max(bounds.x + 8, Math.min(x, bounds.x + bounds.width - popupWindow.getWidth() - 8));
        if (y < bounds.y + 8) {
            y = Math.min(ownerPoint.y + 12, bounds.y + bounds.height - popupWindow.getHeight() - 8);
        }

        popupWindow.setLocation(x, y);
        popupWindow.setAlwaysOnTop(owner.isAlwaysOnTop());
        if (!popupWindow.isVisible()) {
            popupWindow.setVisible(true);
        } else {
            popupWindow.repaint();
        }
    }

    public void hidePopup() {
        if (popupWindow.isVisible()) {
            popupWindow.setVisible(false);
        }
    }

    public boolean isVisible() {
        return popupWindow.isVisible();
    }

    public void syncAlwaysOnTop(boolean alwaysOnTop) {
        popupWindow.setAlwaysOnTop(alwaysOnTop);
        if (alwaysOnTop && popupWindow.isVisible()) {
            popupWindow.toFront();
        }
    }

    public void dispose() {
        popupWindow.dispose();
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

    private static final class InfoPanel extends JPanel {
        private InfoPanel() {
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension preferred = super.getPreferredSize();
            return new Dimension(preferred.width + 4, preferred.height + 4);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            g2.setColor(new Color(0, 0, 0, 20));
            g2.fillRoundRect(2, 2, width - 4, height - 4, 12, 12);

            g2.setColor(new Color(255, 255, 255, 242));
            g2.fillRoundRect(0, 0, width - 4, height - 4, 12, 12);

            g2.setColor(new Color(222, 228, 237));
            g2.drawRoundRect(0, 0, width - 5, height - 5, 12, 12);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }
}
