package com.aliyun.oss.model;

/**
 * Bucket TransferAcceleration Configuration
 */
public class SetBucketTransferAccelerationRequest extends GenericRequest {
    private TransferAcceleration transferAcceleration = new TransferAcceleration(false);

    public SetBucketTransferAccelerationRequest(String bucketName, boolean enabled) {
        super();
        setBucketName(bucketName);
        setEnabled(enabled);
    }

    public boolean isEnabled() {
        return transferAcceleration.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        transferAcceleration.setEnabled(enabled);
    }
}
