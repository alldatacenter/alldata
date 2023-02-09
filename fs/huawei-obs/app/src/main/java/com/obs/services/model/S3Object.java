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

import java.io.InputStream;

@Deprecated
public class S3Object {

    @Deprecated
    public static final String STANDARD = "STANDARD";

    @Deprecated
    public static final String STANDARD_IA = "STANDARD_IA";

    @Deprecated
    public static final String GLACIER = "GLACIER";

    protected String bucketName;

    protected String objectKey;

    protected Owner owner;

    protected ObjectMetadata metadata;

    protected InputStream objectContent;

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public ObjectMetadata getMetadata() {
        if (metadata == null) {
            this.metadata = new ObjectMetadata();
        }
        return metadata;
    }

    public void setMetadata(ObjectMetadata metadata) {
        this.metadata = metadata;
    }

    public InputStream getObjectContent() {
        return objectContent;
    }

    public void setObjectContent(InputStream objectContent) {
        this.objectContent = objectContent;
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "ObsObject [bucketName=" + bucketName + ", objectKey=" + objectKey + ", owner=" + owner + ", metadata="
                + metadata + ", objectContent=" + objectContent + "]";
    }
}
