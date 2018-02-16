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
package com.android.layoutinspector.model

import com.google.common.collect.ComparisonChain
import com.google.common.collect.Ordering

class ViewProperty internal constructor(val fullName: String) : Comparable<ViewProperty> {

    val name: String
    val category: String?

    var value: String? = null

    init {

        val colonIndex = fullName.indexOf(':')
        if (colonIndex != -1) {
            category = fullName.substring(0, colonIndex)
            name = fullName.substring(colonIndex + 1)
        } else {
            category = null
            name = fullName
        }
    }

    override fun toString(): String {
        return fullName + '=' + value
    }

    override fun compareTo(other: ViewProperty): Int {
        return ComparisonChain.start()
            .compare(category, other.category, CATEGORY_COMPARATOR)
            .compare(name, other.name)
            .result()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ViewProperty) {
            return false
        }

        val other = other as ViewProperty?
        return !(category != other!!.category || name != other.name)
    }

    override fun hashCode(): Int {
        var result = fullName.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (category?.hashCode() ?: 0)
        result = 31 * result + (value?.hashCode() ?: 0)
        return result
    }

    companion object {

        private val CATEGORY_COMPARATOR = Ordering.natural<Comparable<in String>>().nullsFirst<String>()
    }
}