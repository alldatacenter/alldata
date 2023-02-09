package com.obs.test;

import com.obs.services.ObsClient;

public class UserInfo {
    private ObsClient obsClient;
    private String domainId;
    private String ownerId;
    
    public UserInfo(ObsClient obsClient, String domainId, String ownerId) {
        this.obsClient = obsClient;
        this.domainId = domainId;
        this.ownerId = ownerId;
    }

    public ObsClient getObsClient() {
        return obsClient;
    }

    public String getDomainId() {
        return domainId;
    }
    
    public String getOwnerId() {
        return this.ownerId;
    }
}
