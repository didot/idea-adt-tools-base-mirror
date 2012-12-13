/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.ide.common.resources.configuration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resource Qualifier for Screen Pixel Density.
 */
public final class ScreenHeightQualifier extends ResourceQualifier {
    /** Default screen size value. This means the property is not set */
    final static int DEFAULT_SIZE = -1;

    private final static Pattern sParsePattern = Pattern.compile("^h(\\d+)dp$");//$NON-NLS-1$
    private final static String sPrintPattern = "h%1$ddp";

    public static final String NAME = "Screen Height";

    private int mValue = DEFAULT_SIZE;

    public ScreenHeightQualifier() {
        // pass
    }

    public ScreenHeightQualifier(int value) {
        mValue = value;
    }

    public int getValue() {
        return mValue;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getShortName() {
        return NAME;
    }

    @Override
    public int since() {
        return 13;
    }

    @Override
    public boolean hasFakeValue() {
        return false;
    }

    @Override
    public boolean isValid() {
        return mValue != DEFAULT_SIZE;
    }

    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        Matcher m = sParsePattern.matcher(value);
        if (m.matches()) {
            String v = m.group(1);

            ScreenHeightQualifier qualifier = getQualifier(v);
            if (qualifier != null) {
                config.setScreenHeightQualifier(qualifier);
                return true;
            }
        }

        return false;
    }

    public static ScreenHeightQualifier getQualifier(String value) {
        try {
            int dp = Integer.parseInt(value);

            ScreenHeightQualifier qualifier = new ScreenHeightQualifier();
            qualifier.mValue = dp;
            return qualifier;

        } catch (NumberFormatException e) {
        }

        return null;
    }

    @Override
    public boolean isMatchFor(ResourceQualifier qualifier) {
        // this is the match only of the current dp value is lower or equal to the
        if (qualifier instanceof ScreenHeightQualifier) {
            return mValue <= ((ScreenHeightQualifier) qualifier).mValue;
        }

        return false;
    }

    @Override
    public boolean isBetterMatchThan(ResourceQualifier compareTo, ResourceQualifier reference) {
        if (compareTo == null) {
            return true;
        }

        ScreenHeightQualifier compareQ = (ScreenHeightQualifier)compareTo;
        ScreenHeightQualifier referenceQ = (ScreenHeightQualifier)reference;

        if (compareQ.mValue == referenceQ.mValue) {
            // what we have is already the best possible match (exact match)
            return false;
        } else if (mValue == referenceQ.mValue) {
            // got new exact value, this is the best!
            return true;
        } else {
            // get the qualifier that has the width that is the closest to the reference, but not
            // above. (which is guaranteed when this is called as isMatchFor is called first.
            return mValue > compareQ.mValue;
        }
    }

    @Override
    public String getFolderSegment() {
        return String.format(sPrintPattern, mValue);
    }

    @Override
    public String getShortDisplayValue() {
        if (isValid()) {
            return getFolderSegment();
        }

        return ""; //$NON-NLS-1$
    }

    @Override
    public String getLongDisplayValue() {
        if (isValid()) {
            return getFolderSegment();
        }

        return ""; //$NON-NLS-1$
    }


    @Override
    public int hashCode() {
        return mValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ScreenHeightQualifier other = (ScreenHeightQualifier) obj;
        if (mValue != other.mValue) {
            return false;
        }
        return true;
    }
}
