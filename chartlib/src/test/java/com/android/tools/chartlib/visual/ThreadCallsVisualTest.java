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

import com.android.annotations.NonNull;
import com.android.tools.chartlib.Animatable;
import com.android.tools.chartlib.AnimatedComponent;
import com.android.tools.chartlib.AxisComponent;
import com.android.tools.chartlib.LineChart;
import com.android.tools.chartlib.SelectionComponent;
import com.android.tools.chartlib.TimeAxisDomain;
import com.android.tools.chartlib.threadstacks.HNode;
import com.android.tools.chartlib.threadstacks.HTreeChart;
import com.android.tools.chartlib.threadstacks.Sampler;
import com.android.tools.chartlib.threadstacks.Method;
import com.android.tools.chartlib.threadstacks.MethodRenderer;
import com.android.tools.chartlib.Range;
import com.android.tools.chartlib.model.LineChartData;
import com.android.tools.chartlib.model.RangedContinuousSeries;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

public class ThreadCallsVisualTest extends VisualTest implements ActionListener {

    private static final String ACTION_START_RECORDING = "start_recording";
    private static final String ACTION_STOP_RECORDING = "stop_recording";
    private static final String ACTION_SAVE_RECORDING = "save_recording";
    private static final String ACTION_LOAD_RECORDING = "load_recording";
    private static final String ACTION_THREAD_SELECTED = "thread_selected";

    private HTreeChart mChart;
    private HashMap<String, HNode<Method>> forest;
    private JButton mRecordButton;
    private JButton mSaveButton;
    private JButton mLoadButton;
    private JComboBox mComboBox;
    private Sampler mSampler;
    private HNode<Method> mtree;

    private Range mSelectionRange;
    private Range mDataRange;

    private SelectionComponent mSelector;
    private AxisComponent mAxis;

    private final static int AXIS_SIZE = 20;

    @NonNull
    private LineChart mLineChart;

    @NonNull
    private LineChartData mData;

    @NonNull
    private JScrollBar mScrollBar;

    public ThreadCallsVisualTest() {
        this.mDataRange = new Range();
        this.mAxis = new AxisComponent(mDataRange, mDataRange, "TIME",
                AxisComponent.AxisOrientation.BOTTOM, AXIS_SIZE, AXIS_SIZE, false,
                new TimeAxisDomain(10, 50, 5));
        this.mSelectionRange = new Range();

        this.mSelector = new SelectionComponent(mAxis, mSelectionRange, mDataRange, mDataRange);

        this.mChart = new HTreeChart<Method>();
        this.mChart.setRenderer(new MethodRenderer());
        this.mChart.setXRange(mSelectionRange);

        mData = new LineChartData();
        mLineChart = new LineChart(mData);
    }

    @Override
    protected void registerComponents(List<AnimatedComponent> components) {
        components.add(mChart);
        components.add(mAxis);
        components.add(mSelector);
        components.add(mLineChart);
    }

    @Override
    public String getName() {
        return "Thread stacks";
    }

    @Override
    protected List<Animatable> createComponentsList() {
        List<Animatable> list = new ArrayList<>();
        list.add(mChart);
        list.add(mSelector);
        list.add(mAxis);
        list.add(mLineChart);
        list.add(mSelectionRange);
        return list;
    }

