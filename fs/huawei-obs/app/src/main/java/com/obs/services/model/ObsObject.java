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

import java.io.InputStream;

/**
 * Objects in OBS
 */
@SuppressWarnings("deprecation")
public class ObsObject extends S3Object {

    /**
     * Obtain the name of the bucket to which the object belongs.
     * 
     * @return Name of the bucket to which the object belongs
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Set the bucket to which the object belongs.
     * 
     * @param bucketName
     *            Name of the bucket to which the object belongs
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Obtain the object name.
     * 
     * @return Object name
     */
    public String getObjectKey() {
        return objectKey;
    }

    /**
     * Set the object name.
     * 
     * @param objectKey
     *            Object name
     */
    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    /**
     * Obtain the object properties, including "content-type", "content-length",
     * and customized metadata.
     * 
     * @return Object properties
     */
    public ObjectMetadata getMetadata() {
        if (metadata == null) {
            this.metadata = new ObjectMetadata();
        }
        return metadata;
    }

    /**
     * Set the object properties, including "content-type", "content-length",
     * and customized metadata.
     * 
     * @param metadata
     *            Object properties
     */
    public void setMetadata(ObjectMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Obtain the data stream of the object.
     * 
     * @return Data stream of the object
     */
    public InputStream getObjectContent() {
        return objectContent;
    }

    /**
     * Set the data stream of the object.
     * 
     * @param objectContent
     *            Object data stream
     */
    public void setObjectContent(InputStream objectContent) {
        this.objectContent = objectContent;
    }

    /**
     * Obtain the owner of the object.
     * 
     * @return Owner of the object
     */
    public Owner getOwner() {
        return owner;
    }

    /**
     * Set the owner of the object.
     * 
     * @param owner
     *            Owner of the object
     */
    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "ObsObject [bucketName=" + bucketName + ", objectKey=" + objectKey + ", owner=" + owner + ", metadata="
                + metadata + ", objectContent=" + objectContent + "]";
    }
}
