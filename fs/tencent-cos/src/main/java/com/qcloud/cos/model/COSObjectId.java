/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 
 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.model;

import static com.qcloud.cos.model.InstructionFileId.DEFAULT_INSTRUCTION_FILE_SUFFIX;
import static com.qcloud.cos.model.InstructionFileId.DOT;

import java.io.Serializable;

public class COSObjectId implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String bucket;
    private final String key;
    /**
     * Optional and applicable only for get operation.
     */
    private final String versionId;

    public COSObjectId(String bucket, String key) {
        this(bucket, key, null);
    }

    /**
     * @param bucket the COS bucket name which must not be null
     * @param key the COS key name which must not be null
     * @param versionId optional version id
     */
    public COSObjectId(String bucket, String key, String versionId) {
        if (bucket == null || key == null)
            throw new IllegalArgumentException("bucket and key must be specified");
        this.bucket = bucket;
        this.key = key;
        this.versionId = versionId;
    }

    /**
     * @param builder must not be null.
     */
    public COSObjectId(COSObjectIdBuilder builder) {
        this.bucket = builder.getBucket();
        this.key = builder.getKey();
        this.versionId = builder.getVersionId();
    }

    public String getBucket() {
        return bucket;
    }

    public String getKey() {
        return key;
    }

    /**
     * Returns the version id which is optionally applicable for COS get (but not put) operations.
     */
    public String getVersionId() {
        return versionId;
    }
    /**
     * Returns the instruction file id of the default instruction file.
     */
    public InstructionFileId instructionFileId() {
        return instructionFileId(null);
    }

    /**
     * Returns the instruction file id of an instruction file with the given
     * suffix.
     */
    public InstructionFileId instructionFileId(String suffix) {
        String ifileKey = key + DOT;
        ifileKey += (suffix == null || suffix.trim().length() == 0)
                  ? DEFAULT_INSTRUCTION_FILE_SUFFIX
                  : suffix
                  ;
         return new InstructionFileId(bucket, ifileKey, versionId);
    }

    @Override
    public String toString() {
        return "bucket: " + bucket + ", key: " + key + ", versionId: "
                + versionId;
    }
}
