/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Options for checking if the object key exists under the specified bucket.
 */
public class HeadObjectRequest extends WebServiceRequest {

    private String bucketName;
    private String key;

    private List<String> matchingETagConstraints = new ArrayList<String>();
    private List<String> nonmatchingETagConstraints = new ArrayList<String>();
    private Date unmodifiedSinceConstraint;
    private Date modifiedSinceConstraint;

    // The object version id
    private String versionId;

    // The one who pays for the request
    private Payer payer; 

    public HeadObjectRequest(String bucketName, String key, String versionId) {
        this.bucketName = bucketName;
        this.key = key;
        this.versionId = versionId;
    }

    public HeadObjectRequest(String bucketName, String key) {
        this.bucketName = bucketName;
        this.key = key;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<String> getMatchingETagConstraints() {
        return matchingETagConstraints;
    }

    public void setMatchingETagConstraints(List<String> matchingETagConstraints) {
        this.matchingETagConstraints.clear();
        if (matchingETagConstraints != null && !matchingETagConstraints.isEmpty()) {
            this.matchingETagConstraints.addAll(matchingETagConstraints);
        }
    }

    public List<String> getNonmatchingETagConstraints() {
        return nonmatchingETagConstraints;
    }

    public void setNonmatchingETagConstraints(List<String> nonmatchingETagConstraints) {
        this.nonmatchingETagConstraints.clear();
        if (nonmatchingETagConstraints != null && !nonmatchingETagConstraints.isEmpty()) {
            this.nonmatchingETagConstraints.addAll(nonmatchingETagConstraints);
        }
    }

    public Date getUnmodifiedSinceConstraint() {
        return unmodifiedSinceConstraint;
    }

    public void setUnmodifiedSinceConstraint(Date unmodifiedSinceConstraint) {
        this.unmodifiedSinceConstraint = unmodifiedSinceConstraint;
    }

    public Date getModifiedSinceConstraint() {
        return modifiedSinceConstraint;
    }

    public void setModifiedSinceConstraint(Date modifiedSinceConstraint) {
        this.modifiedSinceConstraint = modifiedSinceConstraint;
    }

    /**
     * Gets version id.
     * 
     * @return version id.
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Sets version id.
     * 
     * @param versionId version id
     */
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    } 

    /**
     * * <p>
     * Sets the one who pays for the request
     * The Third party should set request payer when requesting resources.
     * </p>
     * @param payer
     *            The one who pays for the request
     * */
    public void setRequestPayer(Payer payer) {
        this.payer = payer;
    }
    
    /**
     * * <p>
     * Gets the one who pays for the request
     * </p>
     * @return The one who pays for the request
     * */
    public Payer getRequestPayer() {
        return payer;
    }
}
