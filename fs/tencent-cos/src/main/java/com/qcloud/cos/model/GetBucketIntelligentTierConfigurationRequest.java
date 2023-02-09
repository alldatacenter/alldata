package com.qcloud.cos.model;


import com.qcloud.cos.internal.CosServiceRequest;

import java.io.Serializable;

public class GetBucketIntelligentTierConfigurationRequest extends CosServiceRequest implements Serializable {

    /** The name of the bucket whose intelligent configuration is being retrieved. */
    private String bucketName;


    /**
     * Creates a new request object, ready to be executed to retrieve the bucket
     * intelligent tier configuration for the specified bucket.
     *
     * @param bucketName
     *            The name of the bucket whose bucket intelligent tier configuration is being retrieved.
     */
    public GetBucketIntelligentTierConfigurationRequest(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Sets the name of the bucket whose bucket intelligent tier configuration is being retrieved.
     *
     * @param bucketName
     *            The name of the bucket whose bucket intelligent tier is being retrieved.
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Returns the name of the bucket whose bucket intelligent tier is being retrieved.
     *
     * @return The name of the bucket whose bucket intelligent tier is being retrieved.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the name of the bucket whose bucket intelligent tier is being
     * retrieved, and returns this updated request object so that additional
     * method calls can be chained together.
     *
     * @param bucketName
     *            The name of the bucket whose bucket intelligent tier is being
     *            retrieved.
     *
     * @return This updated request object, so that additional method calls can
     *         be chained together.
     */
    public GetBucketIntelligentTierConfigurationRequest withBucketName(String bucketName) {
        setBucketName(bucketName);
        return this;
    }
}