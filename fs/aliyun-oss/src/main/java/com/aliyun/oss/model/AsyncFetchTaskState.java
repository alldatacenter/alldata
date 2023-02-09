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

public enum AsyncFetchTaskState {

    /**
     * Task is running.
     */
    Running("Running"),

    /**
     * Task is retrying.
     */
    Retry("Retry"),

    /**
     * Fetch object succeed but callback failed.
     */
    FetchSuccessCallbackFailed("FetchSuccessCallbackFailed"),

    /**
     * Task is failed.
     */
    Failed("Failed"),

    /***
     * Task is succeed.
     */
    Success("Success");

    private String stateString;

    private AsyncFetchTaskState(String stateString) {
        this.stateString = stateString;
    }

    @Override
    public String toString() {
        return this.stateString;
    }

    public static AsyncFetchTaskState parse(String stateString) {
        for (AsyncFetchTaskState o : AsyncFetchTaskState.values()) {
            if (o.toString().equals(stateString)) {
                return o;
            }
        }

        throw new IllegalArgumentException("Unable to parse AsyncFetchTaskState: " + stateString);
    }
}
