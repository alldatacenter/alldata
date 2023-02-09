/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Logging settings of a bucket
 */
public class BucketLoggingConfiguration extends HeaderResponse {
    private String targetBucketName;

    private String logfilePrefix;

    private String agency;

    private final List<GrantAndPermission> targetGrantsList = new ArrayList<GrantAndPermission>();

    public BucketLoggingConfiguration() {
    }

    /**
     * Constructor
     * 
     * @param targetBucketName
     *            Name of the bucket in which logs are saved
     * @param logfilePrefix
     *            Name prefix of the logged objects
     */
    public BucketLoggingConfiguration(String targetBucketName, String logfilePrefix) {
        this.targetBucketName = targetBucketName;
        this.logfilePrefix = logfilePrefix;
    }

    /**
     * Obtain the bucket in which logs are saved.
     * 
     * @return Bucket in which logs are saved
     */
    public String getTargetBucketName() {
        return targetBucketName;
    }

    /**
     * Set the bucket to write logs.
     * 
     * @param targetBucketName
     *            Name of the bucket in which logs are saved
     */
    public void setTargetBucketName(String targetBucketName) {
        this.targetBucketName = targetBucketName;
    }

    /**
     * Obtain the name prefix of the logged objects.
     * 
     * @return Name prefix of the logged objects
     */
    public String getLogfilePrefix() {
        return logfilePrefix;
    }

    /**
     * Set the name prefix of the logged objects.
     * 
     * @param logfilePrefix
     *            Name prefix of the logged objects
     */
    public void setLogfilePrefix(String logfilePrefix) {
        this.logfilePrefix = logfilePrefix;
    }

    /**
     * Obtain the log delivery group.
     * 
     * @return Log delivery group {@link GrantAndPermission}
     */
    public GrantAndPermission[] getTargetGrants() {
        return targetGrantsList.toArray(new GrantAndPermission[targetGrantsList.size()]);
    }

    /**
     * Set the log delivery group.
     * 
     * @param targetGrants
     *            Log delivery group {@link GrantAndPermission}
     */
    public void setTargetGrants(GrantAndPermission[] targetGrants) {
        targetGrantsList.clear();
        targetGrantsList.addAll(Arrays.asList(targetGrants));
    }

    /**
     * Add permissions for logged objects
     * 
     * @param targetGrant
     *            Permissions of the logged object
     */
    public void addTargetGrant(GrantAndPermission targetGrant) {
        targetGrantsList.add(targetGrant);
    }

    /**
     * Check whether bucket logging is enabled.
     * 
     * @return Identifier specifying whether bucket logging is enabled
     */
    public boolean isLoggingEnabled() {
        return targetBucketName != null || logfilePrefix != null || this.targetGrantsList.size() > 0;
    }

    /**
     * Set the agent name.
     * 
     * @return Agent name
     */
    public String getAgency() {
        return agency;
    }

    /**
     * Obtain the agent name.
     * 
     * @param agency
     *            Agent name
     */
    public void setAgency(String agency) {
        this.agency = agency;
    }

    @Override
    public String toString() {
        return "BucketLoggingConfiguration [targetBucketName=" + targetBucketName + ", logfilePrefix=" + logfilePrefix
                + ", agency=" + agency + ", targetGrantsList=" + targetGrantsList + "]";
    }

}
