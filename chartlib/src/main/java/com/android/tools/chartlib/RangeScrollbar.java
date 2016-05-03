/*
 * Copyright (C) 2014 The Android Open Source Project
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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JScrollBar;


/**
 * A component that allows users to scroll a {@link Range} object.]
 * The range's global and current min/max are mapped to the {@link DefaultBoundedRangeModel}'s
 * min/max and extent, as follows:
 *
 * A              B                      C             D
 * +---------------------------------------------------+
 * +              x----------------------|             +
 * +---------------------------------------------------+
 *
 * A. Range data's global minimum → {@link DefaultBoundedRangeModel#min}
 * B. {@link Range#mCurrentMin} → BoundedRangeModel's min
 * C. {@link Range#mCurrentMax} → BoundedRangeModel's min + extent
 * D. Range data's global maximum → BoundedRangeModel's max
 */
public final class RangeScrollbar extends JScrollBar implements Animatable {

    /**
     * Different states to control the behavior of the scrollbar:
     * STREAMING - Sticks to the end of the range to allow users to see the most recent data.
     * VIEWING - Sticks the scrollbar to a particular data range in the past
     * SCROLLING - User is scrolling. Also see {@link #setStableScrolling(boolean)}.
     */
    private enum ScrollingMode {
        STREAMING,
        VIEWING,
        SCROLLING
    }

    /**
     * Percentage threshold to switch the scrollbar to STREAMING mode.
     */
    private static final float STREAMING_POSITION_THRESHOLD = 0.1f;

    @NonNull
    private ScrollingMode mScrollingMode;

    private boolean mStableScrolling;

    @NonNull
    private final Range mGlobalRange;

    @NonNull
    private final Range mRange;

    public RangeScrollbar(@NonNull Range globalRange, @NonNull Range range) {
        super(HORIZONTAL);

        mGlobalRange = globalRange;
        mRange = range;
        mScrollingMode = ScrollingMode.STREAMING;

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mScrollingMode = ScrollingMode.SCROLLING;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mScrollingMode = closeToMaxRange() ?
                                 ScrollingMode.STREAMING : ScrollingMode.VIEWING;
            }
        });
    }

    /**
     * When set to true, prevents the scrollbar from shifting/changing size when the user scrolls.
     * Otherwise, the scrollbar will continue to adjust its visuals based on range changes.
     */
    public void setStableScrolling(boolean fixedScrolling) {
        mStableScrolling = fixedScrolling;
    }

    /**
     * Adjust zoom by a percentage of the current view range.
     */
    public void zoom(float percentage) {
        double zoomDelta = mRange.getLength() * percentage;
        double targetMin = Math.max(mGlobalRange.getMin(), mRange.getMin() - zoomDelta);
        double targetMax = Math.min(mGlobalRange.getMax(), mRange.getMax() + zoomDelta);
        mRange.setMinTarget(targetMin);
        mRange.setMaxTarget(targetMax);
    }

    @Override
    public void animate(float frameLength) {
        if (mScrollingMode == ScrollingMode.STREAMING) {
            if (!mRange.setMax(mGlobalRange.getMax())) {
                // If something else has finalized the range, quit streaming mode.
                mScrollingMode = ScrollingMode.VIEWING;
            }
        }

        // Keeps the scrollbar visuals in sync with the global and current data range.
        // Because the scrollbar's model operates with ints, we map the current data range to the
        // scrollbar's width and scale all the other values needed by the model accordingly.
        double globalRange = mGlobalRange.getLength();
        double currentRange = mRange.getLength();
        int scrollbarExtent = getWidth();
        int scrollbarRange = (int)(globalRange * scrollbarExtent / currentRange);

        switch (mScrollingMode) {
            case STREAMING:
                // Sticks the thumb to the end
                setValues(scrollbarRange - scrollbarExtent, scrollbarExtent, 0, scrollbarRange);
                break;
            case VIEWING:
                // User is viewing data in the past but not actively scrolling,
                // so keep the size and position up to date.
                setValues((int)(scrollbarRange * mRange.getMin() / globalRange),
                          scrollbarExtent, 0, scrollbarRange);
                break;
            case SCROLLING:
                // Only update the scrollbar's max if stable scrolling is disable.
                if (!mStableScrolling) {
                    setMaximum(scrollbarRange);
                }

                // In stable scrolling, user is interacting with an outdated scrollbar model so
                // we have to calculate an adjusted value as if the scrollbar is up to date.
                //
                // If x----| represents the outdated scrollbar as follows:
                // +----------------------+
                // +       x----|         +
                // +----------------------+
                //
                // Then, we need to adjust x----| so that it scales to the new scrollbar range
                // in order to cover the entire range of the new data: (visuals not to scale)
                // +----------------------------------+
                // +               x----|             +
                // +----------------------------------+
                //
                float adjustedValue = getValue() /
                                      (float)(getMaximum() - scrollbarExtent) *
                                      (scrollbarRange - scrollbarExtent);
                // If scrollbarExtent is equals to getMaximum() (and/or scrollbarRange) and
                // getValue() is 0, adjustedValue is going to be 0/0, which is a NaN. In this case,
                // we want it to be 0.
                if (Float.isNaN(adjustedValue)) {
                    adjustedValue = 0f;
                }
                // Use the ratio of adjustValue relative to scrollbarRange to get new min
                double newMin = (globalRange * adjustedValue / scrollbarRange) + mGlobalRange.getMin();
                mRange.setMin(newMin);
                mRange.setMax(newMin + currentRange);
                mRange.lockValues();
                break;
        }
    }

    /**
     * Checks if scrollbar thumb is close to the end of the range.
     * @return True if the thumb is near the end, false otherwise.
     */
    private boolean closeToMaxRange() {
        BoundedRangeModel model = getModel();
        return model.getMaximum() - (model.getValue() + model.getExtent()) <
               STREAMING_POSITION_THRESHOLD * model.getExtent();
    }
}
