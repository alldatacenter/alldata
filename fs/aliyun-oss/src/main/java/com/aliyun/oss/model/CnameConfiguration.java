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

import java.util.Date;

public class CnameConfiguration {
    public enum CnameStatus {
        /**
         * Unknown status
         */
        Unknown("Unknown"),

        /**
         * Enabled
         */
        Enabled("Enabled"),

        /**
         * Disabled
         */
        Disabled("Disabled"),

        /**
         * Blocked
         */
        Blocked("Blocked"),

        /**
         * Forbidden
         */
        Forbidden("Forbidden");

        private String cnameStatusString;
        private CnameStatus(String cnameStatusString) { this.cnameStatusString = cnameStatusString; }

        @Override
        public String toString() { return this.cnameStatusString; }

        public static CnameStatus parse(String cnameStatusString) {
            for (CnameStatus st : CnameStatus.values()) {
                if (st.toString().equalsIgnoreCase(cnameStatusString)) {
                    return st;
                }
            }
            throw new IllegalArgumentException("Unable to parse " + cnameStatusString);
        }
    };

    public enum CertStatus {
        /**
         * Unknown status
         */
        Unknown("Unknown"),

        /**
         * Enabled
         */
        Enabled("Enabled"),

        /**
         * Disabled
         */
        Disabled("Disabled"),

        /**
         * Blocked
         */
        Blocked("Blocked"),

        /**
         * Forbidden
         */
        Forbidden("Forbidden");

        private String certStatusString;
        private CertStatus(String certStatusString) { this.certStatusString = certStatusString; }

        @Override
        public String toString() {
            return this.certStatusString;
        }

        public static CertStatus parse(String certStatusString) {
            for (CertStatus st : CertStatus.values()) {
                if (st.toString().equalsIgnoreCase(certStatusString)) {
                    return st;
                }
            }
            throw new IllegalArgumentException("Unable to parse " + certStatusString);
        }
    };

    public enum CertType {
        /**
         * Unknown type
         */
        Unknown("Unknown"),

        /**
         * Indicate the certificate is stored in CAS.
         */
        CAS("CAS"),

        /**
         * Indicate the certificate is uploaded by user
         */
        Upload("Upload");

        private String certTypeString;
        private CertType(String certTypeString) { this.certTypeString = certTypeString; }

        @Override
        public String toString() { return this.certTypeString; }

        public static CertType parse(String certTypeString) {
            for (CertType ct : CertType.values()) {
                if (ct.toString().equalsIgnoreCase(certTypeString)) {
                    return ct;
                }
            }
            throw new IllegalArgumentException("Unable to parse " + certTypeString);
        }
    }

    public CnameConfiguration() {
        status = CnameStatus.Unknown;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public CnameStatus getStatus() {
        return status;
    }

    public void setStatus(CnameStatus status) {
        this.status = status;
    }

    public Date getLastMofiedTime() {
        return lastMofiedTime;
    }

    public void setLastMofiedTime(Date lastMofiedTime) {
        this.lastMofiedTime = lastMofiedTime;
    }

    public Boolean getPurgeCdnCache() {
        return purgeCdnCache;
    }

    public void setPurgeCdnCache(Boolean purgeCdnCache) {
        this.purgeCdnCache = purgeCdnCache;
    }

    public CertStatus getCertStatus() { return certStatus; }

    public void setCertStatus(CertStatus certStatus) { this.certStatus = certStatus; }

    public CertType getCertType() { return certType; }

    public void setCertType(CertType certType) { this.certType = certType; }

    public String getCertId() { return certId; }

    public void setCertId(String certId) { this.certId = certId; }

    @Override
    public String toString() {
        return "CnameConfiguration [domain=" + domain + ", status=" + status + ", lastMofiedTime=" + lastMofiedTime
                + ", certType=" + certType + ", certId=" + certId + ", certStatus" + certStatus
                + "]";
    }

    private String domain;
    private CnameStatus status;
    private Date lastMofiedTime;
    private Boolean purgeCdnCache;
    private CertStatus certStatus;
    private CertType certType;
    private String certId;
}
