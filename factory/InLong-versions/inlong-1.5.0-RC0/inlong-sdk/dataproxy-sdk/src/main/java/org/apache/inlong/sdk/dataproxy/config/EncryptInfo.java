/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy.config;

public class EncryptInfo {

    private String version;
    private byte[] aesKey;
    private String rsaEncryptedKey;

    public EncryptInfo(String version, String rsaEncryptedKey, byte[] aesKey) {
        this.version = version;
        this.rsaEncryptedKey = rsaEncryptedKey;
        this.aesKey = aesKey;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public byte[] getAesKey() {
        return aesKey;
    }

    public void setAesKey(byte[] aesKey) {
        this.aesKey = aesKey;
    }

    public String getRsaEncryptedKey() {
        return rsaEncryptedKey;
    }

    public void setRsaEncryptedKey(String rsaEncryptedKey) {
        this.rsaEncryptedKey = rsaEncryptedKey;
    }
}