    @Override
    protected void populateUi(@NonNull JPanel mainPanel) {
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        mRecordButton = new JButton("Record");
        mRecordButton.setActionCommand(ACTION_START_RECORDING);
        mRecordButton.addActionListener(this);
        buttonsPanel.add(mRecordButton);

        mLoadButton = new JButton("Load");
        mLoadButton.setActionCommand(ACTION_LOAD_RECORDING);
        mLoadButton.addActionListener(this);
        buttonsPanel.add(mLoadButton);

        mSaveButton = new JButton("Save");
        mSaveButton.setActionCommand(ACTION_SAVE_RECORDING);
        mSaveButton.addActionListener(this);
        buttonsPanel.add(mSaveButton);

        mComboBox = new JComboBox<String>(new String[0]) {
            @Override
            public Dimension getMaximumSize() {
                Dimension max = super.getMaximumSize();
                max.height = getPreferredSize().height;
                return max;
            }
        };
        mComboBox.addActionListener(this);
        mComboBox.setActionCommand(ACTION_THREAD_SELECTED);

        JPanel viewControlPanel = new JPanel();
        viewControlPanel.setLayout(new BoxLayout(viewControlPanel, BoxLayout.Y_AXIS));
        JLayeredPane mockTimelinePane = createMockTimeline();
        viewControlPanel.add(mockTimelinePane);
        viewControlPanel.add(mComboBox);

        controlPanel.add(buttonsPanel);
        controlPanel.add(viewControlPanel);

        mainPanel.add(controlPanel);

        JPanel viewPanel = new JPanel();
        viewPanel.setLayout(new BoxLayout(viewPanel, BoxLayout.X_AXIS));
        viewPanel.add(mChart);
        mScrollBar = new JScrollBar(JScrollBar.VERTICAL);
        mScrollBar.addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                Range yRange = mChart.getYRange();
                int yOffset = e.getValue();
                yRange.setMin(yOffset);
            }
        });

        viewPanel.add(mScrollBar);
        mainPanel.add(viewPanel);
    }

    /**
     * Invoked when an action occurs.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (ACTION_START_RECORDING.equals(e.getActionCommand())) {
            if (mSampler == null) {
                mSampler = new Sampler();
            }
            mSampler.startSampling();
            mRecordButton.setActionCommand(ACTION_STOP_RECORDING);
            mRecordButton.setText("Stop Recording");
        } else if (ACTION_STOP_RECORDING.equals(e.getActionCommand())) {
            mSampler.stopSampling();
            mRecordButton.setActionCommand(ACTION_START_RECORDING);
            mRecordButton.setText("Record");
            setData(mSampler.getData());

        } else if (ACTION_SAVE_RECORDING.equals(e.getActionCommand())) {

        } else if (ACTION_LOAD_RECORDING.equals(e.getActionCommand())) {

        } else if (ACTION_THREAD_SELECTED.equals(e.getActionCommand())) {
            int selected = mComboBox.getSelectedIndex();
            if (selected >= 0 && selected < mComboBox.getItemCount()) {
                String threadName = (String) mComboBox.getSelectedItem();
                mtree = forest.get(threadName);
                mChart.setHTree(mtree);
                double start = mtree.getFirstChild().getStart();
                double end = mtree.getLastChild().getEnd();

                mDataRange.setMin(start);
                mDataRange.setMax(end);
                mSelectionRange.setMin(start);
                mSelectionRange.setMax(end);

                // Generate dummy values to simulate CPU Load.
                RangedContinuousSeries series = new RangedContinuousSeries(mDataRange,
                        new Range(0.0, 200.0));
                Random r = new Random(System.currentTimeMillis());
                for (int i = 0; i < 100; i++) {
                    series.getSeries()
                            .add((long) (start + (end - start) / 100 * i), r.nextInt(100));
                }
                mData.add(series);
                mScrollBar.setValues(0, mChart.getHeight(), 0, mChart.getMaximumHeight());
            }
        }
    }

    public void setData(HashMap<String, HNode<Method>> forest) {
        this.forest = forest;
        mComboBox.removeAllItems();
        for (String threadName : forest.keySet()) {
            mComboBox.addItem(threadName);
        }
    }

    private JLayeredPane createMockTimeline() {
        JLayeredPane timelinePane = new JLayeredPane() {
            @Override
            public Dimension getMaximumSize() {
                Dimension max = super.getMaximumSize();
                max.height = getPreferredSize().height;
                return max;
            }
        };
        timelinePane.add(mAxis);
        timelinePane.add(mLineChart);
        timelinePane.add(mSelector);
        timelinePane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                JLayeredPane host = (JLayeredPane) e.getComponent();
                if (host != null) {
                    Dimension dim = host.getSize();
                    for (Component c : host.getComponents()) {
                        if (c instanceof AxisComponent) {
                            AxisComponent axis = (AxisComponent) c;
                            switch (axis.getOrientation()) {
                                case LEFT:
                                case BOTTOM:
                                case RIGHT:
                                case TOP:
                                    axis.setBounds(0, dim.height - AXIS_SIZE, dim.width, AXIS_SIZE);
                                    break;
                            }
                        } else {
                            c.setBounds(AXIS_SIZE, AXIS_SIZE, dim.width - AXIS_SIZE * 2,
                                    dim.height - AXIS_SIZE * 2);
                        }
                    }
                }
            }
        });
        timelinePane.setPreferredSize(new Dimension(Integer.MAX_VALUE, 100));

        return timelinePane;
    }
}
