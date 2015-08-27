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

package com.android.build.gradle.internal.transforms;

import static com.android.builder.model.AndroidProject.FD_INTERMEDIATES;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.build.gradle.internal.scope.VariantScope;
import com.android.build.transform.api.ScopedContent.ContentType;
import com.android.build.transform.api.ScopedContent.Format;
import com.android.build.transform.api.ScopedContent.Scope;
import com.android.build.transform.api.Transform;
import com.android.build.transform.api.TransformException;
import com.android.build.transform.api.TransformInput;
import com.android.build.transform.api.TransformOutput;
import com.android.ide.common.process.ProcessException;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import proguard.ParseException;

/**
 * Transform for multi-dex.
 *
 * This does not actually consume anything, rather it only reads streams and extract information
 * from them.
 */
public class MultiDexTransform extends BaseProguardAction implements Transform {

    @NonNull
    private final File manifestKeepListFile;
    @NonNull
    private final VariantScope variantScope;
    @Nullable
    private final File includeInMainDexJarFile;

    @NonNull
    private final File configFileOut;
    @NonNull
    private final File mainDexListFile;

    public MultiDexTransform(
            @NonNull File manifestKeepListFile,
            @NonNull VariantScope variantScope,
            @Nullable File includeInMainDexJarFile) {
        this.manifestKeepListFile = manifestKeepListFile;
        this.variantScope = variantScope;
        this.includeInMainDexJarFile = includeInMainDexJarFile;
        configFileOut = new File(variantScope.getGlobalScope().getBuildDir() + "/" + FD_INTERMEDIATES
                + "/multi-dex/" + variantScope.getVariantConfiguration().getDirName()
                + "/components.flags");
        mainDexListFile = variantScope.getMainDexListFile();
    }

    @NonNull
    @Override
    public String getName() {
        return "multidexlist";
    }

    @NonNull
    @Override
    public Set<ContentType> getInputTypes() {
        return Sets.immutableEnumSet(ContentType.CLASSES);
    }

    @NonNull
    @Override
    public Set<ContentType> getOutputTypes() {
        return Sets.immutableEnumSet(EnumSet.noneOf(ContentType.class));
    }

    @NonNull
    @Override
    public Set<Scope> getScopes() {
        return Sets.immutableEnumSet(
                Scope.PROJECT,
                Scope.PROJECT_LOCAL_DEPS,
                Scope.SUB_PROJECTS,
                Scope.SUB_PROJECTS_LOCAL_DEPS,
                Scope.EXTERNAL_LIBRARIES);
    }

    @NonNull
    @Override
    public Set<Scope> getReferencedScopes() {
        return TransformManager.EMPTY_SCOPES;
    }

    @NonNull
    @Override
    public Type getTransformType() {
        return Type.NO_OP;
    }

    @NonNull
    @Override
    public Format getOutputFormat() {
        return Format.SINGLE_FOLDER;
    }

    @NonNull
    @Override
    public Collection<File> getSecondaryFileInputs() {
        if (includeInMainDexJarFile != null) {
            return ImmutableList.of(includeInMainDexJarFile);
        }
        return ImmutableList.of();
    }

    @NonNull
    @Override
    public Collection<File> getSecondaryFileOutputs() {
        return Lists.newArrayList(mainDexListFile, configFileOut);
    }

