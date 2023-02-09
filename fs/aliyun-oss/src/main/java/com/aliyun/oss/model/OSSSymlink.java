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

/**
 * The entity class representing a OSS symlink file.
 * 
 */
public class OSSSymlink extends GenericResult {

    public OSSSymlink(String symlink, String target) {
        this.symlink = symlink;
        this.target = target;
    }

    public String getSymlink() {
        return this.symlink;
    }

    public void setSymlink(String symlink) {
        this.symlink = symlink;
    }

    public String getTarget() {
        return this.target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Gets the metadata of the symlink file
     * 
     * @return The metadata of the symlink file.
     */
    public ObjectMetadata getMetadata() {
        return metadata;
    }

    /**
     * Sets the metadata of the symlink file.
     * 
     * @param metadata
     *            The metadata of the symlink file.
     */
    public void setMetadata(ObjectMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "OSSSymlink [symlink=" + getSymlink() + ", target=" + getTarget() + "]";
    }

    // symlink file key
    private String symlink;

    // The original file's key
    private String target;

    // The symlink file's metadata.
    private ObjectMetadata metadata;

}
