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

import java.io.Serializable;

import com.qcloud.cos.internal.CosServiceRequest;

public class GetObjectAclRequest extends CosServiceRequest implements Serializable {
    /** The name of the bucket containing the object whose ACL is being set. */
    private String bucketName;

    /** The name of the object whose ACL is being set. */
    private String key;

    /** The version ID of the object version whose ACL is being set. */
    private String versionId;



    public GetObjectAclRequest(String bucketName, String key) {
        this(bucketName, key, null);
    }

    private GetObjectAclRequest(String bucketName, String key, String versionId) {
        super();
        this.bucketName = bucketName;
        this.key = key;
        this.versionId = versionId;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public GetObjectAclRequest withBucketName(String bucketName) {
        this.bucketName = bucketName;
        return this;
    }


    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public GetObjectAclRequest withKey(String key) {
        this.key = key;
        return this;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public GetObjectAclRequest withVersionId(String versionId) {
        this.versionId = versionId;
        return this;
    }

}