    @NonNull
    @Override
    public Map<String, Object> getParameterInputs() {
        return ImmutableMap.of();
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(
            @NonNull Map<TransformInput, TransformOutput> inputOutputs,
            @NonNull List<TransformInput> referencedInputs,
            boolean isIncremental) throws IOException, TransformException {
        try {
            File input = verifyInputs(inputOutputs.keySet());
            shrinkWithProguard(input);
            computeList(input);
        } catch (ParseException e) {
            throw new TransformException(e);
        } catch (ProcessException e) {
            throw new TransformException(e);
        }
    }

    private static File verifyInputs(@NonNull Collection<TransformInput> inputs) {
        // Collect the inputs. There should be only one.
        List<File> inputFiles = Lists.newArrayList();

        for (TransformInput transformInput : inputs) {
            switch (transformInput.getFormat()) {
                case SINGLE_FOLDER:
                    inputFiles.addAll(transformInput.getFiles());
                    break;
                case MULTI_FOLDER:
                    for (File file : transformInput.getFiles()) {
                        File[] subStreams = file.listFiles();
                        if (subStreams != null) {
                            Collections.addAll(inputFiles, subStreams);
                        }
                    }
                    break;
                case SINGLE_JAR:
                    inputFiles.add(Iterables.getOnlyElement(transformInput.getFiles()));
                    break;
                default:
                    throw new RuntimeException("Unsupported ScopedContent.Format value: " + transformInput.getFormat().name());
            }
        }

        return Iterables.getOnlyElement(inputFiles);
    }

    private void shrinkWithProguard(@NonNull File input) throws IOException, ParseException {
        dontobfuscate();
        dontoptimize();
        dontpreverify();
        dontwarn();
        forceprocessing();
        applyConfigurationFile(manifestKeepListFile);

        // handle inputs
        libraryJar(findShrinkedAndroidJar());
        inJar(input);

        // outputs.
        outJar(variantScope.getProguardComponentsJarFile());
        printconfiguration(configFileOut);

        // run proguard
        runProguard();
    }

    @NonNull
    private File findShrinkedAndroidJar() {
        Preconditions.checkNotNull(
                variantScope.getGlobalScope().getAndroidBuilder().getTargetInfo());
        File shrinkedAndroid = new File(
                variantScope.getGlobalScope().getAndroidBuilder().getTargetInfo()
                        .getBuildTools()
                        .getLocation(),
                "lib" + File.separatorChar + "shrinkedAndroid.jar");

        if (!shrinkedAndroid.isFile()) {
            shrinkedAndroid = new File(
                    variantScope.getGlobalScope().getAndroidBuilder().getTargetInfo()
                            .getBuildTools().getLocation(),
                    "multidex" + File.separatorChar + "shrinkedAndroid.jar");
        }
        return shrinkedAndroid;
    }

    private void computeList(File _allClassesJarFile) throws ProcessException, IOException {
        // manifest components plus immediate dependencies must be in the main dex.
        Set<String> mainDexClasses = callDx(
                _allClassesJarFile,
                variantScope.getProguardComponentsJarFile());

        // add additional classes specified via a jar file.
        if (includeInMainDexJarFile != null) {
            // proguard shrinking is overly aggressive when it comes to removing
            // interface classes: even if an interface is implemented by a concrete
            // class, if no code actually references the interface class directly
            // (i.e., code always references the concrete class), proguard will
            // remove the interface class when shrinking.  This is problematic,
            // as the runtime verifier still needs the interface class to be
            // present, or the concrete class won't be valid.  Use a
            // ClassReferenceListBuilder here (only) to pull in these missing
            // interface classes.  Note that doing so brings in other unnecessary
            // stuff, too; next time we're low on main dex space, revisit this!
            mainDexClasses.addAll(callDx(_allClassesJarFile, includeInMainDexJarFile));
        }
/*
        TODO: figure this out this wasn't plugged-in in the previous version.
        if (manifestKeepListFile != null) {
            Set<String> mainDexList = new HashSet<String>(
                    Files.readLines(manifestKeepListFile, Charsets.UTF_8));
            mainDexClasses.addAll(mainDexList);
        }*/

        String fileContent = Joiner.on(System.getProperty("line.separator")).join(mainDexClasses);

        Files.write(fileContent, mainDexListFile, Charsets.UTF_8);

    }

    private Set<String> callDx(File allClassesJarFile, File jarOfRoots) throws ProcessException {
        return variantScope.getGlobalScope().getAndroidBuilder().createMainDexList(
                allClassesJarFile, jarOfRoots);
    }
}
