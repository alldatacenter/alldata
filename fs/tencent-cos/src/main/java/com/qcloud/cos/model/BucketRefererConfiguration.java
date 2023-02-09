/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.

 * According to cos feature, we modify some class，comment, field name, etc.
 */

package com.qcloud.cos.model;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class BucketRefererConfiguration implements Serializable {
    public static String ENABLED = "Enabled";
    public static String DISABLED = "Disabled";
    public static String ALLOW = "Allow";
    public static String DENY = "Deny";
    public static String WHITELIST = "White-List";
    public static String BLACKLIST = "Black-List";

    private String status;
    private String refererType;
    private List<String> domainList = new LinkedList<>();
    private String emptyReferConfiguration;

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }

    public void setRefererType(String refererType) {
        this.refererType = refererType;
    }

    public String getRefererType() {
        return this.refererType;
    }

    public void addDomain(String domain) {
        this.domainList.add(domain);
    }

    public void setDomainList(List<String> domainList) {
        this.domainList = domainList;
    }

    public List<String> getDomainList() {
        return this.domainList;
    }

    public void setEmptyReferConfiguration(String emptyReferConfiguration) {
        this.emptyReferConfiguration = emptyReferConfiguration;
    }

    public String getEmptyReferConfiguration() {
        return this.emptyReferConfiguration;
    }
}