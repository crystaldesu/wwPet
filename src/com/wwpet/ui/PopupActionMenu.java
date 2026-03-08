package com.wwpet.ui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public final class PopupActionMenu extends JDialog {
    private static final Color MENU_BACKGROUND = new Color(250, 251, 255);
    private static final Color MENU_BORDER = new Color(210, 216, 226);
    private static final Color MENU_HOVER = new Color(234, 241, 255);
    private static final Color MENU_TEXT = new Color(44, 52, 64);
    private static final Icon EMPTY_MARK_ICON = new MenuMarkIcon(false);
    private static final Icon SELECTED_MARK_ICON = new MenuMarkIcon(true);

    private final int menuWidth;
    private final int rowHeight;
    private final Font menuFont;

    public PopupActionMenu(Window owner, int menuWidth, int rowHeight, Font menuFont) {
        super(owner instanceof Frame frame ? frame : null);
        this.menuWidth = menuWidth;
        this.rowHeight = rowHeight;
        this.menuFont = menuFont;

        setUndecorated(true);
        setModal(false);
        setAlwaysOnTop(true);
        setType(Type.POPUP);
        setFocusableWindowState(true);
        setAutoRequestFocus(true);

        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent event) {
                hideMenu();
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowDeactivated(WindowEvent event) {
                hideMenu();
            }
        });
    }

    public void showMenu(List<MenuEntry> entries, Point screenPoint) {
        rebuild(entries);
        pack();

        Rectangle screenBounds = resolveScreenBounds(screenPoint);
        int x = Math.max(screenBounds.x + 6, Math.min(screenPoint.x, screenBounds.x + screenBounds.width - getWidth() - 6));
        int y = Math.max(screenBounds.y + 6, Math.min(screenPoint.y, screenBounds.y + screenBounds.height - getHeight() - 6));

        setLocation(x, y);
        setVisible(true);
        SwingUtilities.invokeLater(() -> {
            toFront();
            requestFocus();
            requestFocusInWindow();
        });
    }

    public void hideMenu() {
        if (isVisible()) {
            setVisible(false);
        }
    }

    private void rebuild(List<MenuEntry> entries) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(MENU_BACKGROUND);
        panel.setOpaque(true);
        panel.setBorder(BorderFactory.createLineBorder(MENU_BORDER));

        for (MenuEntry entry : entries) {
            if (entry.separatorRow()) {
                panel.add(createSeparator());
            } else {
                panel.add(createButton(entry));
            }
        }

        setContentPane(panel);
    }

    private JButton createButton(MenuEntry entry) {
        JButton button = new JButton(entry.label());
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setHorizontalAlignment(JButton.LEFT);
        button.setFont(menuFont);
        button.setForeground(MENU_TEXT);
        button.setBackground(MENU_BACKGROUND);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setIcon(entry.selected() ? SELECTED_MARK_ICON : EMPTY_MARK_ICON);
        button.setIconTextGap(6);

        Dimension size = new Dimension(menuWidth - 2, rowHeight);
        button.setPreferredSize(size);
        button.setMaximumSize(size);
        button.setMinimumSize(size);

        button.addActionListener(event -> {
            hideMenu();
            if (entry.action() != null) {
                entry.action().run();
            }
        });
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                button.setBackground(MENU_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setBackground(MENU_BACKGROUND);
            }
        });
        return button;
    }

    private JPanel createSeparator() {
        JPanel separator = new JPanel();
        separator.setOpaque(true);
        separator.setBackground(new Color(226, 231, 239));
        separator.setAlignmentX(LEFT_ALIGNMENT);

        Dimension size = new Dimension(menuWidth - 2, 1);
        separator.setPreferredSize(size);
        separator.setMaximumSize(size);
        separator.setMinimumSize(size);
        return separator;
    }

    private Rectangle resolveScreenBounds(Point screenPoint) {
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (GraphicsDevice device : environment.getScreenDevices()) {
            GraphicsConfiguration configuration = device.getDefaultConfiguration();
            Rectangle bounds = configuration.getBounds();
            if (bounds.contains(screenPoint)) {
                return bounds;
            }
        }
        return environment.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
    }

    public record MenuEntry(String label, Runnable action, boolean separatorRow, boolean selected) {
        public static MenuEntry action(String label, Runnable action) {
            return new MenuEntry(label, action, false, false);
        }

        public static MenuEntry selectedAction(String label, boolean selected, Runnable action) {
            return new MenuEntry(label, action, false, selected);
        }

        public static MenuEntry separator() {
            return new MenuEntry("", null, true, false);
        }
    }

    private static final class MenuMarkIcon implements Icon {
        private static final int SIZE = 12;

        private final boolean selected;

        private MenuMarkIcon(boolean selected) {
            this.selected = selected;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            if (!selected) {
                return;
            }

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(MENU_TEXT);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 2, y + 6, x + 5, y + 9);
            g2.drawLine(x + 5, y + 9, x + 10, y + 3);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }
    }
}
