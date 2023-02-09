/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.model;

/**
 * Server-side Encryption Algorithm.
 */
public enum SSEAlgorithm {
    AES256("AES256"),
    KMS("KMS"),
    SM4("SM4"),
    ;

    private final String algorithm;

    public String getAlgorithm() {
        return algorithm;
    }

    private SSEAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public String toString() {
        return algorithm;
    }

    /**
     * Returns the SSEAlgorithm enum corresponding to the given string;
     * or null if and only if the given algorithm is null.
     *
     * @param algorithm
     *            The encryption algorithm.
     *
     * @return  The {@link SSEAlgorithm} enum.
     *
     * @throws IllegalArgumentException if the specified algorithm is not
     * supported.
     */
    public static SSEAlgorithm fromString(String algorithm) {
        if (algorithm == null)
            return null;
        for (SSEAlgorithm e: values()) {
            if (e.getAlgorithm().equals(algorithm))
                return e;
        }
        throw new IllegalArgumentException("Unsupported algorithm " + algorithm);
    }

    /**
     * Gets the default server side encryption algorithm, which is AES256.
     * @return The {@link SSEAlgorithm} enum.
     */
    public static SSEAlgorithm getDefault() {
        return AES256;
    }
    
}

