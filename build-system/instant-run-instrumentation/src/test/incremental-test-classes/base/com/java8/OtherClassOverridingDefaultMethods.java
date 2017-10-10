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

package com.java8;

public class OtherClassOverridingDefaultMethods implements SubInterfaceWithDefault {
    @Override
    public String otherMethod() {
        return defaultedMethod() + "X";
    }

    @Override
    public String defaultedMethod() {
        return "otherDefault" + getClass().getName();
    }

    @Override
    public String someMethod() {
        return "someOther" + getClass().getName();
        // uncomment once we support grand parent methods.
        //return "someOther" + getClass().getName() + SubInterfaceWithDefault.super.finalMethod();
    }
}