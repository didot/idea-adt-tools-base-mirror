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

package com.android.build.gradle.internal.transforms;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.build.api.transform.Context;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.QualifiedContent.ContentType;
import com.android.build.api.transform.QualifiedContent.Scope;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.LoggerWrapper;
import com.android.build.gradle.internal.pipeline.ExtendedContentType;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.builder.core.DefaultDexOptions;
import com.android.builder.core.DexOptions;
import com.android.builder.core.ErrorReporter;
import com.android.builder.dexing.ClassFileEntry;
import com.android.builder.dexing.ClassFileInput;
import com.android.builder.dexing.ClassFileInputs;
import com.android.builder.dexing.DexArchive;
import com.android.builder.dexing.DexArchiveBuilder;
import com.android.builder.dexing.DexArchiveBuilderConfig;
import com.android.builder.dexing.DexArchives;
import com.android.builder.dexing.DexerTool;
import com.android.builder.dexing.DxDexArchiveBuilder;
import com.android.builder.utils.FileCache;
import com.android.dx.command.dexer.DxContext;
import com.android.ide.common.blame.Message;
import com.android.ide.common.blame.ParsingProcessOutputHandler;
import com.android.ide.common.blame.parser.DexParser;
import com.android.ide.common.blame.parser.ToolOutputParser;
import com.android.ide.common.internal.WaitableExecutor;
import com.android.ide.common.process.ProcessException;
import com.android.ide.common.process.ProcessOutput;
import com.android.ide.common.process.ProcessOutputHandler;
import com.android.utils.FileUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.inject.Inject;
import org.gradle.tooling.BuildException;
import org.gradle.workers.IsolationMode;

/**
 * Transform that converts CLASS files to dex archives, {@link
 * com.android.builder.dexing.DexArchive}. This will consume {@link TransformManager#CONTENT_CLASS},
 * and for each of the inputs, corresponding dex archive will be produced.
 *
 * <p>This transform is incremental, only changed streams will be converted again. Additionally, if
 * an input stream is able to provide a list of individual files that were changed, only those files
 * will be processed. Their corresponding dex archives will be updated.
 */
public class DexArchiveBuilderTransform extends Transform {

    private static final LoggerWrapper logger =
            LoggerWrapper.getLogger(DexArchiveBuilderTransform.class);

    public static final int DEFAULT_BUFFER_SIZE_IN_KB = 100;

    public static final int NUMBER_OF_BUCKETS = 5;

    @NonNull private final DexOptions dexOptions;
    @NonNull private final ErrorReporter errorReporter;
    @Nullable private final FileCache userLevelCache;
    @VisibleForTesting @NonNull final WaitableExecutor executor;
    private final int minSdkVersion;
    @NonNull private final DexerTool dexer;
    @NonNull private final DexArchiveBuilderCacheHandler cacheHandler;
    private final boolean useGradleWorkers;
    private final int inBufferSize;
    private final int outBufferSize;

    public DexArchiveBuilderTransform(
            @NonNull DexOptions dexOptions,
            @NonNull ErrorReporter errorReporter,
            @Nullable FileCache userLevelCache,
            int minSdkVersion,
            @NonNull DexerTool dexer,
            boolean useGradleWorkers,
            @Nullable Integer inBufferSize,
            @Nullable Integer outBufferSize) {
        this.dexOptions = dexOptions;
        this.errorReporter = errorReporter;
        this.userLevelCache = userLevelCache;
        this.minSdkVersion = minSdkVersion;
        this.dexer = dexer;
        this.executor = WaitableExecutor.useGlobalSharedThreadPool();
        this.cacheHandler = new DexArchiveBuilderCacheHandler(userLevelCache, dexOptions);
        this.useGradleWorkers = useGradleWorkers;
        this.inBufferSize =
                (inBufferSize == null ? DEFAULT_BUFFER_SIZE_IN_KB : inBufferSize) * 1024;
        this.outBufferSize =
                (outBufferSize == null ? DEFAULT_BUFFER_SIZE_IN_KB : outBufferSize) * 1024;
    }

    @NonNull
    @Override
    public String getName() {
        return "dexBuilder";
    }

    @NonNull
    @Override
    public Set<ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @NonNull
    @Override
    public Set<ContentType> getOutputTypes() {
        return ImmutableSet.of(ExtendedContentType.DEX_ARCHIVE);
    }

