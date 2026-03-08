package com.wwpet.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Window;
import java.time.Duration;

public final class FocusTimerDialog extends JDialog {
    private final JTextField hourField = new JTextField("0", 6);
    private final JTextField minuteField = new JTextField("25", 6);
    private final JLabel errorLabel = new JLabel(" ");
    private final UiTheme uiTheme;
    private final Window relativeOwner;
    private Duration result;

    private FocusTimerDialog(Window owner, UiTheme uiTheme) {
        super(owner instanceof Frame ? (Frame) owner : null, "设置专注定时", ModalityType.APPLICATION_MODAL);
        this.relativeOwner = owner;
        this.uiTheme = uiTheme;
        buildUi();
    }

    public static Duration showDialog(Window owner, UiTheme uiTheme) {
        FocusTimerDialog dialog = new FocusTimerDialog(owner, uiTheme);
        dialog.setVisible(true);
        return dialog.result;
    }

    private void buildUi() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(new Color(247, 249, 255));
        content.setBorder(BorderFactory.createEmptyBorder(18, 18, 16, 18));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("设置专注定时");
        titleLabel.setFont(uiTheme.getDialogTitleFont());
        titleLabel.setForeground(new Color(41, 47, 60));

        JLabel subtitleLabel = new JLabel("输入小时和分钟，开始一段专注时光");
        subtitleLabel.setFont(uiTheme.getDialogTextFont());
        subtitleLabel.setForeground(new Color(112, 120, 136));

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitleLabel);

        JPanel fields = new JPanel(new GridLayout(1, 2, 12, 0));
        fields.setOpaque(false);
        fields.add(createFieldCard("小时", hourField));
        fields.add(createFieldCard("分钟", minuteField));

        JPanel hintCard = new JPanel(new BorderLayout());
        hintCard.setBackground(new Color(237, 244, 255));
        hintCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(214, 227, 247)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel hintLabel = new JLabel("支持 0-99 小时、0-59 分钟，例如 0 小时 25 分钟。");
        hintLabel.setFont(uiTheme.getDialogTextFont());
        hintLabel.setForeground(new Color(85, 101, 129));
        hintCard.add(hintLabel, BorderLayout.CENTER);

        errorLabel.setFont(uiTheme.getDialogTextFont());
        errorLabel.setHorizontalAlignment(SwingConstants.LEFT);
        errorLabel.setForeground(new Color(192, 58, 43));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);

        JButton cancelButton = createActionButton("取消", new Color(236, 240, 248), new Color(78, 86, 99));
        JButton confirmButton = createActionButton("开始专注", new Color(97, 151, 255), Color.WHITE);

        cancelButton.addActionListener(event -> dispose());
        confirmButton.addActionListener(event -> confirm());
        getRootPane().setDefaultButton(confirmButton);

        buttons.add(cancelButton);
        buttons.add(confirmButton);

        content.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(fields);
        center.add(Box.createVerticalStrut(12));
        center.add(hintCard);
        content.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.add(errorLabel);
        bottom.add(Box.createVerticalStrut(10));
        bottom.add(buttons);
        content.add(bottom, BorderLayout.SOUTH);

        setContentPane(content);
        pack();
        setLocationRelativeTo(relativeOwner);
    }

    private JPanel createFieldCard(String labelText, JTextField textField) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 226, 236)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        card.setPreferredSize(new Dimension(146, 92));

        JLabel label = new JLabel(labelText);
        label.setFont(uiTheme.getDialogTextFont());
        label.setForeground(new Color(98, 106, 120));

        textField.setFont(uiTheme.getDialogInputFont());
        textField.setHorizontalAlignment(SwingConstants.CENTER);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(208, 214, 226)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        textField.setBackground(new Color(249, 250, 255));

        card.add(label, BorderLayout.NORTH);
        card.add(textField, BorderLayout.CENTER);
        return card;
    }

    private JButton createActionButton(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setFont(uiTheme.getDialogTextFont());
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(96, 34));
        return button;
    }

    private void confirm() {
        String hourText = hourField.getText().trim();
        String minuteText = minuteField.getText().trim();

        int hours;
        int minutes;
        try {
            hours = Integer.parseInt(hourText);
            minutes = Integer.parseInt(minuteText);
        } catch (NumberFormatException exception) {
            errorLabel.setText("请输入合法整数。");
            pack();
            return;
        }

        if (hours < 0 || hours > 99) {
            errorLabel.setText("小时必须在 0 到 99 之间。");
            pack();
            return;
        }
        if (minutes < 0 || minutes > 59) {
            errorLabel.setText("分钟必须在 0 到 59 之间。");
            pack();
            return;
        }
        if (hours == 0 && minutes == 0) {
            errorLabel.setText("专注时间不能为 0。");
            pack();
            return;
        }

        result = Duration.ofHours(hours).plusMinutes(minutes);
        dispose();
    }
}
