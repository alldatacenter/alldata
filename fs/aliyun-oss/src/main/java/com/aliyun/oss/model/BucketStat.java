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
 * Bucket Stat It contains the current bucket's occupant size and file count.
 */
public class BucketStat extends GenericResult {

    public BucketStat() {
    }

    public BucketStat(Long storageSize, Long objectCount, Long multipartUploadCount) {
        this.storageSize = storageSize;
        this.objectCount = objectCount;
        this.multipartUploadCount = multipartUploadCount;
    }

    /**
     * Gets the used storage size in bytes.
     * 
     * @return Bucket used storage size.
     */
    public Long getStorageSize() {
        return storageSize;
    }

    /**
     * Sets the storage size and returns the current BucketStat instance
     * (this).
     *
     * @param storageSize
     *            The storage size.
     * @return The {@link BucketStat} instance.
     */
    public BucketStat withStorageSize(Long storageSize) {
        this.storageSize = storageSize;
        return this;
    }

    /**
     * Gets the object count under the bucket.
     * 
     * @return Object count
     */
    public Long getObjectCount() {
        return objectCount;
    }

    /**
     * Sets the object count and returns the current BucketStat instance
     * (this).
     *
     * @param objectCount
     *            The object count.
     * @return The {@link BucketStat} instance.
     */
    public BucketStat withObjectCount(Long objectCount) {
        this.objectCount = objectCount;
        return this;
    }

    /**
     * Gets the multipart upload count.
     * 
     * @return The multipart upload count.
     */
    public Long getMultipartUploadCount() {
        return multipartUploadCount;
    }

    /**
     * Sets the multipart upload count and returns the current BucketStat instance
     * (this).
     *
     * @param multipartUploadCount
     *            The multipart upload count.
     * @return The {@link BucketStat} instance.
     */
    public BucketStat withMultipartUploadCount(Long multipartUploadCount) {
        this.multipartUploadCount = multipartUploadCount;
        return this;
    }

    /**
     * Gets the live channel count.
     *
     * @return The live channel count.
     */
    public Long getLiveChannelCount() {
        return liveChannelCount;
    }

    /**
     * Sets the live channel count and returns the current BucketStat instance
     * (this).
     *
     * @param liveChannelCount
     *            The live channel count.
     * @return The {@link BucketStat} instance.
     */
    public BucketStat withLiveChannelCount(Long liveChannelCount) {
        this.liveChannelCount = liveChannelCount;
        return this;
    }

    /**
     * Gets the last modified time.
     *
     * @return The last modified time.
     */
    public Long getLastModifiedTime() {
        return lastModifiedTime;
    }

    /**
     * Sets the last modified time and returns the current BucketStat instance
     * (this).
     *
     * @param lastModifiedTime
     *            The last modified time.
     * @return The {@link BucketStat} instance.
     */
    public BucketStat withLastModifiedTime(Long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
        return this;
    }

    /**
     * Gets the standard storage.
     *
     * @return The standard storage.
     */
    public Long getStandardStorage() {
        return standardStorage;
    }

    /**
     * Sets the standard storage and returns the current BucketStat instance
     * (this).
     *
     * @param standardStorage
     *            The standard storage.
     * @return The {@link BucketStat} instance.
     */
    public BucketStat withStandardStorage(Long standardStorage) {
        this.standardStorage = standardStorage;
        return this;
    }

    /**
     * Gets the standard object count.
     *
     * @return The standard object count.
     */
    public Long getStandardObjectCount() {
        return standardObjectCount;
    }

    /**
     * Sets the standard object count and returns the current BucketStat instance
     * (this).
     *
     * @param standardObjectCount
     *            The standard object count.
     * @return The {@link BucketStat} instance.
     */
    public BucketStat withStandardObjectCount(Long standardObjectCount) {
        this.standardObjectCount = standardObjectCount;
        return this;
    }

    /**
     * Gets the infrequent access storage.
     *
     * @return The infrequent access storage.
     */
    public Long getInfrequentAccessStorage() {
        return infrequentAccessStorage;
    }

    /**
     * Sets the infrequent access storage and returns the current BucketStat instance
     * (this).
     *
     * @param infrequentAccessStorage
     *            The infrequent access storage.
     *
     * @return  The {@link BucketStat} instance.
     */
    public BucketStat withInfrequentAccessStorage(Long infrequentAccessStorage) {
        this.infrequentAccessStorage = infrequentAccessStorage;
        return this;
    }

    /**
     * Gets the infrequent access real storage.
     *
     * @return The infrequent access real storage.
     */
    public Long getInfrequentAccessRealStorage() {
        return infrequentAccessRealStorage;
    }

