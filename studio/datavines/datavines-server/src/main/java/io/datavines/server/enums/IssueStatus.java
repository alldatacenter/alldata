/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.server.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

import java.util.HashMap;
import java.util.Map;

public enum IssueStatus {

    /**
     * 0-data quality task
     * 1-catalog task
     */
    GOOD(0, "good"),
    BAD(1, "bad")
    ;
    IssueStatus(int code, String description){
        this.code = code;
        this.description = description;
    }

    @EnumValue
    private int code;

    private String description;

    private static final Map<Integer, IssueStatus> ISSUE_STATUS_MAP = new HashMap<>();

    static {
        for (IssueStatus issueStatus : IssueStatus.values()) {
            ISSUE_STATUS_MAP.put(issueStatus.code, issueStatus);
        }
    }

    public static IssueStatus of(Integer status) {
        if (ISSUE_STATUS_MAP.containsKey(status)) {
            return ISSUE_STATUS_MAP.get(status);
        }
        throw new IllegalArgumentException("invalid issue status : " + status);
    }

    public static boolean isValid(Integer status) {
        if (ISSUE_STATUS_MAP.containsKey(status)) {
            return true;
        }
        return false;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
