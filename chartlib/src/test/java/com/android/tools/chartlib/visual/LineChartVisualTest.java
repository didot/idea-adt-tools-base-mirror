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
import com.android.tools.chartlib.AnimatedTimeRange;
import com.android.tools.chartlib.LineChart;
import com.android.tools.chartlib.Range;
import com.android.tools.chartlib.model.LineChartData;
import com.android.tools.chartlib.model.RangedContinuousSeries;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;

public class LineChartVisualTest extends VisualTest {

    @NonNull
    private LineChart mLineChart;

    @NonNull
    private LineChartData mData;

    @NonNull
    private AnimatedTimeRange mAnimatedTimeRange;

    @Override
    protected List<Animatable> createComponentsList() {
        mData = new LineChartData();

        long now = System.currentTimeMillis();
        Range xRange = new Range(now, now + 60000);
        mLineChart = new LineChart(mData);
        mAnimatedTimeRange = new AnimatedTimeRange(xRange, 0);

        List<Animatable> componentsList = new ArrayList<>();

        // Add the scene components to the list
        componentsList.add(mAnimatedTimeRange);
        componentsList.add(xRange);
        componentsList.add(mLineChart);

        Range mYRange = new Range(0.0, 100.0);
        for (int i = 0; i < 4; i++) {
            if (i % 2 == 0) {
                mYRange = new Range(0.0, 100.0);
                componentsList.add(mYRange);
            }
            RangedContinuousSeries ranged = new RangedContinuousSeries(xRange, mYRange);
            mData.add(ranged);
        }
        return componentsList;
    }

    @Override
    protected void registerComponents(List<AnimatedComponent> components) {
        components.add(mLineChart);
    }

    @Override
    public String getName() {
        return "LineChart";
    }

    @Override
    protected void populateUi(@NonNull JPanel panel) {
        JPanel controls = VisualTests.createControlledPane(panel, mLineChart);
        mLineChart.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        final AtomicInteger variance = new AtomicInteger(10);
        final AtomicInteger delay = new AtomicInteger(100);
        mUpdateDataThread = new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    while (true) {
                        int v = variance.get();
                        long now = System.currentTimeMillis();
                        for (RangedContinuousSeries rangedSeries : mData.series()) {
                            int size = rangedSeries.getSeries().size();
                            long last = size > 0 ? rangedSeries.getSeries().getY(size - 1) : 0;
                            float delta = (float) Math.random() * variance.get() - v * 0.45f;
                            rangedSeries.getSeries().add(now, last + (long) delta);
                        }
                        Thread.sleep(delay.get());
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        mUpdateDataThread.start();

        controls.add(VisualTests.createVariableSlider("Delay", 10, 5000, new VisualTests.Value() {
            @Override
            public void set(int v) {
                delay.set(v);
            }

            @Override
            public int get() {
                return delay.get();
            }
        }));
        controls.add(VisualTests.createVariableSlider("Variance", 0, 50, new VisualTests.Value() {
            @Override
            public void set(int v) {
                variance.set(v);
            }

            @Override
            public int get() {
                return variance.get();
            }
        }));
        controls.add(VisualTests.createCheckbox("Shift xRange Min", itemEvent ->
                mAnimatedTimeRange.setShift(itemEvent.getStateChange() == ItemEvent.SELECTED)));
        controls.add(VisualTests.createCheckbox("Stepped chart", itemEvent -> {
            boolean isStepped = itemEvent.getStateChange() == ItemEvent.SELECTED;
            // Make only some lines stepped
            for (int i = 0; i < mData.series().size(); i += 2) {
                mData.series().get(i).setStepped(isStepped);
            }
        }));
        controls.add(VisualTests.createCheckbox("Dashed lines", itemEvent -> {
            boolean isDashed = itemEvent.getStateChange() == ItemEvent.SELECTED;
            // Dash only some lines
            for (int i = 0; i < mData.series().size(); i += 2) {
                mData.series().get(i).setDashed(isDashed);
            }
        }));
        controls.add(VisualTests.createCheckbox("Filled lines", itemEvent -> {
            boolean isFilled = itemEvent.getStateChange() == ItemEvent.SELECTED;
            // Fill only some lines
            for (int i = 0; i < mData.series().size(); i += 2) {
                mData.series().get(i).setFilled(isFilled);
            }
        }));

        controls.add(
                new Box.Filler(new Dimension(0, 0), new Dimension(300, Integer.MAX_VALUE),
                        new Dimension(300, Integer.MAX_VALUE)));
    }
}
