package com.qcloud.cos.model;

public class CosServiceResult {
    private String requestId;

    /**
     * get requestid for this upload
     *
     * @return requestid
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * set requestId for this upload
     *
     * @param requestId the requestId for the upload
     */

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
