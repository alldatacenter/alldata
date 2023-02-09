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

public class SetBucketCORSRequest extends GenericRequest {

    private static int MAX_CORS_RULE_LIMIT = 10;
    private static char ASTERISK = '*';
    private static String[] ALL_ALLOWED_METHODS = { "GET", "PUT", "DELETE", "POST", "HEAD" };

    private List<CORSRule> corsRules = new ArrayList<CORSRule>();

    private Boolean responseVary;

    public SetBucketCORSRequest(String bucketName) {
        super(bucketName);
    }

    public void addCorsRule(CORSRule corsRule) {
        checkCorsValidity(corsRule);
        this.corsRules.add(corsRule);
    }

    private void checkCorsValidity(CORSRule corsRule) {
        if (corsRule == null) {
            throw new IllegalArgumentException("corsRule should not be null or empty.");
        }

        if (this.corsRules.size() >= MAX_CORS_RULE_LIMIT) {
            throw new IllegalArgumentException("One bucket not allowed exceed ten items of CORS Rules.");
        }

        // At least one item of allowed origins
        if (corsRule.getAllowedOrigins().isEmpty()) {
            throw new IllegalArgumentException("Required field 'AllowedOrigins' should not be empty.");
        }

        // At least one item of allowed methods
        if (corsRule.getAllowedMethods().isEmpty()) {
            throw new IllegalArgumentException("Required field 'AllowedMethod' should not be empty.");
        }

        // At most one asterisk wildcard in allowed origins
        for (String origin : corsRule.getAllowedOrigins()) {
            if (countOfAsterisk(origin) > 1) {
                throw new IllegalArgumentException("At most one '*' wildcard in allowd origin.");
            }
        }

        // Unsupported method
        for (String method : corsRule.getAllowedMethods()) {
            if (!isAllowedMethod(method)) {
                throw new IllegalArgumentException("Unsupported method " + method + ", (GET,PUT,DELETE,POST,HEAD)");
            }
        }

        // At most one asterisk wildcard in allowed headers
        for (String header : corsRule.getAllowedHeaders()) {
            if (countOfAsterisk(header) > 1) {
                throw new IllegalArgumentException("At most one '*' wildcard in allowd header.");
            }
        }

        // Not allow to use any asterisk wildcard in allowed origins
        for (String header : corsRule.getExposeHeaders()) {
            if (countOfAsterisk(header) > 0) {
                throw new IllegalArgumentException("Not allow to use any '*' wildcard in expose header.");
            }
        }
    }

    private static int countOfAsterisk(String str) {
        if (str == null || str.equals("")) {
            return 0;
        }

        int from = 0;
        int pos = -1;
        int count = 0;
        int len = str.length();
        do {
            pos = str.indexOf(ASTERISK, from);
            if (pos != -1) {
                count++;
                from = pos + 1;
            }
        } while (pos != -1 && from < len);

        return count;
    }

    private static boolean isAllowedMethod(String method) {
        if (method == null || method.equals("")) {
            return false;
        }

        for (String m : ALL_ALLOWED_METHODS) {
            if (m.equals(method)) {
                return true;
            }
        }
        return false;
    }

    public List<CORSRule> getCorsRules() {
        return corsRules;
    }

    public void setCorsRules(List<CORSRule> corsRules) {
        if (corsRules == null || corsRules.isEmpty()) {
            throw new IllegalArgumentException("corsRules should not be null or empty.");
        }

        if (corsRules.size() > MAX_CORS_RULE_LIMIT) {
            throw new IllegalArgumentException("One bucket not allowed exceed ten items of CORS Rules.");
        }

        this.corsRules.clear();
        this.corsRules.addAll(corsRules);
    }

    public void clearCorsRules() {
        this.corsRules.clear();
    }

    public void setResponseVary(Boolean responseVary) {
        this.responseVary = responseVary;
    }

    public Boolean getResponseVary() {
        return this.responseVary;
    }

    public static class CORSRule {
        private List<String> allowedOrigins = new ArrayList<String>();
        private List<String> allowedMethods = new ArrayList<String>();
        private List<String> allowedHeaders = new ArrayList<String>();
        private List<String> exposeHeaders = new ArrayList<String>();

        private Integer maxAgeSeconds;

        public void addAllowdOrigin(String allowedOrigin) {
            if (allowedOrigin != null && !allowedOrigin.trim().isEmpty()) {
                this.allowedOrigins.add(allowedOrigin);
            }
        }

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins.clear();
            if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
                this.allowedOrigins.addAll(allowedOrigins);
            }
        }

        public void clearAllowedOrigins() {
            this.allowedOrigins.clear();
        }

        public void addAllowedMethod(String allowedMethod) {
            if (allowedMethod != null && !allowedMethod.trim().isEmpty()) {
                this.allowedMethods.add(allowedMethod);
            }
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods.clear();
            if (allowedMethods != null && !allowedMethods.isEmpty()) {
                this.allowedMethods.addAll(allowedMethods);
            }
        }

        public void clearAllowedMethods() {
            this.allowedMethods.clear();
        }

        public void addAllowedHeader(String allowedHeader) {
            if (allowedHeader != null && !allowedHeader.trim().isEmpty()) {
                this.allowedHeaders.add(allowedHeader);
            }
        }

        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders.clear();
            if (allowedHeaders != null && !allowedHeaders.isEmpty()) {
                this.allowedHeaders.addAll(allowedHeaders);
            }
        }

        public void clearAllowedHeaders() {
            this.allowedHeaders.clear();
        }

        public void addExposeHeader(String exposeHeader) {
            if (exposeHeader != null && !exposeHeader.trim().isEmpty()) {
                this.exposeHeaders.add(exposeHeader);
            }
        }

        public List<String> getExposeHeaders() {
            return exposeHeaders;
        }

        public void setExposeHeaders(List<String> exposeHeaders) {
            this.exposeHeaders.clear();
            if (exposeHeaders != null && !exposeHeaders.isEmpty()) {
                this.exposeHeaders.addAll(exposeHeaders);
            }
        }

        public void clearExposeHeaders() {
            this.exposeHeaders.clear();
        }

        public Integer getMaxAgeSeconds() {
            return maxAgeSeconds;
        }

        public void setMaxAgeSeconds(Integer maxAgeSeconds) {
            this.maxAgeSeconds = maxAgeSeconds;
        }
    }
}
