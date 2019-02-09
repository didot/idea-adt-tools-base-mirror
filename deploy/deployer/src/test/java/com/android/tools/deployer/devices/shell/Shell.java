/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.deployer.devices.shell;

import com.android.tools.deployer.devices.FakeDevice;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Shell {

    private final Map<String, ShellCommand> commands;

    public Shell() {
        this.commands = new HashMap<>();
    }

    public String execute(FakeDevice device, String cmd, String[] args, InputStream input)
            throws IOException {

        // The most basic shell interpreter ever
        ShellCommand command = commands.get(cmd);
        if (command != null) {
            return command.execute(device, args, input);
        }
        // Adb does not return an error code, just this:
        return String.format("/system/bin/sh: %s: not found\n", cmd);
    }

    public void addComand(ShellCommand command) {
        commands.put(command.getExecutable(), command);
    }
}
