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

package com.android.build.gradle.internal.pipeline;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.android.annotations.NonNull;
import com.android.annotations.VisibleForTesting;
import com.android.annotations.concurrency.Immutable;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent.ContentType;
import com.android.build.api.transform.QualifiedContent.Scope;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.TransformInput;
import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Version of TransformStream handling input that is not generated by transforms.
 */
@Immutable
public class OriginalStream extends TransformStream {

    private static Supplier<Collection<File>> EMPTY_SUPPLIER = ImmutableList::of;

    private final Supplier<Collection<File>> jarFiles;
    private final Supplier<Collection<File>> folders;

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Set<ContentType> contentTypes = Sets.newHashSet();
        private Scope scope;
        private Supplier<Collection<File>> jarFiles;
        private Supplier<Collection<File>> folders;
        private List<? extends Object> dependencies;

        public OriginalStream build() {
            checkNotNull(scope);
            checkState(!contentTypes.isEmpty());

            return new OriginalStream(
                    ImmutableSet.copyOf(contentTypes),
                    scope,
                    jarFiles != null ? jarFiles : EMPTY_SUPPLIER,
                    folders != null ? folders : EMPTY_SUPPLIER,
                    dependencies != null ? dependencies : ImmutableList.of());
        }

        public Builder addContentTypes(@NonNull Set<ContentType> types) {
            this.contentTypes.addAll(types);
            return this;
        }

        public Builder addContentTypes(@NonNull ContentType... types) {
            this.contentTypes.addAll(Arrays.asList(types));
            return this;
        }

        public Builder addContentType(@NonNull ContentType type) {
            this.contentTypes.add(type);
            return this;
        }

        public Builder addScope(@NonNull Scope scope) {
            this.scope = scope;
            return this;
        }

        public Builder setJar(@NonNull final File jarFile) {
            this.jarFiles = Suppliers.ofInstance((Collection<File>) ImmutableList.of(jarFile));
            return this;
        }

        public Builder setJars(@NonNull Supplier<Collection<File>> jarSupplier) {
            this.jarFiles = jarSupplier;
            return this;
        }

        public Builder setFolder(@NonNull final File folder) {
            this.folders = Suppliers.ofInstance((Collection<File>) ImmutableList.of(folder));
            return this;
        }

        public Builder setFolders(@NonNull Supplier<Collection<File>> folderSupplier) {
            this.folders = folderSupplier;
            return this;
        }

        public Builder setDependencies(@NonNull List<? extends Object> dependencies) {
            this.dependencies = ImmutableList.copyOf(dependencies);
            return this;
        }

        public Builder setDependency(@NonNull Object dependency) {
            this.dependencies = ImmutableList.of(dependency);
            return this;
        }
    }

    private OriginalStream(
            @NonNull Set<ContentType> contentTypes,
            @NonNull Scope scope,
            @NonNull Supplier<Collection<File>> jarFiles,
            @NonNull Supplier<Collection<File>> folders,
            @NonNull List<? extends Object> dependencies) {
        super(contentTypes, Sets.immutableEnumSet(scope), dependencies);
        this.jarFiles = jarFiles;
        this.folders = folders;
    }

    @NonNull
    @Override
    List<File> getInputFiles() {
        Collection<File> list1 = jarFiles.get();
        Collection<File> list2 = folders.get();

        List<File> inputFiles = Lists.newArrayListWithCapacity(list1.size() + list2.size());
        inputFiles.addAll(list1);
        inputFiles.addAll(list2);
        return inputFiles;
    }

    private static class OriginalTransformInput extends IncrementalTransformInput {

        @Override
        protected boolean checkRemovedFolder(
                @NonNull Set<Scope> transformScopes,
                @NonNull Set<ContentType> transformInputTypes,
                @NonNull File file,
                @NonNull List<String> fileSegments) {
            // we can never detect if a random file was removed from this input.
            return false;
        }

        @Override
        boolean checkRemovedJarFile(
                @NonNull Set<Scope> transformScopes,
                @NonNull Set<ContentType> transformInputTypes,
                @NonNull File file,
                @NonNull List<String> fileSegments) {
            // we can never detect if a jar was removed from this input.
            return false;
        }
    }

    @NonNull
    @Override
    TransformInput asNonIncrementalInput() {
        Set<ContentType> contentTypes = getContentTypes();
        Set<Scope> scopes = getScopes();

        List<JarInput> jarInputs = jarFiles.get().stream()
                .map(file -> new ImmutableJarInput(
                        getUniqueInputName(file),
                        file,
                        Status.NOTCHANGED,
                        contentTypes,
                        scopes))
                .collect(Collectors.toList());

        List<DirectoryInput> directoryInputs = folders.get().stream()
                .map(file -> new ImmutableDirectoryInput(
                        getUniqueInputName(file),
                        file,
                        contentTypes,
                        scopes))
                .collect(Collectors.toList());

        return new ImmutableTransformInput(jarInputs, directoryInputs, null);
    }

    @NonNull
    @Override
    IncrementalTransformInput asIncrementalInput() {
        IncrementalTransformInput input = new OriginalTransformInput();

        Set<ContentType> contentTypes = getContentTypes();
        Set<Scope> scopes = getScopes();

        for (File file : jarFiles.get()) {
            input.addJarInput(new QualifiedContentImpl(
                    getUniqueInputName(file),
                    file,
                    contentTypes,
                    scopes));
        }

        for (File file : folders.get()) {
            input.addFolderInput(new MutableDirectoryInput(
                    getUniqueInputName(file),
                    file,
                    contentTypes,
                    scopes));
        }

        return input;
    }

    @NonNull
    private static String getUniqueInputName(@NonNull File file) {
        return Hashing.sha1().hashString(file.getPath(), Charsets.UTF_16LE).toString();
    }

    @Override
    TransformStream makeRestrictedCopy(
            @NonNull Set<ContentType> types,
            @NonNull Set<Scope> scopes) {
        if (!scopes.equals(getScopes())) {
            // since the content itself (jars and folders) don't have they own notion of scopes
            // we cannot do a restricted stream. However, since this stream is always created
            // with a single stream, this shouldn't happen.
            throw new UnsupportedOperationException("Cannot do a scope-restricted OriginalStream");
        }
        return new OriginalStream(
                types,
                Iterables.getOnlyElement(scopes),
                jarFiles,
                folders,
                getDependencies());
    }

    @VisibleForTesting
    Supplier<Collection<File>> getJarFiles() {
        return jarFiles;
    }

    @VisibleForTesting
    Supplier<Collection<File>> getFolders() {
        return folders;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("jarFiles", jarFiles.get())
                .add("folders", folders.get())
                .add("scopes", getScopes())
                .add("contentTypes", getContentTypes())
                .add("dependencies", getDependencies())
                .toString();
    }
}
