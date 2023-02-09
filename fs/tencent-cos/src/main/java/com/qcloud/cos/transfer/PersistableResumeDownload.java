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


package com.qcloud.cos.transfer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An opaque token that holds some private state and can be used to resume a
 * download operation.
 */
public class PersistableResumeDownload extends PersistableTransfer {

    static final String TYPE = "resumedownload";

    @JsonProperty
    private final String pauseType = TYPE;

    /** The last modified time stamp of the object int Qcloud COS to be downloaded. */
    @JsonProperty
    private final long lastModified;

    /** The contentLength of the object in Qcloud COS to be downloaded. */
    @JsonProperty
    private final String contentLength;

    /** The etag of the object in Qcloud COS to be downloaded. */
    @JsonProperty
    private final String etag;

    /** The crc64ecma header of the object in Qcloud COS to be downloaded. */
    @JsonProperty
    private final String crc64ecma;

    @JsonProperty
    private final HashMap<String, Integer> downloadedBlocks;

    private File dumpFile;

    public PersistableResumeDownload() {
        this(0, null, null, null, null);
    }

    public PersistableResumeDownload(
            @JsonProperty(value = "lastModified") long lastModified,
            @JsonProperty(value = "contentLength") String contentLength,
            @JsonProperty(value = "etag") String etag,
            @JsonProperty(value = "crc64ecma") String crc64ecma,
            @JsonProperty(value = "downloadedBlocks") HashMap<String, Integer> downloadedBlocks) {
        this.lastModified = lastModified;
        this.contentLength = contentLength;
        this.etag = etag;
        this.crc64ecma = crc64ecma;
        this.downloadedBlocks = downloadedBlocks;
    }

    /**
     * Returns the lastModified of the object.
     */
    long getLastModified() {
        return lastModified;
    }

    /**
     * Returns the content length of the object.
     */
    String getContentLength() {
        return contentLength;
    }

    /**
     * Returns the etag of the object.
     */
    String getEtag() {
        return etag;
    }

    /**
     * Returns the crc64ecma of the object to download.
     */
    String getCrc64ecma() {
        return crc64ecma;
    }

    /**
     * Returns the optional response headers.
     */
    HashMap<String, Integer> getDownloadedBlocks() {
        return downloadedBlocks;
    }

    /**
     * Put a downloaded block.
     * @param block
     */
    public synchronized void putDownloadedBlocks(String block) {
        downloadedBlocks.put(block, 1);
    }

    /**
     * Return true if block is in downloadedBlocks, false else.
     * @param block
     * @return
     */
    public synchronized boolean hasDownloadedBlocks(String block) {
        return downloadedBlocks.getOrDefault(block, 0) == 1;
    }

    public synchronized void reset() {
        downloadedBlocks.clear();
    }

    public void setDumpFile(File dumpFile) {
        this.dumpFile = dumpFile;
    }

    public File getDumpFile() {
        return this.dumpFile;
    }

    public synchronized void dump() throws Exception {
        OutputStream out = new FileOutputStream(dumpFile);
        this.serialize(out);
        out.close();
    }
}
