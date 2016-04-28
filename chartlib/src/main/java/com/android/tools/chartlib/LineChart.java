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

package com.android.tools.chartlib;

import com.android.annotations.NonNull;
import com.android.tools.chartlib.config.LineConfig;
import com.android.tools.chartlib.model.ContinuousSeries;
import com.android.tools.chartlib.model.RangedContinuousSeries;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LineChart extends AnimatedComponent {

    /**
     * Transparency value to be applied in filled line charts.
     */
    private static final int ALPHA_MASK = 0x88000000;

    /**
     * The length of the dash length in terms of the Range's x unit. e.g. 100ms == 1 dash.
     * TODO consider scaling the dash length based on the global range. Otherwise if the range is
     *      too large, the dashes eventually connect to look like a line.
     */
    private static final float DASH_LENGTH = 100f;

    /**
     * The scale difference between the dash lengths in the x and y axes.
     * TODO compute this ratio by examining the actual different between the x and y global ranges.
     */
    private static final float X_TO_Y_RATIO = 0.01f;

    /**
     * Maps the series to their correspondent visual line configuration.
     * The keys insertion order is preserved.
     */
    @NonNull
    private final Map<RangedContinuousSeries, LineConfig> mLinesConfig = new LinkedHashMap<>();

    @NonNull
    private final ArrayList<Path2D.Float> mPaths;

    /**
     * The color of the next line to be inserted, if not specified, is picked from {@code COLORS}
     * array of {@link LineConfig}. This field holds the color index.
     */
    private int mNextLineColorIndex;

    public LineChart() {
        mPaths = new ArrayList<>();
    }

    /**
     * Initialize a {@code LineChart} with a list of lines.
     */
    public LineChart(@NonNull List<RangedContinuousSeries> data) {
        this();
        addLines(data);
    }

    /**
     * Add a line to the line chart.
     * @param series data of the line to be inserted
     * @param config configuration of the line to be inserted
     */
    public void addLine(@NonNull RangedContinuousSeries series, @NonNull LineConfig config) {
        mLinesConfig.put(series, config);
    }

    /**
     * Add a line to the line chart with default configuration.
     * @param series series data of the line to be inserted
     */
    public void addLine(@NonNull RangedContinuousSeries series) {
        mLinesConfig.put(series, new LineConfig(LineConfig.COLORS[mNextLineColorIndex++]));
        mNextLineColorIndex %= LineConfig.COLORS.length;
    }

    /**
     * Add multiple lines with default configuration.
     */
    public void addLines(@NonNull List<RangedContinuousSeries> data) {
        data.forEach(this::addLine);
    }

    @NonNull
    public LineConfig getLineConfig(RangedContinuousSeries rangedContinuousSeries) {
        return mLinesConfig.get(rangedContinuousSeries);
    }

    @Override
    protected void updateData() {
        Map<Range, Long> max = new HashMap<>();
        for (RangedContinuousSeries ranged : mLinesConfig.keySet()) {
            ContinuousSeries series = ranged.getSeries();
            long maxY = series.getMaxY();
            Long m = max.get(ranged.getYRange());
            max.put(ranged.getYRange(), m == null ? maxY : Math.max(maxY, m));
        }

        for (Map.Entry<Range, Long> entry : max.entrySet()) {
            Range range = entry.getKey();
            range.setMaxTarget(entry.getValue());
        }
    }

    @Override
    public void postAnimate() {
        long duration = System.nanoTime();
        int p = 0;
        for (RangedContinuousSeries ranged : mLinesConfig.keySet()) {
            LineConfig config = mLinesConfig.get(ranged);
            Path2D.Float path;
            if (p == mPaths.size()) {
                path = new Path2D.Float();
                mPaths.add(path);
            } else {
                path = mPaths.get(p);
                path.reset();
            }

            double xMin = ranged.getXRange().getMin();
            double xMax = ranged.getXRange().getMax();
            double yMin = ranged.getYRange().getMin();
            double yMax = ranged.getYRange().getMax();
            long prevX = 0;
            long prevY = 0;
            // Amount in percentage the dash pattern has been drawn.
            float currentDashPercentage = 1f;

            double firstXd = 0f; // X coordinate of the first destination point
            // TODO optimize to not draw anything before or after min and max.
            int size = ranged.getSeries().size();
            for (int i = 0; i < size; i++) {
                long currX = ranged.getSeries().getX(i);
                long currY = ranged.getSeries().getY(i);
                double xd = (currX - xMin) / (xMax - xMin);
                double yd = (currY - yMin) / (yMax - yMin);
                if (i == 0) {
                    path.moveTo(xd, 1.0f);
                    firstXd = xd;
                } else {
                    // Dashing only applies if we are not in fill mode.
                    if (config.isDashed() && !config.isFilled()) {
                        if (config.isStepped()) {
                            // If stepping, first draw horizontal dashes to xd
                            currentDashPercentage = drawDash(path, currentDashPercentage,
                                    prevX, prevY, currX, prevY, xd, path.getCurrentPoint().getY());
                            prevX = currX;
                        }
                        currentDashPercentage = drawDash(path, currentDashPercentage,
                                prevX, prevY, currX, currY, xd, 1.0f - yd);
                    } else {
                        // If the chart is stepped, a horizontal line should be drawn from the current
                        // point (e.g. (x0, y0)) to the destination's X value (e.g. (x1, y0)) before
                        // drawing a line to the destination point itself (e.g. (x1, y1)).
                        if (config.isStepped()) {
                            path.lineTo(xd, path.getCurrentPoint().getY());
                        }
                        path.lineTo(xd, 1.0f - yd);
                    }
                }

                prevX = currX;
                prevY = currY;
            }

            // If the chart is filled, draw a line from the last point to X axis and another one
            // from this new point to the first destination point. The resulting polygon is going to
            // be filled.
            // TODO: When stacked charts are implemented, the polygon shouldn't be drawn
            // until the X axis, but the Y value of the last path
            if (config.isFilled()) {
                path.lineTo(path.getCurrentPoint().getX(), 1.0f);
                path.lineTo(firstXd, 1.0f);
            }

            addDebugInfo("Range[%d] Max: %.2f", p, xMax);
            p++;
        }
        mPaths.subList(p, mPaths.size()).clear();
        addDebugInfo("postAnimate time: %.2fms", (System.nanoTime() - duration) / 1000000.f);
    }

    @Override
    protected void draw(Graphics2D g2d) {
        Dimension dim = getSize();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        AffineTransform scale = AffineTransform.getScaleInstance(dim.getWidth(), dim.getHeight());
        assert mPaths.size() == mLinesConfig.size();
        int i = 0;
        for (RangedContinuousSeries ranged : mLinesConfig.keySet()) {
            LineConfig config = mLinesConfig.get(ranged);
            g2d.setColor(config.getColor());
            Shape shape = scale.createTransformedShape(mPaths.get(i));
            if (config.isFilled()) {
                // If the chart is filled, we want to set some transparency in its color
                // so the other charts can also be visible
                int newColorRGBA = 0x00ffffff & g2d.getColor().getRGB(); // reset alpha
                newColorRGBA |= ALPHA_MASK; // set new alpha
                g2d.setColor(new Color(newColorRGBA, true));
                g2d.fill(shape);
            } else {
                g2d.draw(shape);
            }
            i++;
        }
    }

    /**
     * Given the previous and current points, compute the dash length that should be used based on
     * the slope of the line.
     *
     * @return the dash length that is scaled to the normalized length of the line.
     */
    private static float computeDashLength(float dashLength, float xToYRatio,
            long prevX, long prevY, long currX, long currY,
            double prevXNorm, double prevYNorm, double currXNorm, double currYNorm) {
        float xDiff = currX - prevX;
        float yDiff = currY - prevY;

        // Normalize x to y so that their scales are consistent when we compute the hypotenuse.
        // Otherwise, x will dominate y most of the time because x is usually in ms,
        // while y will be kb, percentage or some relatively smaller scale.
        float xDiffScaled = xDiff * xToYRatio;

        // Some trigonometry to compute the x/y ratios relative to the hypotenuse.
        float angle = (float) Math.atan2(yDiff, xDiffScaled);
        float xRatio = (float) Math.cos(angle);
        float yRatio = (float) Math.sin(angle);

        // Compute the adjusted dash length based on the x/y ratios.
        float length = (float) Math.sqrt(Math.pow(dashLength * xRatio, 2) +
                Math.pow(dashLength * xToYRatio * yRatio, 2));

        // Compute the number of dashes that would appear on the un-normalized line.
        float h = (float) Math.sqrt(xDiff * xDiff + yDiff * yDiff);
        float numDashes = h / length;

        // Compute the dash length on the normalized line based on numDashes.
        float xDiffNorm = (float) (currXNorm - prevXNorm);
        float yDiffNorm = (float) (currYNorm - prevYNorm);
        float hNorm = (float) Math
                .sqrt(xDiffNorm * xDiffNorm + yDiffNorm * yDiffNorm);

        return hNorm / numDashes;
    }

    /**
     * TODO Investigate performance issue when a lot of dashes need to be drawn, resulting in many
     *      moveTo/lineTo calls. This may not be a problem if we decide to scale the dash length
     *      incrementally based on the global range.
     *
     * @param path           the path object to draw the dashes in.
     * @param dashPercentage the percentage along the dash pattern the draw should start from.
     * @param prevX          the unscaled X of the last point in {@code path}.
     * @param prevY          the unscaled Y of the last point in {@code path}.
     * @param nextX          the unscaled X of the next point.
     * @param nextY          the unscaled Y of the next point.
     * @param currXNorm      the end X - normalized to current range.
     * @param currYNorm      the end Y - normalized to current range.
     * @return the remaining dash percentage after dashes have been drawn from {prevX, prevY} tp
     * {currX, currY}.
     */
    private static float drawDash(Path2D.Float path, float dashPercentage,
            long prevX, long prevY, long nextX, long nextY, double currXNorm, double currYNorm) {
        if (prevX == nextX && prevY == nextY) {
            // Skip drawing if it is a point.
            return dashPercentage;
        }

        double xd, yd;
        double prevXNorm = path.getCurrentPoint().getX();
        double prevYNorm = path.getCurrentPoint().getY();

        float pathLength = (float) Point2D.distance(prevXNorm, prevYNorm, currXNorm, currYNorm);
        float dashLength = computeDashLength(DASH_LENGTH, X_TO_Y_RATIO,
                prevX, prevY, nextX, nextY,
                prevXNorm, prevYNorm, currXNorm, currYNorm);
        float drawLength = dashLength / 2;
        float currentDashPosition = dashPercentage * dashLength;

        while (pathLength > 0) {
            xd = currXNorm - prevXNorm; // Remaining x delta.
            yd = currYNorm - prevYNorm; // Remaining y delta.
            if (currentDashPosition > drawLength) {
                // Only draws the first half of dashLength.
                float currentDrawLength = currentDashPosition - drawLength;
                float pathLengthToDraw = pathLength > currentDrawLength ?
                        currentDrawLength : pathLength;

                path.lineTo(prevXNorm + xd * pathLengthToDraw / pathLength,
                        prevYNorm + yd * pathLengthToDraw / pathLength);

                currentDashPosition -= pathLengthToDraw;
                pathLength -= pathLengthToDraw;
            } else {
                // Treats the last half of dashLength as space, skip forward.
                float pathLengthToDraw = pathLength > currentDashPosition ?
                        currentDashPosition : pathLength;

                path.moveTo(prevXNorm + xd * pathLengthToDraw / pathLength,
                        prevYNorm + yd * pathLengthToDraw / pathLength);

                currentDashPosition -= pathLengthToDraw;
                pathLength -= pathLengthToDraw;
            }

            prevXNorm = path.getCurrentPoint().getX();
            prevYNorm = path.getCurrentPoint().getY();
            if (currentDashPosition == 0) {
                // Reset dash length if we have finished the pattern.
                currentDashPosition = dashLength;
            }
        }

        return currentDashPosition / dashLength;
    }
}

