package com.aliyun.oss.model;

public class AddBucketCnameResult extends GenericResult{
    /**
     * Certificate ID for the CNAME.
     */
    private String certId;

    /**
     * Gets the certificate ID for the CNAME.
     * @return null if no certificate ID exists.
     */
    public String getCertId() { return certId; }

    /**
     * Sets the certificate ID for the CNAME.
     * @param certId the certificate id that the response contains.
     */
    public void setCertId(String certId) { this.certId = certId; }
}
