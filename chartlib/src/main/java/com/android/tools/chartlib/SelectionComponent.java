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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * A component for performing/rendering selection and any overlay information (e.g. Tooltip).
 */
public final class SelectionComponent extends AnimatedComponent {

    /**
     * This component can be in five modes: - OBSERVE: User is not modifying the selection (if any).
     * - CREATE: User is currently creating a selection. The first handle has been set and the other
     * will be when the user release the mouse button. - MOVE: User placed the mouse cursor between
     * the two handle and pressed the button. The two handles are moving together as a block. -
     * ADJUST User is adjusting a end of the selection.
     */
    private enum Mode {
        OBSERVE, CREATE, MOVE, ADJUST_MIN, ADJUST_MAX
    }

    private Mode mode;

    private static final Color SELECTION_FORECOLOR = new Color(0x88aae2);
    private static final Color SELECTION_BACKCOLOR = new Color(0x5588aae2, true);

    @NonNull
    private final AxisComponent mAxis;

    /**
     * The range being selected.
     */
    @NonNull
    private final Range mSelectionRange;

    /**
     * The global range for clamping selection.
     */
    @NonNull
    private final Range mDataRange;

    /**
     * The current viewing range which gets shifted when user drags the selection box beyond the
     * component's dimension.
     */
    @NonNull
    private final Range mViewRange;

    /**
     * Value used when moving the selection as a block: The user never click right in the middle of
     * the selection. This allows to move the block relative to the initial point the selection was
     * "grabbed".
     */
    double mSelectionBlockClickOffset = 0;


    /**
     * Default drawing Dimension for the handles.
     */
    private Dimension handleDim = new Dimension(12, 40);
    final static private int dimRoundingCorner = 5;


