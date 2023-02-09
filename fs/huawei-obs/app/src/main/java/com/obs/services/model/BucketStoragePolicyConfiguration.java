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
 * Bucket storage policy
 */
public class BucketStoragePolicyConfiguration extends HeaderResponse {

    private StorageClassEnum storageClass;

    /**
     * Constructor
     * 
     * @param storageClass
     *            Bucket storage class
     */
    @Deprecated
    public BucketStoragePolicyConfiguration(String storageClass) {
        this.storageClass = StorageClassEnum.getValueFromCode(storageClass);
    }

    /**
     * Constructor
     * 
     * @param storageClass
     *            Bucket storage class
     */
    public BucketStoragePolicyConfiguration(StorageClassEnum storageClass) {
        this.storageClass = storageClass;
    }

    public BucketStoragePolicyConfiguration() {

    }

    @Override
    public String toString() {
        return "BucketStoragePolicyConfiguration [storageClass=" + storageClass + "]";
    }

    /**
     * Obtain the bucket storage class.
     * 
     * @return storageClass Bucket storage class
     * @see #getBucketStorageClass()
     */
    @Deprecated
    public String getStorageClass() {
        return this.storageClass != null ? this.storageClass.getCode() : null;
    }

    /**
     * Set the bucket storage class.
     * 
     * @param storageClass
     *            Bucket storage class
     * @see #setBucketStorageClass(StorageClassEnum storageClass)
     */
    @Deprecated
    public void setStorageClass(String storageClass) {
        this.storageClass = StorageClassEnum.getValueFromCode(storageClass);
    }

    /**
     * Obtain the bucket storage class.
     * 
     * @return storageClass Bucket storage class
     */
    public StorageClassEnum getBucketStorageClass() {
        return storageClass;
    }

    /**
     * Set the bucket storage class.
     * 
     * @param storageClass
     *            Bucket storage class
     */
    public void setBucketStorageClass(StorageClassEnum storageClass) {
        this.storageClass = storageClass;
    }

}
