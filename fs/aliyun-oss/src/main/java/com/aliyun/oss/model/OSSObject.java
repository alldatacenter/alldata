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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * The entity class for representing an object in OSS.
 * <p>
 * In OSS, every file is an OSSObject and every single file should be less than
 * 5G for using Simple upload, Form upload and Append Upload. Only multipart
 * upload could upload a single file more than 5G. Any object has key, data and
 * user metadata. The key is the object's name and the data is object's file
 * content. The user metadata is a dictionary of key-value entries to store some
 * custom data about the object.
 * </p>
 * Object naming rules
 * <ul>
 * <li>use UTF-8 encoding</li>
 * <li>Length is between 1 to 1023</li>
 * <li>Could not have slash or backslash</li>
 * </ul>
 *
 */
public class OSSObject extends GenericResult implements Closeable {

    // Object key (name)
    private String key;

    // Object's bucket name
    private String bucketName;

    // Object's metadata.
    private ObjectMetadata metadata = new ObjectMetadata();

    // Object's content
    private InputStream objectContent;

    /**
     * Gets the object's metadata
     * 
     * @return Object's metadata in（{@link ObjectMetadata}
     */
    public ObjectMetadata getObjectMetadata() {
        return metadata;
    }

    /**
     * Sets the object's metadata.
     * 
     * @param metadata
     *            Object's metadata.
     */
    public void setObjectMetadata(ObjectMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Get's the object's content in {@link InputStream}.
     * 
     * @return The object's content in {@link InputStream}.
     */
    public InputStream getObjectContent() {
        return objectContent;
    }

    /**
     * Sets the object's content in {@link InputStream}.
     * 
     * @param objectContent
     *            The object's content in {@link InputStream}.
     */
    public void setObjectContent(InputStream objectContent) {
        this.objectContent = objectContent;
    }

    /**
     * Gets the object's bucket name.
     * 
     * @return The object's bucket name.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the object's bucket name.
     * 
     * @param bucketName
     *            The object's bucket name.
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Gets the object's key.
     * 
     * @return Object Key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the object's key.
     * 
     * @param key
     *            Object Key.
     */
    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public void close() throws IOException {
        if (objectContent != null) {
            objectContent.close();
        }
    }

    /**
     * Forcefully close the response. The remaining data in the server will not
     * be downloaded.
     * 
     * @throws IOException
     *             An IO errors are encountered in the client while making the
     *             request or handling the response.
     */
    public void forcedClose() throws IOException {
        this.response.abort();
    }

    @Override
    public String toString() {
        return "OSSObject [key=" + getKey() + ",bucket=" + (bucketName == null ? "<Unknown>" : bucketName) + "]";
    }
}
