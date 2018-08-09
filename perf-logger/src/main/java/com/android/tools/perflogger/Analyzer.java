/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.tools.perflogger;

import java.util.Map;

/** Interface to hold configuration parameters for a perfgate analyzer */
public interface Analyzer {

    /** returns a map of the analyzer's parameters' names and values as strings. */
    Map<String, String> getNameValueMap();

    /** specifies how metrics from the same run are aggregated for the analyzer. */
    enum MetricAggregate {
        MEAN,
        MEDIAN,
    }

    /** specifies regression direction. */
    enum DirectionBias {
        NO_BIAS,
        IGNORE_INCREASE,
        IGNORE_DECREASE,
    }
}
