// Copyright (C) 2017 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package android.support;

import static org.junit.Assert.*;

import com.android.support.AndroidxName;
import com.android.support.AndroidxNameUtils;
import org.junit.Test;

public class AndroidxNameTest {

    @Test
    public void className() {
        AndroidxName pkgName =
                AndroidxName.of("android.support.design.widget.", "FloatingActionButton");
        assertEquals("android.support.design.widget.FloatingActionButton", pkgName.oldName());
        assertEquals("androidx.widget.FloatingActionButton", pkgName.newName());

        // Test a non-existent class name
        pkgName = AndroidxName.of("android.support.design.test.", "TestClassName");
        assertEquals("android.support.design.test.TestClassName", pkgName.oldName());
        assertEquals("androidx.test.TestClassName", pkgName.newName());

        // Test a non-existent class name with a subpackage
        pkgName = AndroidxName.of("android.support.design.test.subpackage.", "TestClassName");
        assertEquals("android.support.design.test.subpackage.TestClassName", pkgName.oldName());
        assertEquals("androidx.test.subpackage.TestClassName", pkgName.newName());

        pkgName = AndroidxName.of("android.support.v4.widget.", "TestClassName");
        assertEquals("android.support.v4.widget.TestClassName", pkgName.oldName());
        assertEquals("androidx.widget.TestClassName", pkgName.newName());

        pkgName = AndroidxName.of("android.support.widget.", "TestClassName");
        assertEquals("android.support.widget.TestClassName", pkgName.oldName());
        assertEquals("androidx.widget.TestClassName", pkgName.newName());
    }

    @Test
    public void specificClass() {
        AndroidxName className = AndroidxName.of("android.support.v4.view.", "PagerTabStrip");
        assertEquals("android.support.v4.view.PagerTabStrip", className.oldName());
        assertEquals("androidx.widget.PagerTabStrip", className.newName());
    }

    @Test
    public void pkgName() {
        AndroidxName pkgName = AndroidxName.of("android.support.design.widget.");
        assertEquals("android.support.design.widget.", pkgName.oldName());
        assertEquals("androidx.widget.", pkgName.newName());

        // Test a non-existent name
        pkgName = AndroidxName.of("android.support.design.test.");
        assertEquals("android.support.design.test.", pkgName.oldName());
        assertEquals("androidx.test.", pkgName.newName());

        // Test a non-existent name with a subpackage
        pkgName = AndroidxName.of("android.support.design.test.subpackage.");
        assertEquals("android.support.design.test.subpackage.", pkgName.oldName());
        assertEquals("androidx.test.subpackage.", pkgName.newName());
    }

    @Test
    public void getNewName() {
        assertEquals(
                "androidx.widget.FloatingActionButton",
                AndroidxNameUtils.getNewName("android.support.design.widget.FloatingActionButton"));
        assertEquals(
                "unknown.package.FloatingActionButton",
                AndroidxNameUtils.getNewName("unknown.package.FloatingActionButton"));
        assertEquals("FloatingActionButton", AndroidxNameUtils.getNewName("FloatingActionButton"));
    }
}