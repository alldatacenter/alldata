package com.qcloud.cos.model.ciModel.common;


import com.qcloud.cos.internal.CosServiceRequest;
import com.qcloud.cos.model.DeleteObjectRequest;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;

public class ImageProcessRequest extends CosServiceRequest {
    /**
     * The name of the Qcloud COS bucket containing the object to image process
     */
    private String bucketName;

    /**
     * The key of the object to image process.
     */
    private String key;
    /**
     *  pic operations
     */
    private PicOperations picOperations;

    /**
     * Constructs a new {@link ImageProcessRequest}, specifying the object's bucket name and key.
     *
     * @param bucketName The name of the Qcloud COS bucket containing the object to image process
     * @param key The key of the object to image process
     */
    public ImageProcessRequest(String bucketName, String key) {
        setBucketName(bucketName);
        setKey(key);
    }


    /**
     * Gets the name of the Qcloud COS bucket containing the object to image process.
     *
     * @return The name of the Qcloud COS bucket containing the object to image process.
     *
     * @see DeleteObjectRequest#setBucketName(String)
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the name of the Qcloud COS bucket containing the object to image process
     *
     * @param bucketName The name of the Qcloud COS bucket containing the object to image process
     * @see DeleteObjectRequest#getBucketName()
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Sets the name of the Qcloud COS bucket containing the object to image process and returns this
     * object, enabling additional method calls to be chained together.
     *
     * @param bucketName The name of the Qcloud COS bucket containing the object to image process
     *
     * @return The updated {@link DeleteObjectRequest} object, enabling additional method calls to
     *         be chained together.
     */
    public ImageProcessRequest withBucketName(String bucketName) {
        setBucketName(bucketName);
        return this;
    }

    /**
     * Gets the key of the object to image process.
     *
     * @return The key of the object to image process.
     *
     * @see ImageProcessRequest#setKey(String)
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the key of the object to image process.
     *
     * @param key The key of the object to image process.
     *
     * @see ImageProcessRequest#getKey()
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Sets the key of the object to image process and returns this object, enabling additional method
     * calls to be chained together.
     *
     * @param key The key of the object to image process.
     *
     * @return The updated {@link ImageProcessRequest} object, enabling additional method calls to
     *         chained together.
     */
    public ImageProcessRequest withKey(String key) {
        setKey(key);
        return this;
    }

    public PicOperations getPicOperations() {
        return picOperations;
    }

    public void setPicOperations(PicOperations picOperations) {
        this.picOperations = picOperations;
    }

    /**
     * Sets the pic operations of the object to image process and returns this object, enabling additional method
     * calls to be chained together.
     *
     * @param picOperations The pic operations of the object to image process.
     *
     * @return The updated {@link ImageProcessRequest} object, enabling additional method calls to
     *         chained together.
     */
    public ImageProcessRequest withKey(PicOperations picOperations) {
        setPicOperations(picOperations);
        return this;
    }
}
