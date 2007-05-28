/*
 * Copyright 2006-2007 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.userinterface.dialogs;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.NumberFormatter;
import net.sf.mzmine.util.NumberFormatter.FormatterType;

public class FormatSetupDialog extends JDialog implements ActionListener {

    public static final String helpText = "Number formatting:\n\n"
            + "0 = Digit\n" + "# = Digit (zero absent)\n"
            + ". = Decimal separator\n" + ", = Grouping separator\n"
            + "E = Exponent\n" + "% = Percentage\n\n" + "Time formatting:\n\n"
            + "H = Hour\n" + "m = Minute\n" + "s = Second\n"
            + "S = Millisecond";

    public static final int TEXTFIELD_COLUMNS = 12;

    private ExitCode exitCode = ExitCode.UNKNOWN;

    // NumberFormatter instances
    private NumberFormatter mzFormat, rtFormat, intensityFormat;

    // Dialog controles
    private JButton btnOK, btnCancel, btnHelp;
    private JTextField mzFormatField, rtFormatField, intensityFormatField;
    private JRadioButton timeRadioButton, numberRadioButton;

    /**
     * Constructor
     */
    public FormatSetupDialog(Frame owner, NumberFormatter mzFormat,
            NumberFormatter rtFormat, NumberFormatter intensityFormat) {

        // Make dialog modal
        super(owner, "Format setting", true);

        this.mzFormat = mzFormat;
        this.rtFormat = rtFormat;
        this.intensityFormat = intensityFormat;

        JLabel mzFormatLabel = new JLabel("m/z format");
        JLabel rtFormatLabel = new JLabel("Retention time format");
        JLabel intensityFormatLabel = new JLabel("Intensity format");

        mzFormatField = new JTextField(TEXTFIELD_COLUMNS);
        JPanel mzFormatFieldPanel = new JPanel();
        mzFormatFieldPanel.add(mzFormatField);
        rtFormatField = new JTextField(TEXTFIELD_COLUMNS);
        JPanel rtFormatFieldPanel = new JPanel();
        rtFormatFieldPanel.add(rtFormatField);
        intensityFormatField = new JTextField(TEXTFIELD_COLUMNS);
        JPanel intensityFormatFieldPanel = new JPanel();
        intensityFormatFieldPanel.add(intensityFormatField);

        ButtonGroup radioButtons = new ButtonGroup();
        timeRadioButton = new JRadioButton("Time");
        numberRadioButton = new JRadioButton("Number");
        radioButtons.add(timeRadioButton);
        radioButtons.add(numberRadioButton);
        JPanel radioButtonsPanel = new JPanel();
        radioButtonsPanel.add(timeRadioButton);
        radioButtonsPanel.add(numberRadioButton);

        // Create panel with controls
        JPanel pnlLabelsAndFields = new JPanel(new GridLayout(4, 2));
        pnlLabelsAndFields.add(mzFormatLabel);
        pnlLabelsAndFields.add(mzFormatFieldPanel);
        pnlLabelsAndFields.add(rtFormatLabel);
        pnlLabelsAndFields.add(radioButtonsPanel);
        pnlLabelsAndFields.add(new JLabel());
        pnlLabelsAndFields.add(rtFormatFieldPanel);
        pnlLabelsAndFields.add(intensityFormatLabel);
        pnlLabelsAndFields.add(intensityFormatFieldPanel);

        // Create buttons
        JPanel pnlButtons = new JPanel();
        btnOK = GUIUtils.addButton(pnlButtons, "OK", null, this);
        btnCancel = GUIUtils.addButton(pnlButtons, "Cancel", null, this);
        btnHelp = GUIUtils.addButton(pnlButtons, "Help", null, this);

        // Put everything into a main panel
        JPanel pnlAll = new JPanel(new BorderLayout());
        GUIUtils.addMargin(pnlAll, 10);
        pnlAll.add(pnlLabelsAndFields, BorderLayout.CENTER);
        pnlAll.add(pnlButtons, BorderLayout.SOUTH);
        add(pnlAll);

        // Set values
        mzFormatField.setText(mzFormat.getPattern());
        rtFormatField.setText(rtFormat.getPattern());
        intensityFormatField.setText(intensityFormat.getPattern());
        if (rtFormat.getType() == FormatterType.TIME)
            timeRadioButton.setSelected(true);
        else
            numberRadioButton.setSelected(true);

        pack();

        setResizable(false);
        setLocationRelativeTo(owner);

    }

    /**
     * Implementation for ActionListener interface
     */
    public void actionPerformed(ActionEvent ae) {

        Object src = ae.getSource();
        MainWindow desktop = MainWindow.getInstance();

        if (src == btnOK) {

            try {
                mzFormat.setFormat(FormatterType.NUMBER,
                        mzFormatField.getText());
            } catch (IllegalArgumentException e) {
                desktop.displayErrorMessage("Error in m/z format string: "
                        + e.getMessage());
                return;
            }

            try {
                if (timeRadioButton.isSelected()) {
                    rtFormat.setFormat(FormatterType.TIME,
                            rtFormatField.getText());
                } else {
                    rtFormat.setFormat(FormatterType.NUMBER,
                            rtFormatField.getText());
                }
            } catch (IllegalArgumentException e) {
                desktop.displayErrorMessage("Error in retention time format string: "
                        + e.getMessage());
                return;
            }

            try {
                intensityFormat.setFormat(FormatterType.NUMBER,
                        intensityFormatField.getText());
            } catch (IllegalArgumentException e) {
                desktop.displayErrorMessage("Error in intensity format string: "
                        + e.getMessage());
                return;
            }

            exitCode = ExitCode.OK;
            dispose();

            // repaint to update all formatted numbers
            desktop.repaint();

        }

        if (src == btnCancel) {
            exitCode = ExitCode.CANCEL;
            dispose();
        }

        if (src == btnHelp) {
            desktop.displayMessage(helpText);
        }

    }

    /**
     * Method for reading exit code
     * 
     */
    public ExitCode getExitCode() {
        return exitCode;
    }

}
