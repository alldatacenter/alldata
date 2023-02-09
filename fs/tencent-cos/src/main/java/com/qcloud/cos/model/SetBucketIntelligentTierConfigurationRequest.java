package com.qcloud.cos.model;

import com.qcloud.cos.internal.CosServiceRequest;

public class SetBucketIntelligentTierConfigurationRequest extends CosServiceRequest {

    /** The name of the bucket whose intelligent tier configuration is being set. */
    private String bucketName;

    /** The intelligent tier configuration for the specified bucket. */
    private BucketIntelligentTierConfiguration configuration;

    public SetBucketIntelligentTierConfigurationRequest() { }

    /**
     * Creates a new request object, ready to be executed to set the specified
     * bucket's intelligent tier configuration.
     *
     * @param bucketName
     *            The name of the bucket whose intelligent tier configuration is being set.
     * @param configuration
     *            The new intelligent tier configuration for the specified bucket.
     */
    public SetBucketIntelligentTierConfigurationRequest(String bucketName, BucketIntelligentTierConfiguration configuration) {
        this.bucketName = bucketName;
        this.configuration = configuration;
    }

    /**
     * Sets the name of the bucket whose intelligent tier configuration is to be updated.
     *
     * @param bucketName
     *            The name of the bucket whose intelligent tier configuration is being set.
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Returns the name of the bucket whose intelligent tier configuration is being set.
     *
     * @return The name of the bucket whose intelligent tier configuration is being set.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the name of the bucket whose intelligent tier configuration is being set and
     * returns this updated request object so that additional method calls can
     * be chained together.
     *
     * @param bucketName
     *            The name of the bucket whose intelligent tier configuration is being
     *            set.
     * @return This updated request object so that additional method calls can
     *         be chained together.
     */
    public SetBucketIntelligentTierConfigurationRequest withBucketName(String bucketName) {
        setBucketName(bucketName);
        return this;
    }

    /**
     * Sets the intelligent tier configuration to send as part of this request.
     *
     * @param configuration
     *            The intelligent tier configuration to set for the specified bucket.
     */
    public void setIntelligentTierConfiguration(BucketIntelligentTierConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Returns the intelligent tier configuration to send as part of this request.
     *
     * @return The intelligent tier configuration to set for the specified bucket.
     */
    public BucketIntelligentTierConfiguration getBucketIntelligentTierConfiguration() {
        return configuration;
    }

    /**
     * Sets the intelligent tier configuration to send as part of this request, and
     * returns this updated request object so that additional method calls can
     * be chained together.
     *
     * @param configuration
     *            The intelligent tier configuration to set for the specified bucket.
     *
     * @return This updated request object so that additional method calls can
     *         be chained together.
     */
    public SetBucketIntelligentTierConfigurationRequest withConfiguration(BucketIntelligentTierConfiguration configuration) {
        setIntelligentTierConfiguration(configuration);
        return this;
    }
}
