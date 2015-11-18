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

package com.android.repository.impl.manager;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.repository.api.FallbackLocalRepoLoader;
import com.android.repository.api.LocalPackage;
import com.android.repository.api.ProgressIndicator;
import com.android.repository.api.RepoManager;
import com.android.repository.api.Repository;
import com.android.repository.api.SchemaModule;
import com.android.repository.impl.meta.LocalPackageImpl;
import com.android.repository.impl.meta.SchemaModuleUtil;
import com.android.repository.io.FileOp;
import com.android.repository.io.FileOpUtils;
import com.google.common.collect.Maps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;

/**
 * A utility class that finds {@link LocalPackage}s under a given path based on {@code package.xml}
 * files.
 */
public final class LocalRepoLoader {

    /**
     * The name of the package metadata file we can read.
     */
    public static final String PACKAGE_XML_FN = "package.xml";

    /**
     * The maximum depth we'll descend into the directory tree while looking for packages. TODO:
     * adjust once the path of the current deepest package is known (e.g. maven packages).
     */
    private static final int MAX_SCAN_DEPTH = 10;

    /**
     * Cache of found packages. TODO: this isn't really used in the current code. Simplify by
     * removing if wider adoption of the new APIs doesn't turn up a need for it. This applies to all
     * the fields in this class.
     */
    private Map<String, LocalPackage> mPackages = null;

    /**
     * Directory under which we look for packages.
     */
    private final File mRoot;

    private final RepoManager mRepoManager;

    private final FileOp mFop;

    /**
     * If we can't find a package in a directory, we ask mFallback to find one. If it does, we write
     * out a {@code package.xml} so we can read it next time.
     */
    private FallbackLocalRepoLoader mFallback;

    /**
     * Constructor. Probably should only be used within repository framework.
     *
     * @param root     The root directory under which we'll look for packages.
     * @param manager  A RepoManager, notably containing the {@link SchemaModule}s we'll use for
     *                 reading and writing {@link LocalPackage}s
     * @param fallback The {@link FallbackLocalRepoLoader} we'll use if we can't find a package in a
     *                 directory.
     * @param fop      The {@link FileOp} to use for file operations. Should be
     *                 {@link FileOpUtils#create()} for normal operation.
     */
    public LocalRepoLoader(@NonNull File root, @NonNull RepoManager manager,
            @Nullable FallbackLocalRepoLoader fallback, @NonNull FileOp fop) {
        mRoot = root;
        mRepoManager = manager;
        mFop = fop;
        mFallback = fallback;
    }

    /**
     * Gets our packages, loading them if necessary.
     *
     * @param progress A {@link ProgressIndicator} used to show progress (unimplemented) and
     *                 logging.
     * @return A map of install path to {@link LocalPackage}, containing all the packages found in
     * the given root.
     */
    @NonNull
    public Map<String, LocalPackage> getPackages(@NonNull ProgressIndicator progress) {
        if (mPackages == null) {
            Map<String, LocalPackage> packages = Maps.newHashMap();
            collectPackages(progress, packages, mRoot, 0);
            mPackages = packages;
        }
        return Collections.unmodifiableMap(mPackages);
    }

    /**
     * Collect packages under the given root into {@code collector}.
     *
     * @param progress  {@link ProgressIndicator} for logging.
     * @param collector The collector.
     * @param root      Directory we're looking in.
     * @param depth     The depth we've descended to so far. Once we reach {@link #MAX_SCAN_DEPTH}
     *                  we'll stop recursing.
     */
    private void collectPackages(@NonNull ProgressIndicator progress,
            @NonNull Map<String, LocalPackage> collector, @NonNull File root, int depth) {
        if (depth > MAX_SCAN_DEPTH) {
            return;
        }
        File packageXml = new File(root, PACKAGE_XML_FN);
        LocalPackage p = null;
        if (mFop.exists(packageXml)) {
            p = parsePackage(packageXml, progress);
        } else if (mFallback != null) {
            p = mFallback.parseLegacyLocalPackage(root, progress);
            if (p != null) {
                writePackage(p, packageXml, progress);
            }
        }
        if (p != null) {
            collector.put(p.getPath(), p);
        } else {
            for (File f : mFop.listFiles(root)) {
                if (mFop.isDirectory(f)) {
                    collectPackages(progress, collector, f, depth + 1);
                }
            }
        }
    }

    /**
     * If the {@link FallbackLocalRepoLoader} finds a package, we write out a package.xml so we can
     * load it next time without falling back.
     *
     * @param p          The {@link LocalPackage} to write out.
     * @param packageXml The destination to write to.
     * @param progress   {@link ProgressIndicator} for logging.
     */
    private void writePackage(@NonNull LocalPackage p, @NonNull File packageXml,
            @NonNull ProgressIndicator progress) {
        // We need a LocalPackageImpl to be able to save it.
        LocalPackageImpl impl = LocalPackageImpl.create(p, mRepoManager);
        OutputStream fos = null;
        try {
            fos = mFop.newFileOutputStream(packageXml);
            Repository repo = impl.createFactory().createRepositoryType();
            repo.setLocalPackage(impl);
            repo.addLicense(impl.getLicense());

            SchemaModuleUtil.marshal(p.getTypeDetails().createFactory().generateElement(repo),
                    mRepoManager.getSchemaModules(), fos,
                    mRepoManager.getResourceResolver(progress), progress);
        } catch (FileNotFoundException e) {
            progress.logWarning("File not found while marshalling " + packageXml, e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // ignore.
                }
            }
        }
    }

    /**
     * Unmarshal a package.xml file and extract the {@link LocalPackage}.
     */
    @Nullable
    private LocalPackage parsePackage(@NonNull File packageXml,
            @NonNull ProgressIndicator progress) {
        Repository repo;
        try {
            repo = (Repository) SchemaModuleUtil.unmarshal(mFop.newFileInputStream(packageXml),
                    mRepoManager.getSchemaModules(), mRepoManager.getResourceResolver(progress),
                    progress);
        } catch (FileNotFoundException e) {
            // This shouldn't ever happen
            progress.logError(String.format("XML file %s doesn't exist", packageXml), e);
            return null;
        }
        if (repo == null) {
            progress.logWarning(String.format("Failed to parse %s", packageXml));
            return null;
        } else {
            LocalPackage p = repo.getLocalPackage();
            if (p == null) {
                progress.logWarning("Didn't find any local package in repository");
                return null;
            }
            p.setInstalledPath(packageXml.getParentFile());
            return p;
        }
    }
}
