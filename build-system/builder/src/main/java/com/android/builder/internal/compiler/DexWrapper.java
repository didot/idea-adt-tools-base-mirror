/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.builder.internal.compiler;

import static com.google.common.base.Preconditions.checkArgument;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.builder.core.DexOptions;
import com.android.builder.core.DexProcessBuilder;
import com.android.ide.common.process.ProcessException;
import com.android.ide.common.process.ProcessOutput;
import com.android.ide.common.process.ProcessOutputHandler;
import com.android.utils.ILogger;
import com.google.common.base.Objects;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.io.Closer;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper to access dx.jar through reflection.
 *
 * <p/>Since there is no proper api to call the method in the dex library, this wrapper is going to
 * access it through reflection.
 */
public class DexWrapper {

    private static final LoadingCache<File, LinkedBlockingDeque<DexWrapper>> CACHE;

    private static final String DEX_MAIN = "com.android.dx.command.dexer.Main";

    private static final String DEX_CONSOLE = "com.android.dx.command.DxConsole";

    private static final String DEX_ARGS = "com.android.dx.command.dexer.Main$Arguments";

    private static final String MAIN_RUN = "run";

    private File mDexJar;

    private Method mRunMethod;

    private Method mSetOut;

    private Constructor<?> mArgConstructor;

    private Field mArgOutName;

    private Field mArgVerbose;

    private Field mOptimize;

    private Field mMultiDex;

    private Field mMainDexListFile;

    private Field mArgJarOutput;

    private Field mArgFileNames;

    private Field mConsoleOut;

    private Field mConsoleErr;

    private Field mNumThreads;

    private Field mForceJumbo;

    static {
        CACHE = CacheBuilder.newBuilder()
                .expireAfterWrite(3, TimeUnit.HOURS)
                .build(new CacheLoader<File, LinkedBlockingDeque<DexWrapper>>() {
                    @Override
                    public LinkedBlockingDeque<DexWrapper> load(@NonNull File jarFile) throws Exception {
                        int poolSize = Integer.getInteger("android.dexerPoolSize", 4);

                        LinkedBlockingDeque<DexWrapper> deque =
                                new LinkedBlockingDeque<DexWrapper>(poolSize);

                        for (int i = 0; i < poolSize; i++) {
                            deque.push(new DexWrapper(jarFile));
                        }

                        return deque;
                    }
                });
    }

