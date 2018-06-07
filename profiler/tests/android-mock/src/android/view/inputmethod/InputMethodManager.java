/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.view.inputmethod;

import android.mock.MockInputConnectionWrapper;
import com.google.common.annotations.VisibleForTesting;

/** Empty class to act as a test mock */
public class InputMethodManager {
    private static final InputMethodManager sInstance = new InputMethodManager();
    private InputConnectionWrapper mServedInputConnectionWrapper = new MockInputConnectionWrapper();
    private boolean mIsAcceptingText;

    public boolean isAcceptingText() {
        return mIsAcceptingText;
    }

    public static InputMethodManager getInstance() {
        return sInstance;
    }

    @VisibleForTesting
    public InputConnectionWrapper getConnectionWrapper() {
        return mServedInputConnectionWrapper;
    }

    @VisibleForTesting
    public void setIsAcceptingText(boolean accepting) {
        mIsAcceptingText = accepting;
    }
}
