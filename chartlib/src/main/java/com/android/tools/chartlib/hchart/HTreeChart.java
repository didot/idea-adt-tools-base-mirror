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

package com.android.tools.chartlib.hchart;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.chartlib.AnimatedComponent;
import com.android.tools.chartlib.Range;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.awt.RenderingHints;
import java.util.Stack;

import javax.swing.UIManager;

public class HTreeChart<T> extends AnimatedComponent implements MouseWheelListener {

    @Nullable
    private HRenderer<T> mHRenderer;

    @NonNull
    private HNode<T> mRoot;

    @NonNull
    private Font mFont;

    @NonNull
    private FontMetrics mFontMetrics;

    @Nullable
    private Range mXRange;

    @NonNull
    private Range mYRange;

    @NonNull
    private Rectangle2D.Float mRect;

    private static final String NO_HTREE = "No HTree selected.";
    private static final String NO_RANGE = "X range width is zero: Please use a wider range.";
    private static final int BORDER_PLUS_PADDING = 2;
    private static final int ZOOM_FACTOR = 20;

    public HTreeChart() {
        this.mFont = UIManager.getDefaults().getFont("TabbedPane.font");
        mRoot = new HNode<>();
        mRect = new Rectangle2D.Float();
        mYRange = new Range();
        addMouseWheelListener(this);
    }

    @Override
    protected void updateData() {
    }

    @Override
    protected void draw(Graphics2D g) {

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        this.mFontMetrics = g.getFontMetrics(mFont);

        if (mRoot.getChildren().size() == 0) {
            g.drawString(NO_HTREE, getWidth() / 2 - mFontMetrics.stringWidth(NO_HTREE),
                    getHeight() / 2);
            return;
        }

        if (getXRange().getLength() == 0) {
            g.drawString(NO_RANGE, getWidth() / 2 - mFontMetrics.stringWidth(NO_RANGE),
                    getHeight() / 2);
            return;
        }

        // Render using a LIFO Stack instead of recursion to limit the depth of the Java call
        // stack.
        Stack<HNode<T>> stack = new Stack<>();
        stack.addAll(mRoot.getChildren());
        while (!stack.isEmpty()) {
            HNode<T> n = stack.pop();
            renderHNode(g, n);
            stack.addAll(n.getChildren());
        }
    }

    // This method is not thread-safe: It re-uses mRect.
    private void renderHNode(Graphics2D g, HNode<T> n) {

        // 1. Cull node to view Range.
        if (n.getStart() > getXRange().getMax() || n.getEnd() < getXRange().getMin()) {
            return;
        }

        // 2. Clip node.
        double leftEdge = rangeToPosition(n.getStart());
        if (leftEdge < 0) {
            leftEdge = 0;
        }
        double rightEdge = rangeToPosition(n.getEnd());
        if (rightEdge > getWidth()) {
            rightEdge = getWidth();
        }
        double width = rightEdge - leftEdge;

        // 3. Calculate node position and dimension.
        mRect.x = (float) leftEdge;
        mRect.y = (float) ((mFontMetrics.getHeight() + BORDER_PLUS_PADDING) * n.getDepth()
                - getYRange().getMin());
        mRect.width = (float) width - BORDER_PLUS_PADDING;
        mRect.height = mFontMetrics.getHeight();

        // 4. Render node
        mHRenderer.render(g, n.getData(), mRect);
    }

    // This could be done with an Axis. But that seems overkill. A simple method will do for now.
    private double rangeToPosition(double v) {
        double translate = -getXRange().getMin();
        double scale = this.getWidth() / (getXRange().getMax() - getXRange().getMin());
        return (v + translate) * scale;
    }

    private double positionToRange(double x) {
        return x / getWidth() * getXRange().getLength() + getXRange().getMin();
    }

    public void setHRenderer(HRenderer<T> r) {
        this.mHRenderer = r;
        this.mHRenderer.setFont(mFont);
    }

    public void setHTree(HNode<T> root) {
        this.mRoot = root;
    }

    public Range getXRange() {
        return mXRange;
    }

    public void setXRange(Range XRange) {
        mXRange = XRange;
    }

    public Range getYRange() {
        return mYRange;
    }

    public int getMaximumHeight() {
        int maxDepth = -1;
        // Traverse the tree FIFO instead of recursion to limit the depth of the Java call
        // stack.
        Stack<HNode<T>> stack = new Stack<>();
        stack.addAll(mRoot.getChildren());
        while (!stack.isEmpty()) {
            HNode<T> n = stack.pop();
            if (n.getDepth() > maxDepth) {
                maxDepth = n.getDepth();
            }
            stack.addAll(n.getChildren());
        }
        maxDepth += 1;
        return (mFontMetrics.getHeight() + BORDER_PLUS_PADDING) * maxDepth;
    }

    // TODO we probably want to extract/abstract this logic out later so the zooming behavior
    // is consistent across components
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double cursorRange = positionToRange(e.getX());
        double leftDelta = (cursorRange - getXRange().getMin()) / ZOOM_FACTOR * e
                .getWheelRotation();
        double rightDelta = (getXRange().getMax() - cursorRange) / ZOOM_FACTOR * e
                .getWheelRotation();
        getXRange().setMin(getXRange().getMin() - leftDelta);
        getXRange().setMax(getXRange().getMax() + rightDelta);
    }
}