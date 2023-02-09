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

public class DeleteDirectoryRequest extends GenericRequest {
    private boolean deleteRecursive;
    private String nextDeleteToken;

    public DeleteDirectoryRequest(String bucketName, String directoryName) {
        super(bucketName, directoryName);
    }

    public DeleteDirectoryRequest(String bucketName, String directoryName, boolean deleteRecursive, String nextDeleteToken) {
        super(bucketName, directoryName);
        setDeleteRecursive(deleteRecursive);
        setNextDeleteToken(nextDeleteToken);
    }

    public void setDirectoryName(String directoryName) {
        super.setKey(directoryName);
    }

    public String getDirectoryName() {
        return super.getKey();
    }

    public Boolean isDeleteRecursive() {
        return deleteRecursive;
    }

    public void setDeleteRecursive(boolean deleteRecursive) {
        this.deleteRecursive = deleteRecursive;
    }

    public DeleteDirectoryRequest withDeleteRecursive(boolean deleteRecursive) {
        setDeleteRecursive(deleteRecursive);
        return this;
    }

    public String getNextDeleteToken() {
        return nextDeleteToken;
    }

    public void setNextDeleteToken(String nextDeleteToken) {
        this.nextDeleteToken = nextDeleteToken;
    }

    public DeleteDirectoryRequest withNextDeleteToken(String nextDeleteToken) {
        setNextDeleteToken(nextDeleteToken);
        return this;
    }

}
