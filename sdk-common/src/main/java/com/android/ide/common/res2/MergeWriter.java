/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.ide.common.res2;

import com.android.annotations.NonNull;
import com.android.ide.common.internal.WaitableExecutor;

import java.io.File;
import java.util.concurrent.ExecutionException;

/**
 * A {@link MergeConsumer} that writes the result on the disk.
 */
public abstract class MergeWriter<I extends DataItem> implements MergeConsumer<I> {

    @NonNull
    private final File mRootFolder;
    @NonNull
    private final WaitableExecutor<Void> mExecutor;

    public MergeWriter(@NonNull File rootFolder) {
        mRootFolder = rootFolder;
        mExecutor = new WaitableExecutor<Void>(Runtime.getRuntime().availableProcessors());
    }

    @Override
    public void start() throws ConsumerException {
    }

    @Override
    public void end() throws ConsumerException {
        try {
            postWriteAction();

            getExecutor().waitForTasksWithQuickFail();
        } catch (InterruptedException e) {
            // if this thread was cancelled we need to cancel the rest of the executor tasks.
            getExecutor().cancelAllTasks();
            throw new ConsumerException(e);
        } catch (ExecutionException e) {
            // if a task fail, we also want to cancel the rest of the tasks.
            mExecutor.cancelAllTasks();
            // and return the first error
            throw new ConsumerException(e.getCause());
        }
    }

    /**
     * Called after all the items have been added/removed. This is called by {@link #end()}.
     * @throws ConsumerException
     */
    protected void postWriteAction() throws ConsumerException {
    }

    @NonNull
    protected WaitableExecutor<Void> getExecutor() {
        return mExecutor;
    }

    @NonNull
    protected File getRootFolder() {
        return mRootFolder;
    }
}
