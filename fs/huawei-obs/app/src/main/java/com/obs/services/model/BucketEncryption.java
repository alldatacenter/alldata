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
**/

package com.obs.services.model;

/**
 * Bucket encryption information. Only the SSE-KMS encryption is supported.
 *
 */
public class BucketEncryption extends HeaderResponse {

    private SSEAlgorithmEnum sseAlgorithm;

    private String kmsKeyId;

    public BucketEncryption() {
    }

    /**
     * Constructor
     * 
     * @param sseAlgorithm
     *            Bucket encryption algorithm
     */
    public BucketEncryption(SSEAlgorithmEnum sseAlgorithm) {
        this.sseAlgorithm = sseAlgorithm;
    }

    /**
     * Obtain the bucket encryption algorithm.
     * 
     * @return Bucket encryption algorithm
     */
    public SSEAlgorithmEnum getSseAlgorithm() {
        return sseAlgorithm;
    }

    /**
     * Set the bucket encryption algorithm.
     * 
     * @param sseAlgorithm
     *            Bucket encryption algorithm
     */
    public void setSseAlgorithm(SSEAlgorithmEnum sseAlgorithm) {
        this.sseAlgorithm = sseAlgorithm;
    }

    /**
     * Obtain the master key used in the SSE-KMS encryption. If the value is
     * blank, the default master key is used.
     * 
     * @return Master key used in the SSE-KMS encryption
     */
    public String getKmsKeyId() {
        return kmsKeyId;
    }

    /**
     * Set the master key used in the SSE-KMS encryption. If the value is blank,
     * the default master key will be used.
     * 
     * @param kmsKeyId
     *            Master key used in the SSE-KMS encryption
     */
    public void setKmsKeyId(String kmsKeyId) {
        this.kmsKeyId = kmsKeyId;
    }

    @Override
    public String toString() {
        return "BucketEncryption [sseAlgorithm=" + sseAlgorithm + ", kmsKeyId=" + kmsKeyId + "]";
    }

}
