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

import java.util.Date;

/**
 * 包含通过Multipart上传模式上传的Part的摘要信息。 The summary information of the part in a
 * multipart upload.
 *
 */
public class PartSummary {

    private int partNumber;

    private Date lastModified;

    private String eTag;

    private long size;

    /**
     * Constructor
     */
    public PartSummary() {
    }

    /**
     * Gets part number.
     * 
     * @return The Part number.
     */
    public int getPartNumber() {
        return partNumber;
    }

    /**
     * Sets the part number.
     * 
     * @param partNumber
     *            The part number.
     */
    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    /**
     * Gets the last modified time of the part.
     * 
     * @return Part's last modified time.
     */
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * Sets the last modified time of the part.
     * 
     * @param lastModified
     *            Part's last modified time.
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Gets the Part's ETag.
     * 
     * @return Part ETag value.
     */
    public String getETag() {
        return eTag;
    }

    /**
     * Sets the Part ETag value.
     * 
     * @param eTag
     *            Part ETag value.
     */
    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    /**
     * Gets the size of the part.
     * 
     * @return Part size.
     */
    public long getSize() {
        return size;
    }

    /**
     * Sets the part data size.
     * 
     * @param size
     *            Part data size.
     */
    public void setSize(long size) {
        this.size = size;
    }

}
