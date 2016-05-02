/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.chartlib.visual;

import com.android.tools.chartlib.AnimatedComponent;
import com.android.tools.chartlib.visual.threadgraph.ThreadCallsVisualTest;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class VisualTests {

    interface Value {
        void set(int v);
        int get();
    }

    static JPanel createVariableSlider(String name, final int a, final int b, final Value value) {
        JPanel panel = new JPanel(new BorderLayout());
        final JLabel text = new JLabel();
        final JSlider slider = new JSlider(a, b);
        ChangeListener listener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                value.set(slider.getValue());
                text.setText(String.format("%d [%d,%d]", slider.getValue(), a, b));
            }
        };
        slider.setValue(value.get());
        listener.stateChanged(null);
        slider.addChangeListener(listener);
        panel.add(slider, BorderLayout.CENTER);
        panel.add(new JLabel(name + ": "), BorderLayout.WEST);
        panel.add(text, BorderLayout.EAST);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    static JPanel createControlledPane(JPanel panel, AnimatedComponent animated) {
        panel.setLayout(new BorderLayout());
        panel.add(animated, BorderLayout.CENTER);

        JPanel controls = new JPanel();
        LayoutManager manager = new BoxLayout(controls, BoxLayout.Y_AXIS);
        controls.setLayout(manager);
        panel.add(controls, BorderLayout.WEST);
        return controls;
    }


    static Component createButton(String label, ActionListener action) {
        JButton button = createButton(label);
        button.addActionListener(action);
        return button;
    }

    static Component createCheckbox(String label, ItemListener action) {
        return createCheckbox(label, action, false);
    }

    static Component createCheckbox(String label, ItemListener action, boolean selected) {
        JCheckBox button = new JCheckBox(label);
        button.addItemListener(action);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getMaximumSize().height));
        button.setSelected(selected);
        return button;
    }

    static JButton createButton(String label) {
        JButton button = new JButton(label);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getMaximumSize().height));
        return button;
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                VisualTestsDialog dialog = new VisualTestsDialog();
                dialog.addTest(new ThreadCallsVisualTest());
                dialog.addTest(new AxisLineChartVisualTest());
                dialog.addTest(new StateChartVisualTest());
                dialog.addTest(new LineChartVisualTest());
                dialog.addTest(new SunburstVisualTest());
                dialog.addTest(new TimelineVisualTest());
                dialog.addTest(new EventVisualTest());
                dialog.setTitle("Visual Tests");
                dialog.pack();
                dialog.setVisible(true);
            }
        });
        System.exit(0);
    }
}