    /**
     * Get an instance of {@link DexWrapper} for the given dx.jar file.
     *
     * <p>Can block if the whole dexer pool is in use.
     */
    public static DexWrapper obtain(File jarFile) {
        try {
            return CACHE.get(jarFile).takeFirst();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Releases the given {@link DexWrapper} back to the pool, so that other threads can use it.
     */
    public void release() {
        try {
            CACHE.get(mDexJar).putFirst(this);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private DexWrapper(File jarFile) {
        mDexJar = jarFile;
    }

    /**
     * Loads the dex library from a file path.
     *
     * @param dxJarFile the location of the dx.jar file.
     * @param logger
     */
    private void loadDex(@NonNull File dxJarFile, ILogger logger) {
        logger.info("Loading jar into dexer: %s", mDexJar.getAbsolutePath());

        try {
            if (!dxJarFile.isFile()) {
                throw new RuntimeException("dx.jar not found at : " + dxJarFile);
            }
            URL url = dxJarFile.toURI().toURL();
            @SuppressWarnings("resource")
            URLClassLoader loader = new URLClassLoader(new URL[]{url},
                    DexWrapper.class.getClassLoader());
            // get the classes.
            Class<?> mainClass = loader.loadClass(DEX_MAIN);
            Class<?> consoleClass = loader.loadClass(DEX_CONSOLE);
            Class<?> argClass = loader.loadClass(DEX_ARGS);
            Class<?> systemClass = loader.loadClass("java.lang.System");
            // now get the fields/methods we need
            mRunMethod = mainClass.getMethod(MAIN_RUN, argClass);
            mArgConstructor = argClass.getConstructor();
            mArgOutName = argClass.getField("outName");
            mArgJarOutput = argClass.getField("jarOutput");
            mArgFileNames = argClass.getField("fileNames");
            mArgVerbose = argClass.getField("verbose");
            mOptimize = argClass.getField("optimize");
            mMultiDex = argClass.getField("multiDex");
            mForceJumbo = argClass.getField("forceJumbo");
            mMainDexListFile = argClass.getField("mainDexListFile");
            mNumThreads = argClass.getField("numThreads");
            mConsoleOut = consoleClass.getField("out");
            mConsoleErr = consoleClass.getField("err");
            mSetOut = systemClass.getMethod("setOut", loader.loadClass("java.io.PrintStream"));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Runs the dex command. The wrapper must have been initialized via {@link #loadDex(File, ILogger)}
     * first.
     *
     * @return the integer return code of com.android.dx.command.dexer.Main.run()
     */
    public synchronized int run(
            @NonNull DexProcessBuilder processBuilder,
            @NonNull DexOptions dexOptions,
            @NonNull ProcessOutputHandler outputHandler,
            @NonNull ILogger logger) throws IOException, ProcessException {
        if (mRunMethod == null) {
            loadDex(mDexJar, logger);
        }

        assert mArgOutName != null;
        assert mArgJarOutput != null;
        assert mArgFileNames != null;
        assert mArgVerbose != null;
        assert mConsoleOut != null;
        assert mConsoleErr != null;

        ProcessOutput processOutput = outputHandler.createOutput();

        Closer closer = Closer.create();
        try {
            PrintStream err = closer.register(new PrintStream(processOutput.getErrorOutput()));
            PrintStream out = closer.register(new PrintStream(processOutput.getStandardOutput()));

            // Set the streams.
            mConsoleErr.set(null /* obj: static field */, err);
            mConsoleOut.set(null /* obj: static field */, out);
            mSetOut.invoke(null /* obj: static field */, out);

            // Create the Arguments object.
            Object args = mArgConstructor.newInstance();
            setOutput(args, processBuilder);
            setInputs(args, processBuilder);
            setOtherOptions(args, processBuilder, dexOptions);


            // Call the run method.
            Stopwatch stopwatch = Stopwatch.createStarted();
            Object res = mRunMethod.invoke(null /* obj: static method */, args);
            stopwatch.stop();

            logger.info(
                    "Dexing %s in-process: %s",
                    processBuilder.getOutputFile().getPath(),
                    stopwatch);

            if (res instanceof Integer) {
                return (Integer) res;
            }
            return -1;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            closer.close();
            outputHandler.handleOutput(processOutput);
        }
    }

    private void setInputs(Object args, @NonNull DexProcessBuilder processBuilder)
            throws IllegalAccessException, ProcessException {
        mArgFileNames.set(args, Iterables.toArray(processBuilder.getFilesToAdd(), String.class));
    }

    private void setOutput(Object args, @NonNull DexProcessBuilder processBuilder)
            throws IllegalAccessException {
        if (processBuilder.getOutputFile().isDirectory()) {
            mArgOutName.set(args, new File(processBuilder.getOutputFile(), "classes.dex").getPath());
            mArgJarOutput.set(args, false);
        } else {
            String outputFileAbsolutePath = processBuilder.getOutputFile().getAbsolutePath();
            mArgOutName.set(args, outputFileAbsolutePath);
            mArgJarOutput.set(args, outputFileAbsolutePath.endsWith(SdkConstants.DOT_JAR));
        }
    }

    private void setOtherOptions(
            @NonNull Object args,
            @NonNull DexProcessBuilder processBuilder,
            @NonNull DexOptions dexOptions) throws IllegalAccessException {
        mArgVerbose.set(args, processBuilder.isVerbose());
        mOptimize.set(args, !processBuilder.isNoOptimize());
        mMultiDex.set(args, processBuilder.isMultiDex());
        if (processBuilder.getMainDexList() != null) {
            mMainDexListFile.set(args, processBuilder.getMainDexList().getPath());
        }

        mNumThreads.set(args, Objects.firstNonNull(dexOptions.getThreadCount(), 4));
        mForceJumbo.set(args, dexOptions.getJumboMode());

        // TODO: remove it from DexProcessBuilder, it's never set to true?
        checkArgument(!processBuilder.isIncremental(), "--incremental is not supported.");
    }

}
