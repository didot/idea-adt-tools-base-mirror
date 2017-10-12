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

package com.android.build.api.dsl.extension

import org.gradle.api.DomainObjectSet

/** 'android' extension for 'com.android.application' projects.  */
interface AppExtension : BuildProperties, VariantOrExtensionProperties, VariantAwareProperties, EmbeddedTestProperties, OnDeviceTestProperties, ApkProperties, AndroidExtension {

    /**
     * Returns the list of build output. Since the collections is built after evaluation, it should
     * be used with [DomainObjectSet.all] to process future items.
     */
    // FIXME figure out the type that's specific to App
    val buildOutputs: DomainObjectSet<*>
}