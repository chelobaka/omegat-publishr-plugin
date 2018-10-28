/**************************************************************************
 PublishR file filter for OmegaT

 Copyright (C) 2018 Lev Abashkin

 This file is NOT a part of OmegaT.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package com.pilulerouge.publishr.omegat;

import org.omegat.util.OStrings;
import org.openide.awt.Mnemonics;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;


/**
 * Settings dialog in OmegaT.
 */
final class SettingsDialog extends JDialog {

    private Map<String, String> options;

    private JPanel panel;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox plainFootnotesCheckBox;
    private JLabel tagColorLabel;
    private JLabel textColorLabel;
    private JButton changeTagColorButton;
    private JButton changeTextColorButton;

    /**
     * Constructor.
     * @param parent parent window
     * @param options options
     */
    SettingsDialog(final Window parent, final Map<String, String> options) {
        super(parent);
        initComponents();

        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        setMinimumSize(new java.awt.Dimension(530, 200));

        this.options = new TreeMap<>(options);

        // Set localized UI text values
        setTitle(Util.RB.getString("SETTINGS_TITLE"));
        plainFootnotesCheckBox.setText(Util.RB.getString("SETTINGS_PLAIN_SHORTCUTS"));
        textColorLabel.setText(Util.RB.getString("SETTINGS_EXTRA_TEXT_COLOR_EXAMPLE"));
        tagColorLabel.setText(Util.RB.getString("SETTINGS_EXTRA_TAG_COLOR_EXAMPLE"));
        changeTextColorButton.setText(Util.RB.getString("SETTINGS_CHANGE_COLOR_BUTTON"));
        changeTagColorButton.setText(Util.RB.getString("SETTINGS_CHANGE_COLOR_BUTTON"));

        Mnemonics.setLocalizedText(buttonOK, OStrings.getString("BUTTON_OK"));
        Mnemonics.setLocalizedText(buttonCancel, OStrings.getString("BUTTON_CANCEL"));

        // Set values to control elements
        String usePlainFootnotes = options.getOrDefault(Util.PLAIN_SHORTCUTS, "false");
        plainFootnotesCheckBox.setSelected(Boolean.valueOf(usePlainFootnotes));

        Color extraTagColor = Color.decode(options.getOrDefault(Util.EXTRA_TAG_COLOR,
                Util.DEFAULT_EXTRA_TAG_COLOR));
        tagColorLabel.setForeground(extraTagColor);
        tagColorLabel.setBackground(Color.WHITE);

        Color extraTextColor = Color.decode(options.getOrDefault(Util.EXTRA_TEXT_COLOR,
                Util.DEFAULT_EXTRA_TEXT_COLOR));
        textColorLabel.setForeground(extraTextColor);
        textColorLabel.setBackground(Color.WHITE);

        // Set action callbacks
        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        changeTagColorButton.addActionListener(e -> {
            Color initialColor = tagColorLabel.getForeground();
            Color newColor = JColorChooser.showDialog(null,
                    Util.RB.getString("SETTINGS_COLOR_DIALOG_TITLE"),
                    initialColor);
            if (newColor != null) {
                tagColorLabel.setForeground(newColor);
            }
        });

        changeTextColorButton.addActionListener(e -> {
            Color initialColor = textColorLabel.getForeground();
            Color newColor = JColorChooser.showDialog(null,
                    Util.RB.getString("SETTINGS_COLOR_DIALOG_TITLE"),
                    initialColor);
            if (newColor != null) {
                textColorLabel.setForeground(newColor);
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(final WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        panel.registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        setLocationRelativeTo(parent);
    }

    Map<String, String> getOptions() {
        return options;
    }

    private void onOK() {
        options.put(Util.PLAIN_SHORTCUTS, Boolean.toString(plainFootnotesCheckBox.isSelected()));
        options.put(Util.EXTRA_TAG_COLOR, colorToHex(tagColorLabel.getForeground()));
        options.put(Util.EXTRA_TEXT_COLOR, colorToHex(textColorLabel.getForeground()));
        dispose();
    }

    private void onCancel() {
        options = null;
        dispose();
    }

    /**
     * Convert Color to HEX string.
     * @param color color
     * @return HEX encoded color
     */
    static String colorToHex(final Color color) {
        StringBuilder sb = new StringBuilder();
        sb.append('#');
        Stream.of(color.getRed(), color.getGreen(), color.getBlue())
                .map(v -> {
                            String cs = Integer.toHexString(v).toUpperCase();
                            if (cs.length() == 1) {
                                return "0" + cs;
                            } else {
                                return cs;
                            }
                        }
                ).forEach(sb::append);
        return  sb.toString();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        panel = new JPanel();
        plainFootnotesCheckBox = new JCheckBox();
        buttonCancel = new JButton();
        buttonOK = new JButton();
        changeTagColorButton = new JButton();
        changeTextColorButton = new JButton();
        tagColorLabel = new JLabel();
        textColorLabel = new JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(
                layout.createParallelGroup()
                        .addComponent(plainFootnotesCheckBox)
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addComponent(tagColorLabel, GroupLayout.PREFERRED_SIZE,
                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(changeTagColorButton)
                        )
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addComponent(textColorLabel, GroupLayout.PREFERRED_SIZE,
                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(changeTextColorButton)
                        )
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                Short.MAX_VALUE)
                                        .addComponent(buttonOK)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(buttonCancel)
                        )
        );

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(plainFootnotesCheckBox)
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(tagColorLabel)
                                        .addComponent(changeTagColorButton)
                        )
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(textColorLabel)
                                        .addComponent(changeTextColorButton)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                                30, Short.MAX_VALUE)
                        .addGroup(
                                layout.createParallelGroup()
                                        .addComponent(buttonOK)
                                        .addComponent(buttonCancel)
                        )
        );

        layout.linkSize(buttonOK, buttonCancel);
        layout.linkSize(changeTagColorButton, changeTextColorButton);

        setContentPane(panel);
        pack();
    }
}
