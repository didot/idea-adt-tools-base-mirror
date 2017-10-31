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

package com.android.build.gradle.internal.errors

import com.android.builder.errors.EvalIssueReporter
import com.android.builder.errors.EvalIssueReporter.Severity
import com.android.builder.errors.EvalIssueReporter.Type

class DeprecationReporterImpl(
        private val issueReporter: EvalIssueReporter,
        private val projectPath: String) : DeprecationReporter {

    override fun reportDeprecatedUsage(
            newDslElement: String,
            oldDslElement: String,
            deprecationTarget: DeprecationReporter.DeprecationTarget) {
        issueReporter.reportIssue(
                Type.DEPRECATED_DSL,
                Severity.WARNING,
                "DSL element '$oldDslElement' is obsolete and has been replaced with '$newDslElement'.\n" +
                        "It will be removed ${deprecationTarget.removalTime}",
                "$oldDslElement::$newDslElement::${deprecationTarget.name}")
    }

    override fun reportDeprecatedUsage(
            newDslElement: String,
            oldDslElement: String,
            url: String,
            deprecationTarget: DeprecationReporter.DeprecationTarget) {
        issueReporter.reportIssue(
                Type.DEPRECATED_DSL,
                Severity.WARNING,
                "DSL element '$oldDslElement' is obsolete and has been replaced with '$newDslElement'.\n" +
                        "It will be removed ${deprecationTarget.removalTime}\n" +
                        "For more information, see $url",
                "$oldDslElement::$newDslElement::${deprecationTarget.name}")
    }

    override fun reportObsoleteUsage(oldDslElement: String,
            deprecationTarget: DeprecationReporter.DeprecationTarget) {
        issueReporter.reportIssue(
                Type.DEPRECATED_DSL,
                Severity.WARNING,
                "DSL element '$oldDslElement' is obsolete and will be removed ${deprecationTarget.removalTime}",
                "$oldDslElement::::${deprecationTarget.name}")
    }

    override fun reportObsoleteUsage(
            oldDslElement: String,
            url: String,
            deprecationTarget: DeprecationReporter.DeprecationTarget) {
        issueReporter.reportIssue(
                Type.DEPRECATED_DSL,
                Severity.WARNING,
                "DSL element '$oldDslElement' is obsolete and will be removed ${deprecationTarget.removalTime}\n" +
                        "For more information, see $url",
                "$oldDslElement::::${deprecationTarget.name}")
    }

    override fun reportDeprecatedConfiguration(
            newConfiguration: String,
            oldConfiguration: String,
            deprecationTarget: DeprecationReporter.DeprecationTarget) {
        issueReporter.reportIssue(
                Type.DEPRECATED_CONFIGURATION,
                Severity.WARNING,
                "Configuration '$oldConfiguration' is obsolete and has been replaced with '$newConfiguration'.\n" +
                        "It will be removed ${deprecationTarget.removalTime}",
                "$oldConfiguration::$newConfiguration::${deprecationTarget.name}")
    }
}