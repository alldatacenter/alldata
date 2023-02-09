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
import java.util.List;

public class SetBucketWebsiteRequest extends GenericRequest {

    private String indexDocument;
    private String errorDocument;
    private boolean supportSubDir;
    private SubDirType subDirType;

    private List<RoutingRule> routingRules = new ArrayList<RoutingRule>();

    public SetBucketWebsiteRequest(String bucketName) {
        super(bucketName);
    }

    public String getIndexDocument() {
        return indexDocument;
    }

    public void setIndexDocument(String indexDocument) {
        this.indexDocument = indexDocument;
    }

    public String getErrorDocument() {
        return errorDocument;
    }

    public void setErrorDocument(String errorDocument) {
        this.errorDocument = errorDocument;
    }

    public List<RoutingRule> getRoutingRules() {
        return routingRules;
    }

    public void setRoutingRules(List<RoutingRule> routingRules) {
        if (routingRules == null || routingRules.isEmpty()) {
            throw new IllegalArgumentException("routingRules should not be null or empty.");
        }

        for (RoutingRule rule : routingRules) {
            rule.ensureRoutingRuleValid();
        }

        this.routingRules.clear();
        this.routingRules.addAll(routingRules);
    }

    public void AddRoutingRule(RoutingRule routingRule) {
        routingRule.ensureRoutingRuleValid();
        this.routingRules.add(routingRule);
    }

    public boolean isSupportSubDir() {
        return supportSubDir;
    }

    public void setSupportSubDir(boolean supportSubDir) {
        this.supportSubDir = supportSubDir;
    }

    public SubDirType getSubDirType() {
        return subDirType;
    }

    public void setSubDirType(SubDirType subDirType) {
        this.subDirType = subDirType;
    }
}
