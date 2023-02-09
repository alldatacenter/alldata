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

public class SetBucketVersioningConfigurationRequest extends CosServiceRequest implements Serializable {
    /**
     * The bucket whose versioning configuration is being set.
     */
    private String bucketName;
    
    /**
     * The new versioning configuration for the specified bucket.
     */
    private BucketVersioningConfiguration versioningConfiguration;
    
    /**
     * Constructs a new {@link SetBucketVersioningConfigurationRequest} 
     * to set the bucket versioning configuration of
     * the specified bucket.
     * 
     * @param bucketName
     *            The name of the bucket whose versioning configuration is being
     *            set.
     * @param configuration
     *            The new versioning configuration for the specified bucket.
     *            
     * @see SetBucketVersioningConfigurationRequest#SetBucketVersioningConfigurationRequest(String, BucketVersioningConfiguration, MultiFactorAuthentication)          
     */
    public SetBucketVersioningConfigurationRequest(
            String bucketName, BucketVersioningConfiguration configuration) {
        this.bucketName = bucketName;
        this.versioningConfiguration = configuration;
    }
    
    /**
     * Gets the name of the bucket whose versioning configuration is being
     * set.
     * 
     * @return The name of the bucket whose versioning configuration is being
     *         set.
     *         
     * @see SetBucketVersioningConfigurationRequest#setBucketName(String)
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the name of the bucket whose versioning configuration is being set.
     * 
     * @param bucketName
     *            The name of the bucket whose versioning configuration is being
     *            set.
     *            
     * @see SetBucketVersioningConfigurationRequest#getBucketName()           
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Sets the name of the bucket whose versioning configuration is being set,
     * and returns this object so that additional method calls may be chained
     * together.
     * 
     * @param bucketName
     *            The name of the bucket whose versioning configuration is being
     *            set.
     * 
     * @return This {@link SetBucketVersioningConfigurationRequest} object so that
     *         additional method calls may be chained together.
     *         
     * @see SetBucketVersioningConfigurationRequest#setBucketName(String)       
     */
    public SetBucketVersioningConfigurationRequest withBucketName(String bucketName) {
        setBucketName(bucketName);
        return this;
    }

    /**
     * Gets the new versioning configuration for the specified bucket.
     * 
     * @return The new versioning configuration for the specified bucket.
     * 
     * @see SetBucketVersioningConfigurationRequest#setVersioningConfiguration(BucketVersioningConfiguration)
     * @see SetBucketVersioningConfigurationRequest#withVersioningConfiguration(BucketVersioningConfiguration)
     */
    public BucketVersioningConfiguration getVersioningConfiguration() {
        return versioningConfiguration;
    }

    /**
     * Sets the new versioning configuration for the specified bucket.
     * 
     * @param versioningConfiguration
     *            The new versioning configuration for the specified bucket.
     *            
     * @see SetBucketVersioningConfigurationRequest#getVersioningConfiguration()           
     * @see SetBucketVersioningConfigurationRequest#withVersioningConfiguration(BucketVersioningConfiguration)
     */
    public void setVersioningConfiguration(
            BucketVersioningConfiguration versioningConfiguration) {
        this.versioningConfiguration = versioningConfiguration;
    }

    /**
     * Sets the new versioning configuration for the specified bucket and
     * returns this object, enabling additional method calls to be chained
     * together.
     * 
     * @param versioningConfiguration
     *            The new versioning configuration for the specified bucket.
     * 
     * @return This {@link SetBucketVersioningConfigurationRequest} object, enabling that
     *         additional method calls may be chained together.
     *         
     * @see SetBucketVersioningConfigurationRequest#getVersioningConfiguration()  
     * @see SetBucketVersioningConfigurationRequest#getVersioningConfiguration()               
     */
    public SetBucketVersioningConfigurationRequest withVersioningConfiguration(
            BucketVersioningConfiguration versioningConfiguration) {
        setVersioningConfiguration(versioningConfiguration);
        return this;
    }
}
