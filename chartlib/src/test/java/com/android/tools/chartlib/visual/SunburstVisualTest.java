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
import com.android.tools.chartlib.Choreographer;
import com.android.tools.chartlib.SunburstComponent;
import com.android.tools.chartlib.ValuedTreeNode;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Random;

import javax.accessibility.Accessible;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;

public class SunburstVisualTest extends VisualTest {

    private final SunburstComponent mSunburst;

    public SunburstVisualTest(Choreographer choreographer) {

        final DataNode data = new DataNode();
        data.addDataNode(new DataNode(1, 10));
        mSunburst = new SunburstComponent(data);
        choreographer.register(mSunburst);
    }

    @Override
    void registerComponents(List<AnimatedComponent> components) {
        components.add(mSunburst);
    }

    @Override
    public String getName() {
        return "Sunburst";
    }

    @Override
    protected JPanel create() {
        final JPanel panel = new JPanel();
        JPanel controls = VisualTests.createControlledPane(panel, mSunburst);
        final JLabel info = new JLabel("<No information yet>");
        panel.add(info, BorderLayout.SOUTH);

        controls.add(VisualTests.createVariableSlider("Gap", 0, 200, new VisualTests.Value() {
            @Override
            public void set(int v) {
                mSunburst.setGap(v);
            }

            @Override
            public int get() {
                return (int) mSunburst.getGap();
            }
        }));
        final JPanel sizeSlider =
                VisualTests.createVariableSlider("Size", 0, 200, new VisualTests.Value() {
            @Override
            public void set(int v) {
                mSunburst.setSliceWidth(v);
            }

            @Override
            public int get() {
                return (int) mSunburst.getSliceWidth();
            }
        });
        controls.add(sizeSlider);
        controls.add(VisualTests.createVariableSlider("Angle", 0, 360, new VisualTests.Value() {
            @Override
            public void set(int v) {
                mSunburst.setAngle(v);
            }

            @Override
            public int get() {
                return (int) mSunburst.getAngle();
            }
        }));
        controls.add(VisualTests.createVariableSlider("Start", 0, 360, new VisualTests.Value() {
            @Override
            public void set(int v) {
                mSunburst.setStart(v);
            }

            @Override
            public int get() {
                return (int) mSunburst.getStart();
            }
        }));
        controls.add(VisualTests.createVariableSlider("Fixed", 1, 100, new VisualTests.Value() {
            @Override
            public void set(int v) {
                mSunburst.setFixed(v);
            }

            @Override
            public int get() {
                return (int) mSunburst.getFixed();
            }
        }));
        controls.add(VisualTests.createVariableSlider("Separator", 0, 100, new VisualTests.Value() {
            @Override
            public void set(int v) {
                mSunburst.setSeparator(v);
            }

            @Override
            public int get() {
                return (int) mSunburst.getSeparator();
            }
        }));
        controls.add(VisualTests.createButton("Generate", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                generateLayoutData((DataNode) mSunburst.getData(), 5);
            }
        }));
        controls.add(VisualTests.createButton("Tree A", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                DataNode g = new DataNode();
                g.addDataNode(createTree(1));
                g.addDataNode(createValue());
                g.addDataNode(createTree(1));
                g.addDataNode(createValue());
                g.addDataNode(createTree(0));
                mSunburst.setData(g);
            }
        }));
        controls.add(VisualTests.createButton("Tree B", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                DataNode g = new DataNode();
                g.addDataNode(createValue());
                g.addDataNode(createValue());
                g.addDataNode(createTree(0));
                mSunburst.setData(g);
            }
        }));
        controls.add(VisualTests.createButton("Value", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                DataNode g = new DataNode();
                g.addDataNode(new DataNode(1, (int) (Math.random() * 50)));
                mSunburst.setData(g);
            }
        }));
        controls.add(VisualTests.createCheckbox("Auto size", new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                final boolean enableAutoSize = itemEvent.getStateChange() == ItemEvent.SELECTED;
                mSunburst.setAutoSize(enableAutoSize);
                // If auto size is enabled, size shouldn't be controlled by slider
                // sizeSlider is a JPanel and its first child is a JSlider
                Accessible sliderAccessible = SwingUtilities.getAccessibleChild(sizeSlider, 0);
                if (sliderAccessible instanceof JSlider) {
                    JSlider slider = (JSlider) sliderAccessible;
                    slider.setEnabled(!enableAutoSize);
                    // If disabling auto size, set slider width to current slice width
                    if (!enableAutoSize) {
                        slider.setValue((int) mSunburst.getSliceWidth());
                    }
                }
            }
        }));
        controls.add(
                new Box.Filler(new Dimension(0, 0), new Dimension(300, Integer.MAX_VALUE),
                        new Dimension(300, Integer.MAX_VALUE)));

        mSunburst.addSelectionListener(new SunburstComponent.SliceSelectionListener() {
            @Override
            public void valueChanged(SunburstComponent.SliceSelectionEvent e) {
                ValuedTreeNode node = e.getNode();
                info.setText(node == null ? "<No selection>" : String.format("Value %d Count %d",
                        node.getValue(), node.getCount()));
            }
        });
        return panel;
    }

    static class DataNode extends DefaultMutableTreeNode implements ValuedTreeNode {

        private int mCount;

        private int mValue;

        public DataNode() {
            this(0, 0);
        }

        public DataNode(int count, int value) {
            mCount = count;
            mValue = value;
        }

        @Override
        public int getCount() {
            return mCount;
        }

        @Override
        public int getValue() {
            return mValue;
        }

        public void add(int count, int value) {
            mCount += count;
            mValue += value;
            if (parent instanceof DataNode) {
                ((DataNode) parent).add(count, value);
            }
        }

        public void addDataNode(DataNode dataNode) {
            super.add(dataNode);
            add(dataNode.getCount(), dataNode.getValue());
        }
    }

    private static DataNode createTree(int depth) {
        DataNode b = depth == 0 ? createValue() : createTree(depth - 1);
        DataNode c = depth == 0 ? createValue() : createTree(depth - 1);
        DataNode a = new DataNode();
        a.addDataNode(b);
        a.addDataNode(c);
        return a;
    }

    private static DataNode createValue() {
        return new DataNode(1, (int) (Math.random() * 50));
    }

    private static void generateLayoutData(DataNode data, int maxDepth) {
        Random random = new Random();
        int branch = random.nextInt(9) + 1;
        for (int i = 0; i < branch; i++) {
            int value = random.nextInt(1024);
            if (maxDepth > 0 && random.nextInt(4) == 0) {
                DataNode group = new DataNode();
                group.add(new DataNode(1, value));
                generateLayoutData(group, maxDepth - 1);
                data.addDataNode(group);
            } else {
                data.addDataNode(new DataNode(1, value));
            }
        }
    }
}