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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy.config;

import java.net.URLEncoder;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.codec.binary.Base64;
import org.apache.inlong.sdk.dataproxy.utils.EncryptUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by lamberliu on 2016/5/13.
 */
public class EncryptConfigEntry implements java.io.Serializable {
    private static final Logger logger = LoggerFactory.getLogger(EncryptConfigEntry.class);
    private String userName = "";
    private String version;
    private String pubKey;
    private byte[] desKey;
    private String rsaEncryptedKey;
    private AtomicLong lastUpdateTime = new AtomicLong(0);

    public EncryptConfigEntry(final String userName, final String version, final String pubKey) {
        this.userName = userName;
        this.version = version;
        this.pubKey = pubKey;
        this.desKey = null;
        this.rsaEncryptedKey = null;
        // this.rsaKey = EncryptUtil.loadPublicKeyByText(pubKey);
    }

    public String getVersion() {
        return version;
    }

    public String getPubKey() {
        return pubKey;
    }

    public String getUserName() {
        return userName;
    }

    public synchronized byte[] getDesKey() {
        if (desKey == null) {
            desKey = EncryptUtil.generateDesKey();
        }

        return desKey;
    }

    public String getRsaEncryptedKey() {
        if (rsaEncryptedKey == null) {
            RSAPublicKey rsaKey = EncryptUtil.loadPublicKeyByText(pubKey);
            try {
                byte[] encryptedKey = EncryptUtil.rsaEncrypt(rsaKey, getDesKey());
                String tmpKey = Base64.encodeBase64String(encryptedKey);
                rsaEncryptedKey = URLEncoder.encode(tmpKey, "utf8");
                this.lastUpdateTime.set(System.currentTimeMillis());
                return rsaEncryptedKey;
            } catch (Exception e) {
                logger.error("RSA Encrypt error {}", e);
                return null;
            }

        }
        return rsaEncryptedKey;
    }

    public EncryptInfo getRsaEncryptInfo() {
        EncryptInfo encryptInfo = null;
        long visitTime = this.lastUpdateTime.get();
        if (rsaEncryptedKey != null && (System.currentTimeMillis() - visitTime) <= 3 * 60 * 1000) {
            encryptInfo = new EncryptInfo(this.version, this.rsaEncryptedKey, this.desKey);
            if (visitTime == this.lastUpdateTime.get()) {
                return encryptInfo;
            }
            encryptInfo = null;
        }
        synchronized (this.lastUpdateTime) {
            if (visitTime == this.lastUpdateTime.get()) {
                RSAPublicKey rsaKey = EncryptUtil.loadPublicKeyByText(pubKey);
                this.desKey = EncryptUtil.generateDesKey();
                try {
                    byte[] encryptedKey = EncryptUtil.rsaEncrypt(rsaKey, this.desKey);
                    String tmpKey = Base64.encodeBase64String(encryptedKey);
                    rsaEncryptedKey = URLEncoder.encode(tmpKey, "utf8");
                    this.lastUpdateTime.set(System.currentTimeMillis());
                    return new EncryptInfo(this.version, this.rsaEncryptedKey, this.desKey);
                } catch (Throwable e) {
                    logger.error("getRsaEncryptInfo failure, RSA Encrypt error {}", e);
                    return null;
                }
            }
        }
        return new EncryptInfo(this.version, this.rsaEncryptedKey, this.desKey);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof EncryptConfigEntry)) {
            return false;
        }
        if (other == this) {
            return true;
        }
        EncryptConfigEntry info = (EncryptConfigEntry) other;
        return (this.userName.equals(info.getUserName()))
                && (this.version.equals(info.getVersion()))
                && (this.pubKey == info.getPubKey());
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setPubKey(String pubKey) {
        this.pubKey = pubKey;
    }

    public String toString() {
        return "{\"version\":\"" + version + "\",\"public_key\":\"" + pubKey + "\",\"groupId\":\"" + userName + "\"}";
    }

}
