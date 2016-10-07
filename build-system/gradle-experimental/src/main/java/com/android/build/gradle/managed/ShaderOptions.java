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

package com.android.build.gradle.managed;

import org.gradle.model.Managed;
import org.gradle.model.ModelMap;

import java.util.List;

/**
 * Managed type shader options.
 */
@Managed
public interface ShaderOptions {
    /**
     * Returns the list of glslc args
     */
    List<String> getGlslcArgs();

    ModelMap<ScopedShaderOptions> getScopedArgs();
}