    public SelectionComponent(@NonNull AxisComponent axis,
            @NonNull Range selectionRange,
            @NonNull Range dataRange,
            @NonNull Range viewRange) {
        mAxis = axis;
        mDataRange = dataRange;
        mViewRange = viewRange;
        mode = Mode.OBSERVE;
        mSelectionRange = selectionRange;
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Just capture events of the left mouse button.
                if (e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }

                Point mMousePosition = getMouseLocation();
                mode = getModeForMousePosition(mMousePosition);
                switch (mode) {
                    case CREATE:
                        double value = mAxis.getValueAtPosition(e.getX());
                        mSelectionRange.setEmptyAt(value);
                        mode = Mode.ADJUST_MIN;
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // Just capture events of the left mouse button.
                if (!(e.getButton() == MouseEvent.BUTTON1)) {
                    return;
                }
                mode = Mode.OBSERVE;
            }
        });
    }

    /**
     * Component.getMousePosition() returns null because SelectionComponent is usually overlayed.
     * The work around is to use absolute coordinates for mouse and component and subtract them.
     */
    private Point getMouseLocation() {
        Point mLoc = MouseInfo.getPointerInfo().getLocation();
        Point cLoc = getLocationOnScreen();
        int x = (int) (mLoc.getX() - cLoc.getX());
        int y = (int) (mLoc.getY() - cLoc.getY());
        return new Point(x, y);
    }

    private Mode getModeForMousePosition(Point mMousePosition) {
        // Detect when mouse is over a handle.
        if (getHandleAreaForValue(mSelectionRange.getMax()).contains(mMousePosition)) {
            return Mode.ADJUST_MAX;
        }

        // Detect when mouse is over the other handle.
        if (getHandleAreaForValue(mSelectionRange.getMin()).contains(mMousePosition)) {
            return Mode.ADJUST_MIN;
        }

        // Detect mouse between handle.
        if (getBetweenHandlesArea().contains(mMousePosition)) {
            saveMouseBlockOffset(mMousePosition);
            return Mode.MOVE;
        }
        return Mode.CREATE;
    }

    private void saveMouseBlockOffset(Point mMousePosition) {
        double value = mAxis.getValueAtPosition(mMousePosition.x);
        mSelectionBlockClickOffset = mSelectionRange.getMin() - value;
    }

    @Override
    protected void updateData() {
        // If component is not showing, there is no point in updating data
        if (!isShowing()) {
            return;
        }

        Point mMousePosition = getMouseLocation();
        double valueAtCursor = mAxis.getValueAtPosition(mMousePosition.x);

        // Clamp to data range.
        if (valueAtCursor > mDataRange.getMax()) {
            valueAtCursor = mDataRange.getMax();
        }
        if (valueAtCursor < mDataRange.getMin()) {
            valueAtCursor = mDataRange.getMin();
        }

        // Extend view range if necessary
        if (valueAtCursor > mViewRange.getMax()) {
            mViewRange.setMax(valueAtCursor);
        }
        if (valueAtCursor < mViewRange.getMin()) {
            mViewRange.setMin(valueAtCursor);
        }

        // Check if selection was inverted (min > max or max<min)
        if (mode == Mode.ADJUST_MIN && valueAtCursor > mSelectionRange.getMax()) {
            mSelectionRange.flip();
            mode = Mode.ADJUST_MAX;
        } else if (mode == Mode.ADJUST_MAX && valueAtCursor < mSelectionRange.getMin()) {
            mSelectionRange.flip();
            mode = Mode.ADJUST_MIN;
        }

        switch (mode) {
            case CREATE:
                break;
            case ADJUST_MIN:
                mSelectionRange.setMin(valueAtCursor);
                break;
            case ADJUST_MAX:
                mSelectionRange.setMax(valueAtCursor);
                break;
            case MOVE:
                double length = mSelectionRange.getLength();
                mSelectionRange.set(valueAtCursor + mSelectionBlockClickOffset,
                        valueAtCursor + mSelectionBlockClickOffset + length);

                // Limit the selection block to viewRange Min
                if (mSelectionRange.getMin() < mViewRange.getMin()) {
                    mSelectionRange.add(mViewRange.getMin() - mSelectionRange.getMin());
                }
                // Limit the selection block to viewRange Max
                if (mSelectionRange.getMax() > mViewRange.getMax()) {
                    mSelectionRange.add(mViewRange.getMax() - mSelectionRange.getMax());
                }
                break;
        }
    }

    private void drawCursor() {
        Point mMousePosition = getMouseLocation();
        int cursor = Cursor.DEFAULT_CURSOR;

        if (getHandleAreaForValue(mSelectionRange.getMax()).contains(mMousePosition) ||
                getHandleAreaForValue(mSelectionRange.getMin()).contains(mMousePosition)) {
            // Detect mouse over the handles.
            // TODO: Replace with appropriate cursor.
            cursor = Cursor.MOVE_CURSOR;
        } else if (getBetweenHandlesArea().contains(mMousePosition)) {
            // Detect mouse between handles
            cursor = Cursor.HAND_CURSOR;
        }

        if (getTopLevelAncestor().getCursor().getType() != cursor) {
            getTopLevelAncestor().setCursor(Cursor.getPredefinedCursor(cursor));
        }
    }


    @Override
    protected void draw(Graphics2D g) {
        if (mSelectionRange.isEmpty()) {
            return;
        }

        Dimension dim = getSize();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawCursor();

        // Draw selected area.
        g.setColor(SELECTION_BACKCOLOR);
        float startXPos = mAxis.getPositionAtValue(mSelectionRange.getMin());
        float endXPos = mAxis.getPositionAtValue(mSelectionRange.getMax());
        Rectangle2D.Float rect = new Rectangle2D.Float(startXPos, 0, endXPos - startXPos,
                dim.height);
        g.fill(rect);

        // Draw vertical lines, one for each endsValue.
        g.setColor(SELECTION_FORECOLOR);
        Path2D.Float path = new Path2D.Float();
        path.moveTo(startXPos, 0);
        path.lineTo(startXPos, dim.height);
        path.moveTo(endXPos, dim.height);
        path.lineTo(endXPos, 0);
        g.draw(path);

        // Draw handles
        drawHandleAtValue(g, mSelectionRange.getMin());
        drawHandleAtValue(g, mSelectionRange.getMax());
    }

    private void drawHandleAtValue(Graphics2D g, double value) {
        g.setPaint(Color.gray);
        RoundRectangle2D.Double handle = getHandleAreaForValue(value);
        g.fill(handle);
    }


    private RoundRectangle2D.Double getHandleAreaForValue(double value) {
        float x = mAxis.getPositionAtValue(value);
        return new RoundRectangle2D.Double(x - handleDim.getWidth() / 2, 0, handleDim.getWidth(),
                handleDim.getHeight(), dimRoundingCorner, dimRoundingCorner);
    }

    private Rectangle2D.Double getBetweenHandlesArea() {

        // Convert range space value to component space value.
        double startXPos = mAxis.getPositionAtValue(mSelectionRange.getMin());
        double endXPos = mAxis.getPositionAtValue(mSelectionRange.getMax());

        return new Rectangle2D.Double(startXPos - handleDim.getWidth() / 2, 0, endXPos - startXPos,
                handleDim.getHeight());
    }
}