    @NonNull
    @Override
    public Set<? super Scope> getScopes() {
        return TransformManager.SCOPE_FULL_WITH_IR_FOR_DEXING;
    }

    @NonNull
    @Override
    public Map<String, Object> getParameterInputs() {
        try {
            Map<String, Object> params = Maps.newHashMapWithExpectedSize(4);
            params.put("optimize", !dexOptions.getAdditionalParameters().contains("--no-optimize"));
            params.put("jumbo", dexOptions.getJumboMode());
            params.put("min-sdk-version", minSdkVersion);
            params.put("dex-builder-tool", dexer.name());

            return params;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public void transform(@NonNull TransformInvocation transformInvocation)
            throws TransformException, IOException, InterruptedException {
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
        Preconditions.checkNotNull(outputProvider, "Missing output provider.");

        if (dexOptions.getAdditionalParameters().contains("--no-optimize")) {
            logger.warning(DefaultDexOptions.OPTIMIZE_WARNING);
        }

        logger.verbose("Task is incremental : %b ", transformInvocation.isIncremental());

        ProcessOutputHandler outputHandler =
                new ParsingProcessOutputHandler(
                        new ToolOutputParser(new DexParser(), Message.Kind.ERROR, logger),
                        new ToolOutputParser(new DexParser(), logger),
                        errorReporter);

        if (!transformInvocation.isIncremental()) {
            outputProvider.deleteAll();
        }

        ProcessOutput processOutput = null;
        Multimap<QualifiedContent, File> cacheableItems = HashMultimap.create();
        try (Closeable ignored = processOutput = outputHandler.createOutput()) {
            // hash to detect duplicate inputs (due to issue with library and tests)
            final Set<String> hashes = Sets.newHashSet();

            for (TransformInput input : transformInvocation.getInputs()) {
                for (DirectoryInput dirInput : input.getDirectoryInputs()) {
                    logger.verbose("Dir input %s", dirInput.getFile().toString());
                    convertToDexArchive(
                            transformInvocation.getContext(),
                            hashes,
                            dirInput,
                            outputProvider);
                }

                for (JarInput jarInput : input.getJarInputs()) {
                    logger.verbose("Jar input %s", jarInput.getFile().toString());
                    List<File> dexArchives =
                            processJarInput(
                                    transformInvocation.getContext(),
                                    transformInvocation.isIncremental(),
                                    hashes,
                                    jarInput,
                                    outputProvider);
                    cacheableItems.putAll(jarInput, dexArchives);
                }
            }

            // all work items have been submitted, now wait for completion.
            if (useGradleWorkers) {
                transformInvocation.getContext().getWorkerExecutor().await();
            } else {
                executor.waitForAllTasks();
            }

            // if we are in incremental mode, delete all removed files.
            if (transformInvocation.isIncremental()) {
                for (TransformInput transformInput : transformInvocation.getInputs()) {
                    for (DirectoryInput directoryInput : transformInput.getDirectoryInputs()) {
                        File outputFile = getPreDexFolder(outputProvider, directoryInput);
                        try (DexArchive output = DexArchives.fromInput(outputFile.toPath())) {
                            for (Map.Entry<File, Status> fileStatusEntry :
                                    directoryInput.getChangedFiles().entrySet()) {
                                if (fileStatusEntry.getValue() == Status.REMOVED) {
                                    Path relativePath =
                                            directoryInput
                                                    .getFile()
                                                    .toPath()
                                                    .relativize(fileStatusEntry.getKey().toPath());
                                    output.removeFile(
                                            ClassFileEntry.withDexExtension(relativePath));
                                }
                            }
                        }
                    }
                }
            }

            // and finally populate the caches.
            if (!cacheableItems.isEmpty()) {
                cacheHandler.populateCache(cacheableItems);
            }

            logger.verbose("Done with all dex archive conversions");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransformException(e);
        } catch (Exception e) {
            throw new TransformException(e);
        } finally {
            if (processOutput != null) {
                try {
                    outputHandler.handleOutput(processOutput);
                } catch (ProcessException e) {
                    // ignore this one
                }
            }
        }
    }

    private List<File> processJarInput(
            @NonNull Context context,
            boolean isIncremental,
            @NonNull Set<String> hashes,
            @NonNull JarInput jarInput,
            TransformOutputProvider transformOutputProvider)
            throws Exception {
        if (!isIncremental) {
            if (jarInput.getFile().exists()) {
                return convertJarToDexArchive(context, hashes, jarInput, transformOutputProvider);
            } else {
                FileUtils.deleteIfExists(jarInput.getFile());
            }
        } else {
            if (jarInput.getStatus() == Status.REMOVED) {
                for (int bucketId = 0; bucketId < NUMBER_OF_BUCKETS; bucketId++) {
                    FileUtils.deleteIfExists(
                            getPreDexJar(transformOutputProvider, jarInput, bucketId));
                }
            } else if (jarInput.getStatus() == Status.ADDED
                    || jarInput.getStatus() == Status.CHANGED) {
                return convertJarToDexArchive(context, hashes, jarInput, transformOutputProvider);
            }
        }
        return ImmutableList.of();
    }

    private List<File> convertJarToDexArchive(
            @NonNull Context context,
            @NonNull Set<String> hashes,
            @NonNull JarInput toConvert,
            @NonNull TransformOutputProvider transformOutputProvider)
            throws Exception {

        File cachedVersion = cacheHandler.getCachedVersionIfPresent(toConvert);
        if (cachedVersion == null) {
            return convertToDexArchive(context, hashes, toConvert, transformOutputProvider);
        } else {
            File outputFile = getPreDexJar(transformOutputProvider, toConvert, null);
            Files.copy(
                    cachedVersion.toPath(),
                    outputFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            // no need to try to cache an already cached version.
            return ImmutableList.of();
        }
    }

    public static class DexConversionParameters implements Serializable {
        private final QualifiedContent input;
        private final String output;
        private final int numberOfBuckets;
        private final int buckedId;
        private final int minSdkVersion;
        private final List<String> dexAdditionalParameters;
        private final int inBufferSize;
        private final int outBufferSize;

        public DexConversionParameters(
                QualifiedContent input,
                File output,
                int numberOfBuckets,
                int buckedId,
                int minSdkVersion,
                List<String> dexAdditionalParameters,
                int inBufferSize,
                int outBufferSize) {
            this.input = input;
            this.numberOfBuckets = numberOfBuckets;
            this.buckedId = buckedId;
            this.output = output.toURI().toString();
            this.minSdkVersion = minSdkVersion;
            this.dexAdditionalParameters = dexAdditionalParameters;
            this.inBufferSize = inBufferSize;
            this.outBufferSize = outBufferSize;
        }

        public boolean belongsToThisBucket(Path path) {
            return (Math.abs(path.toString().hashCode()) % numberOfBuckets) == buckedId;
        }

        public boolean isDirectoryBased() {
            return input instanceof DirectoryInput;
        }
    }

    public static class DexConversionWorkAction implements Runnable {

        private final DexConversionParameters dexConversionParameters;

        @Inject
        public DexConversionWorkAction(DexConversionParameters dexConversionParameters) {
            this.dexConversionParameters = dexConversionParameters;
        }

        @Override
        public void run() {
            try {
                DexArchiveBuilder dexArchiveBuilder =
                        getDexArchiveBuilder(
                                dexConversionParameters.minSdkVersion,
                                dexConversionParameters.dexAdditionalParameters,
                                dexConversionParameters.inBufferSize,
                                dexConversionParameters.outBufferSize);

                Path rootFolder = dexConversionParameters.input.getFile().toPath();
                Predicate<Path> bucketFilter = dexConversionParameters::belongsToThisBucket;

                Predicate<Path> toProcess =
                        dexConversionParameters.isDirectoryBased()
                                ? path -> {
                                    Map<File, Status> changedFiles =
                                            ((DirectoryInput) dexConversionParameters.input)
                                                    .getChangedFiles();
                                    if (changedFiles.isEmpty()) {
                                        return true;
                                    }

                                    File resolved = rootFolder.resolve(path).toFile();
                                    Status status = changedFiles.get(resolved);
                                    return status == Status.ADDED || status == Status.CHANGED;
                                }
                                : path -> true;

                bucketFilter = bucketFilter.and(toProcess);

                // take bucketId'th entries from the input.
                File outputFile = new File(new URI(dexConversionParameters.output));
                try (ClassFileInput input = ClassFileInputs.fromPath(rootFolder);
                        DexArchive outputArchive = DexArchives.fromInput(outputFile.toPath())) {

                    dexArchiveBuilder.convert(input.entries(bucketFilter), outputArchive);
                }

            } catch (Exception e) {
                throw new BuildException(e.getMessage(), e);
            }
        }
    }

    private static DexArchiveBuilder getDexArchiveBuilder(
            int minSdkVersion,
            List<String> dexAdditionalParameters,
            int inBufferSize,
            int outBufferSize)
            throws IOException {

        boolean optimizedDex = !dexAdditionalParameters.contains("--no-optimize");
        DxContext dxContext = new DxContext(System.out, System.err);
        DexArchiveBuilderConfig config =
                new DexArchiveBuilderConfig(
                        dxContext,
                        optimizedDex,
                        inBufferSize,
                        minSdkVersion,
                        DexerTool.DX,
                        outBufferSize,
                        DexArchiveBuilderCacheHandler.isJumboModeEnabledForDx());

        return new DxDexArchiveBuilder(config);
    }

    private List<File> convertToDexArchive(
            @NonNull Context context,
            @NonNull Set<String> hashes,
            @NonNull QualifiedContent input,
            @NonNull TransformOutputProvider outputProvider)
            throws Exception {

        logger.verbose("Dexing {}", input.getFile().getAbsolutePath());
        String hash = DexArchiveBuilderCacheHandler.getFileHash(input.getFile());

        synchronized (hashes) {
            if (hashes.contains(hash)) {
                logger.verbose("Input with the same hash exists. Pre-dexing skipped.");
                return ImmutableList.of();
            }

            hashes.add(hash);
        }

        ImmutableList.Builder<File> dexArchives = ImmutableList.builder();
        for (int bucketId = 0; bucketId < NUMBER_OF_BUCKETS; bucketId++) {

            File preDexOutputFile = getPreDexFile(outputProvider, input, bucketId);
            dexArchives.add(preDexOutputFile);
            DexConversionParameters parameters =
                    new DexConversionParameters(
                            input,
                            preDexOutputFile,
                            NUMBER_OF_BUCKETS,
                            bucketId,
                            minSdkVersion,
                            dexOptions.getAdditionalParameters(),
                            inBufferSize,
                            outBufferSize);

            if (useGradleWorkers) {
                context.getWorkerExecutor()
                        .submit(
                                DexConversionWorkAction.class,
                                configuration -> {
                                    configuration.setIsolationMode(IsolationMode.NONE);
                                    configuration.setParams(parameters);
                                });
            } else {
                executor.execute(
                        () -> {
                            new DexConversionWorkAction(parameters).run();
                            return null;
                        });
            }
        }
        return dexArchives.build();
    }

    @NonNull
    private static File getPreDexFile(
            @NonNull TransformOutputProvider output,
            @NonNull QualifiedContent qualifiedContent,
            int bucketId) {

        return qualifiedContent.getFile().isDirectory()
                ? getPreDexFolder(output, (DirectoryInput) qualifiedContent)
                : getPreDexJar(output, (JarInput) qualifiedContent, bucketId);
    }

    @NonNull
    private static File getPreDexJar(
            @NonNull TransformOutputProvider output,
            @NonNull JarInput qualifiedContent,
            @Nullable Integer bucketId) {

        File contentLocation =
                output.getContentLocation(
                        qualifiedContent.getName() + (bucketId == null ? "" : ("-" + bucketId)),
                        ImmutableSet.of(ExtendedContentType.DEX_ARCHIVE),
                        qualifiedContent.getScopes(),
                        Format.JAR);

        FileUtils.mkdirs(contentLocation.getParentFile());
        return contentLocation;
    }

    @NonNull
    private static File getPreDexFolder(
            @NonNull TransformOutputProvider output, @NonNull DirectoryInput directoryInput) {

        return FileUtils.mkdirs(
                output.getContentLocation(
                        directoryInput.getName(),
                        ImmutableSet.of(ExtendedContentType.DEX_ARCHIVE),
                        directoryInput.getScopes(),
                        Format.DIRECTORY));
    }
}