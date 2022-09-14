/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.corebase.config;

import org.apache.inlong.tubemq.corebase.TBaseConstants;

public class TLSConfig {
    private boolean tlsEnable = false;
    private int tlsPort = TBaseConstants.META_DEFAULT_BROKER_TLS_PORT;
    private String tlsTrustStorePath = "";
    private String tlsTrustStorePassword = "";
    private boolean tlsTwoWayAuthEnable = false;
    private String tlsKeyStorePath = "";
    private String tlsKeyStorePassword = "";

    public TLSConfig() {

    }

    public boolean isTlsEnable() {
        return tlsEnable;
    }

    public void setTlsEnable(boolean tlsEnable) {
        this.tlsEnable = tlsEnable;
    }

    public int getTlsPort() {
        return tlsPort;
    }

    public void setTlsPort(int tlsPort) {
        this.tlsPort = tlsPort;
    }

    public String getTlsTrustStorePath() {
        return tlsTrustStorePath;
    }

    public void setTlsTrustStorePath(String tlsTrustStorePath) {
        this.tlsTrustStorePath = tlsTrustStorePath;
    }

    public String getTlsTrustStorePassword() {
        return tlsTrustStorePassword;
    }

    public void setTlsTrustStorePassword(String tlsTrustStorePassword) {
        this.tlsTrustStorePassword = tlsTrustStorePassword;
    }

    public boolean isTlsTwoWayAuthEnable() {
        return tlsTwoWayAuthEnable;
    }

    public void setTlsTwoWayAuthEnable(boolean tlsTwoWayAuthEnable) {
        this.tlsTwoWayAuthEnable = tlsTwoWayAuthEnable;
    }

    public String getTlsKeyStorePath() {
        return tlsKeyStorePath;
    }

    public void setTlsKeyStorePath(String tlsKeyStorePath) {
        this.tlsKeyStorePath = tlsKeyStorePath;
    }

    public String getTlsKeyStorePassword() {
        return tlsKeyStorePassword;
    }

    public void setTlsKeyStorePassword(String tlsKeyStorePassword) {
        this.tlsKeyStorePassword = tlsKeyStorePassword;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TLSConfig)) {
            return false;
        }
        TLSConfig tlsConfig = (TLSConfig) o;
        if (tlsEnable != tlsConfig.tlsEnable) {
            return false;
        }
        if (tlsPort != tlsConfig.tlsPort) {
            return false;
        }
        if (tlsTwoWayAuthEnable != tlsConfig.tlsTwoWayAuthEnable) {
            return false;
        }
        if (!tlsTrustStorePath.equals(tlsConfig.tlsTrustStorePath)) {
            return false;
        }
        if (!tlsTrustStorePassword.equals(tlsConfig.tlsTrustStorePassword)) {
            return false;
        }
        if (!tlsKeyStorePath.equals(tlsConfig.tlsKeyStorePath)) {
            return false;
        }
        return tlsKeyStorePassword.equals(tlsConfig.tlsKeyStorePassword);
    }

    @Override
    public int hashCode() {
        int result = (tlsEnable ? 1 : 0);
        result = 31 * result + tlsPort;
        result = 31 * result + tlsTrustStorePath.hashCode();
        result = 31 * result + tlsTrustStorePassword.hashCode();
        result = 31 * result + (tlsTwoWayAuthEnable ? 1 : 0);
        result = 31 * result + tlsKeyStorePath.hashCode();
        result = 31 * result + tlsKeyStorePassword.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return new StringBuilder(512)
                .append("\"TLSConfig\":{\"tlsEnable\":").append(tlsEnable)
                .append(",\"tlsPort\":").append(tlsPort)
                .append(",\"tlsTrustStorePath\":\"").append(tlsTrustStorePath)
                .append("\",\"tlsTrustStorePassword\":\"").append(tlsTrustStorePassword)
                .append("\",\"tlsTwoWayAuthEnable\":").append(tlsTwoWayAuthEnable)
                .append(",\"tlsKeyStorePath\":\"").append(tlsKeyStorePath)
                .append("\",\"tlsKeyStorePassword\":\"").append(tlsKeyStorePassword)
                .append("\"}").toString();
    }
}
