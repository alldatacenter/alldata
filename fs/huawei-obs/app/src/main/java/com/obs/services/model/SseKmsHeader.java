/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model;

/**
 * SSE-KMS encryption headers
 */
public class SseKmsHeader {
    @SuppressWarnings("deprecation")
    private ServerEncryption encryption;

    private SSEAlgorithmEnum sseAlgorithm = SSEAlgorithmEnum.KMS;

    private String kmsKeyId;

    private String context;

    private String projectId;

    /**
     * Obtain the project ID.
     * 
     * @return projectId Project ID
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * Set the project ID.
     * 
     * @param projectId
     *            Project ID
     */
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    /**
     * Obtain the encryption algorithm type. Only kms is supported.
     * 
     * @return Encryption algorithm type
     */
    @Deprecated
    public ServerEncryption getEncryption() {
        return encryption;
    }

    /**
     * Set the encryption algorithm type. Only kms is supported.
     * 
     * @param encryption
     *            Encryption algorithm type
     */
    @Deprecated
    public void setEncryption(ServerEncryption encryption) {
        this.encryption = encryption;
    }

    /**
     * Obtain the encryption algorithm type. Only KMS is supported.
     * 
     * @return Encryption algorithm type
     */
    public SSEAlgorithmEnum getSSEAlgorithm() {
        return sseAlgorithm;
    }

    /**
     * Obtain the master key used in the SSE-KMS mode. If the value is blank,
     * the default master key will be used.
     * 
     * @return Master key used in the SSE-KMS mode
     */
    public String getKmsKeyId() {
        return kmsKeyId;
    }

    /**
     * Set the master key used in the SSE-KMS mode. If the value is blank, the
     * default master key will be used.
     * 
     * @param kmsKeyId
     *            Master key used in the SSE-KMS mode
     */
    public void setKmsKeyId(String kmsKeyId) {
        this.kmsKeyId = kmsKeyId;
    }

    @Deprecated
    public String getContext() {
        return context;
    }

    @Deprecated
    public void setContext(String context) {
        this.context = context;
    }

    @Override
    public String toString() {
        return "SseKmsHeader [encryption=" + encryption + ", kmsKeyId=" + kmsKeyId + ", context=" + context + "]";
    }

}
