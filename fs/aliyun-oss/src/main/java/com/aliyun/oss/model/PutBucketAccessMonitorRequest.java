package com.aliyun.oss.model;

/**
 * Bucket AccessMonitor Configuration
 */
public class PutBucketAccessMonitorRequest extends GenericRequest {
    private AccessMonitor accessMonitor = new AccessMonitor(AccessMonitor.AccessMonitorStatus.Disabled.toString());

    public PutBucketAccessMonitorRequest(String bucketName, String status) {
        super();
        setBucketName(bucketName);
        setStatus(status);
    }

    public String getStatus() {
        return accessMonitor.getStatus();
    }

    public void setStatus(String status) {
        accessMonitor.setStatus(status);
    }
}
