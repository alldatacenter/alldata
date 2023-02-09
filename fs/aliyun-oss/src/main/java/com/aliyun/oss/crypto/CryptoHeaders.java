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
package com.aliyun.oss.crypto;

public interface CryptoHeaders {
    public static String CRYPTO_KEY = "client-side-encryption-key";
    public static String CRYPTO_IV = "client-side-encryption-start";
    public static String CRYPTO_CEK_ALG = "client-side-encryption-cek-alg";
    public static String CRYPTO_WRAP_ALG = "client-side-encryption-wrap-alg";
    public static String CRYPTO_MATDESC = "client-side-encryption-matdesc";
    public static String CRYPTO_DATA_SIZE = "client-side-encryption-data-size";
    public static String CRYPTO_PART_SIZE = "client-side-encryption-part-size";
    public static String CRYPTO_UNENCRYPTION_CONTENT_LENGTH = "client-side-encryption-unencrypted-content-length";
    public static String CRYPTO_UNENCRYPTION_CONTENT_MD5 = "client-side-encryption-unencrypted-content-md5";
}
