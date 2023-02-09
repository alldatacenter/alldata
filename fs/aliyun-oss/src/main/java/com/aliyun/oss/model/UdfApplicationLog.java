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
 * The result class represents the UDF Application Log's content. It must be
 * closed after the usage.
 */
public class UdfApplicationLog extends GenericResult implements Closeable {

    public UdfApplicationLog() {
        super();
    }

    public UdfApplicationLog(String udfName) {
        super();
        this.udfName = udfName;
    }

    public UdfApplicationLog(String udfName, InputStream logContent) {
        super();
        this.udfName = udfName;
        this.logContent = logContent;
    }

    public String getUdfName() {
        return udfName;
    }

    public void setUdfName(String udfName) {
        this.udfName = udfName;
    }

    /**
     * Gets the {@link InputStream} instance which has the log content.
     * 
     * @return A {@link InputStream} instance which has the log content.
     */
    public InputStream getLogContent() {
        return logContent;
    }

    /**
     * Sets the {@link InputStream} instance.
     * 
     * @param logContent
     *            The {@link InputStream} instance.
     */
    public void setLogContent(InputStream logContent) {
        this.logContent = logContent;
    }

    @Override
    public void close() throws IOException {
        if (logContent != null) {
            logContent.close();
        }
    }

    /**
     * Forcefully close the object. The remaining data in server side is
     * ignored.
     * 
     * @throws IOException
     *             if an I/O error occurs from the underlying input stream
     */
    public void forcedClose() throws IOException {
        this.response.abort();
    }

    private String udfName;
    private InputStream logContent;

}
