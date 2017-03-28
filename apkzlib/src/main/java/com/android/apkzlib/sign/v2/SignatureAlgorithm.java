/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.apkzlib.sign.v2;

import com.android.apkzlib.utils.ApkZLibPair;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;

/**
 * APK Signature Scheme v2 content digest algorithm.
 */
public enum SignatureAlgorithm {
    /**
     * RSASSA-PSS with SHA2-256 digest, SHA2-256 MGF1, 32 bytes of salt, trailer: 0xbc, content
     * digested using SHA2-256 in 1 MB chunks.
     */
    RSA_PSS_WITH_SHA256(
            0x0101,
            ContentDigestAlgorithm.CHUNKED_SHA256,
            new ApkZLibPair<>("SHA256withRSA/PSS",
                    new PSSParameterSpec(
                            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 256 / 8, 1))),

    /**
     * RSASSA-PSS with SHA2-512 digest, SHA2-512 MGF1, 64 bytes of salt, trailer: 0xbc, content
     * digested using SHA2-512 in 1 MB chunks.
     */
    RSA_PSS_WITH_SHA512(
            0x0102,
            ContentDigestAlgorithm.CHUNKED_SHA512,
            new ApkZLibPair<>(
                    "SHA512withRSA/PSS",
                    new PSSParameterSpec(
                            "SHA-512", "MGF1", MGF1ParameterSpec.SHA512, 512 / 8, 1))),

    /** RSASSA-PKCS1-v1_5 with SHA2-256 digest, content digested using SHA2-256 in 1 MB chunks. */
    RSA_PKCS1_V1_5_WITH_SHA256(
            0x0103,
            ContentDigestAlgorithm.CHUNKED_SHA256,
            new ApkZLibPair<>("SHA256withRSA", null)),

    /** RSASSA-PKCS1-v1_5 with SHA2-512 digest, content digested using SHA2-512 in 1 MB chunks. */
    RSA_PKCS1_V1_5_WITH_SHA512(
            0x0104,
            ContentDigestAlgorithm.CHUNKED_SHA512,
            new ApkZLibPair<>("SHA512withRSA", null)),

    /** ECDSA with SHA2-256 digest, content digested using SHA2-256 in 1 MB chunks. */
    ECDSA_WITH_SHA256(
            0x0201,
            ContentDigestAlgorithm.CHUNKED_SHA256,
            new ApkZLibPair<>("SHA256withECDSA", null)),

    /** ECDSA with SHA2-512 digest, content digested using SHA2-512 in 1 MB chunks. */
    ECDSA_WITH_SHA512(
            0x0202,
            ContentDigestAlgorithm.CHUNKED_SHA512,
            new ApkZLibPair<>("SHA512withECDSA", null)),

    /** DSA with SHA2-256 digest, content digested using SHA2-256 in 1 MB chunks. */
    DSA_WITH_SHA256(
            0x0301,
            ContentDigestAlgorithm.CHUNKED_SHA256,
            new ApkZLibPair<>("SHA256withDSA", null));

    private final int mId;
    private final ContentDigestAlgorithm mContentDigestAlgorithm;
    private final ApkZLibPair<String, ? extends AlgorithmParameterSpec> mJcaSignatureAlgAndParams;

    private SignatureAlgorithm(int id,
            ContentDigestAlgorithm contentDigestAlgorithm,
            ApkZLibPair<String, ? extends AlgorithmParameterSpec> jcaSignatureAlgAndParams) {
        mId = id;
        mContentDigestAlgorithm = contentDigestAlgorithm;
        mJcaSignatureAlgAndParams = jcaSignatureAlgAndParams;
    }

    /**
     * Returns the ID of this signature algorithm as used in APK Signature Scheme v2 wire format.
     */
    int getId() {
        return mId;
    }

    /**
     * Returns the content digest algorithm associated with this signature algorithm.
     */
    ContentDigestAlgorithm getContentDigestAlgorithm() {
        return mContentDigestAlgorithm;
    }

    /**
     * Returns the {@link java.security.Signature} algorithm and the {@link AlgorithmParameterSpec}
     * (or null if not needed) to parameterize the {@code Signature}.
     */
    ApkZLibPair<String, ? extends AlgorithmParameterSpec> getJcaSignatureAlgorithmAndParams() {
        return mJcaSignatureAlgAndParams;
    }
}
