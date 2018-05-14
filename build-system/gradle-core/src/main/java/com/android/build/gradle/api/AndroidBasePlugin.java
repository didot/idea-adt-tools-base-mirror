/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.build.gradle.api;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Common plugin applied by all plugins.
 *
 * <p>The purpose of this no-op plugin is to allow other plugin authors to determine if an Android
 * plugin was applied.
 *
 * @deprecated Use {@link com.android.build.api.plugin.AndroidBasePlugin} instead.
 */
@Deprecated
public class AndroidBasePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {}
}