    /**
     * Sets the infrequent access real storage and returns the current BucketStat instance
     * (this).
     *
     * @param infrequentAccessRealStorage
     *            The infrequent access real storage.
     *
     * @return  The {@link BucketStat} instance.
     */
    public BucketStat withInfrequentAccessRealStorage(Long infrequentAccessRealStorage) {
        this.infrequentAccessRealStorage = infrequentAccessRealStorage;
        return this;
    }

    /**
     * Gets the infrequent access object count.
     *
     * @return The infrequent access object count.
     */
    public Long getInfrequentAccessObjectCount() {
        return infrequentAccessObjectCount;
    }

    /**
     * Sets the infrequent access object count and returns the current BucketStat instance
     * (this).
     *
     * @param infrequentAccessObjectCount
     *            The infrequent access object count.
     *
     * @return  The {@link BucketStat} instance.
     */
    public BucketStat withInfrequentAccessObjectCount(Long infrequentAccessObjectCount) {
        this.infrequentAccessObjectCount = infrequentAccessObjectCount;
        return this;
    }

    /**
     * Gets the archive storage.
     *
     * @return The archive storage.
     */
    public Long getArchiveStorage() {
        return archiveStorage;
    }

    /**
     * Sets the archive storage and returns the current BucketStat instance
     * (this).
     *
     * @param archiveStorage
     *            The archive storage.
     *
     * @return  The {@link BucketStat} instance.
     */
    public BucketStat withArchiveStorage(Long archiveStorage) {
        this.archiveStorage = archiveStorage;
        return this;
    }

    /**
     * Gets the archive real storage.
     *
     * @return The archive real storage.
     */
    public Long getArchiveRealStorage() {
        return archiveRealStorage;
    }

    /**
     * Sets the archive real storage and returns the current BucketStat instance
     * (this).
     *
     * @param archiveRealStorage
     *            The archive real storage.
     *
     * @return  The {@link BucketStat} instance.
     */
    public BucketStat withArchiveRealStorage(Long archiveRealStorage) {
        this.archiveRealStorage = archiveRealStorage;
        return this;
    }

    /**
     * Gets the archive object count.
     *
     * @return The archive object count.
     */
    public Long getArchiveObjectCount() {
        return archiveObjectCount;
    }

    /**
     * Sets the archive object count and returns the current BucketStat instance
     * (this).
     *
     * @param archiveObjectCount
     *            The archive object count.
     *
     * @return  The {@link BucketStat} instance.
     */
    public BucketStat withArchiveObjectCount(Long archiveObjectCount) {
        this.archiveObjectCount = archiveObjectCount;
        return this;
    }

    /**
     * Gets the cold archive storage.
     *
     * @return The cold archive storage.
     */
    public Long getColdArchiveStorage() {
        return coldArchiveStorage;
    }

    /**
     * Sets the cold archive storage and returns the current BucketStat instance
     * (this).
     *
     * @param coldArchiveStorage
     *            The cold archive storage.
     *
     * @return  The {@link BucketStat} instance.
     */
    public BucketStat withColdArchiveStorage(Long coldArchiveStorage) {
        this.coldArchiveStorage = coldArchiveStorage;
        return this;
    }

    /**
     * Gets the cold archive real storage.
     *
     * @return The cold archive real storage.
     */
    public Long getColdArchiveRealStorage() {
        return coldArchiveRealStorage;
    }

    /**
     * Sets the cold archive real storage and returns the current BucketStat instance
     * (this).
     *
     * @param coldArchiveRealStorage
     *            The cold archive real storage.
     *
     * @return  The {@link BucketStat} instance.
     */
    public BucketStat withColdArchiveRealStorage(Long coldArchiveRealStorage) {
        this.coldArchiveRealStorage = coldArchiveRealStorage;
        return this;
    }

    /**
     * Gets the cold archive object count.
     *
     * @return The cold archive object count.
     */
    public Long getColdArchiveObjectCount() {
        return coldArchiveObjectCount;
    }

    /**
     * Sets the cold archive object count and returns the current BucketStat instance
     * (this).
     *
     * @param coldArchiveObjectCount
     *            The cold archive object count.
     *
     * @return  The {@link BucketStat} instance.
     */
    public BucketStat withColdArchiveObjectCount(Long coldArchiveObjectCount) {
        this.coldArchiveObjectCount = coldArchiveObjectCount;
        return this;
    }

    private Long storageSize; // bytes
    private Long objectCount;
    private Long multipartUploadCount;
    private Long liveChannelCount;
    private Long lastModifiedTime;
    private Long standardStorage;
    private Long standardObjectCount;
    private Long infrequentAccessStorage;
    private Long infrequentAccessRealStorage;
    private Long infrequentAccessObjectCount;
    private Long archiveStorage;
    private Long archiveRealStorage;
    private Long archiveObjectCount;
    private Long coldArchiveStorage;
    private Long coldArchiveRealStorage;
    private Long coldArchiveObjectCount;
}
