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

package com.android.builder.internal.packaging.sign;

import com.android.annotations.NonNull;
import com.google.common.base.Verify;

/**
 * Message digest algorithms.
 */
public enum DigestAlgorithm {
    /**
     * SHA-1 digest.
     * <p>
     * Android 2.3 (API Level 9) to 4.2 (API Level 17) (inclusive) do not support SHA-2
     * JAR signatures.
     * <p>
     * Moreover, platforms prior to API Level 18, without the additional
     * Digest-Algorithms attribute, only support SHA or SHA1 algorithm names in .SF and
     * MANIFEST.MF attributes.
     */
    SHA1(0, 1, "SHA1", "SHA-1"),

    /**
     * SHA-256 digest.
     */
    SHA256(18, 2, "SHA-256", "SHA-256");

    /**
     * The minimum SDK version this algorithm can be used with.
     */
    public final int minSdk;

    /**
     * The priority of the algorithm; the higher the priority, the better the algorithm.
     */
    public final int priority;

    /**
     * Name of algorithm for message digest.
     */
    @NonNull
    public final String messageDigestName;

    /**
     * Name of attribute in signature file with the manifest digest.
     */
    @NonNull
    public final String manifestAttributeName;

    /**
     * Name of attribute in entry (both manifest and signature file) with the entry's digest.
     */
    @NonNull
    public final String entryAttributeName;

    /**
     * Creates a digest algorithm.
     *
     * @param minSdk the minimum SDK version this algorithm can be used with
     * @param priority the priority of the algorithm; the higher the priority, the better the
     * algorithm
     * @param attributeName attribute name in the signature file
     * @param messageDigestName name of algorithm for message digest
     */
    DigestAlgorithm(int minSdk, int priority, @NonNull String attributeName,
            @NonNull String messageDigestName) {
        this.minSdk = minSdk;
        this.priority = priority;
        this.messageDigestName = messageDigestName;
        this.entryAttributeName = attributeName + "-Digest";
        this.manifestAttributeName = attributeName + "-Digest-Manifest";
    }

    /**
     * Finds the best digest algorithm applicable for a given SDK.
     *
     * @param minSdk the minimum SDK
     * @return the best algorithm found
     */
    @NonNull
    public static DigestAlgorithm findBest(int minSdk) {
        DigestAlgorithm bestSoFar = null;
        for (DigestAlgorithm da : values()) {
            if (da.minSdk <= minSdk && (bestSoFar == null || bestSoFar.priority < da.priority)) {
                bestSoFar = da;
            }
        }

        Verify.verifyNotNull(bestSoFar);
        assert bestSoFar != null;
        return bestSoFar;
    }
}
