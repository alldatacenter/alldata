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
 * The class wraps the HTTP Get response headers to override. For Response
 * headers, please refer to
 * https://help.aliyun.com/document_detail/31980.html?spm=5176.doc31980.6.870.
 * zfxavo Basically it tells the OSS to return the specified headers in the
 * response.
 */
public class ResponseHeaderOverrides {

    private String contentType;
    private String contentLangauge;
    private String expires;
    private String cacheControl;
    private String contentDisposition;
    private String contentEncoding;

    public static final String RESPONSE_HEADER_CONTENT_TYPE = "response-content-type";
    public static final String RESPONSE_HEADER_CONTENT_LANGUAGE = "response-content-language";
    public static final String RESPONSE_HEADER_EXPIRES = "response-expires";
    public static final String RESPONSE_HEADER_CACHE_CONTROL = "response-cache-control";
    public static final String RESPONSE_HEADER_CONTENT_DISPOSITION = "response-content-disposition";
    public static final String RESPONSE_HEADER_CONTENT_ENCODING = "response-content-encoding";

    /**
     * Gets the content type. If the type is not specified, return null.
     * 
     * @return The override content type. If it's not specified, return null.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the content type.
     * 
     * @param contentType
     *            The override content type.
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Gets the content language header. If it's not specified, returns null.
     * 
     * @return The override content language.
     */
    public String getContentLangauge() {
        return contentLangauge;
    }

    /**
     * Sets the content language.
     * 
     * @param contentLangauge
     *            The override content language header.
     */
    public void setContentLangauge(String contentLangauge) {
        this.contentLangauge = contentLangauge;
    }

    /**
     * Gets the expires header. If it's not specfied, return null.
     * 
     * @return The override expires header.
     */
    public String getExpires() {
        return expires;
    }

    /**
     * Sets the expires header.
     * 
     * @param expires
     *            The override expires header.
     */
    public void setExpires(String expires) {
        this.expires = expires;
    }

    /**
     * Gets the Cache-Control header. If it's not specified, return null.
     * 
     * @return The override Cache-Control header.
     */
    public String getCacheControl() {
        return cacheControl;
    }

    /**
     * Sets the Cache-Control header.
     * 
     * @param cacheControl
     *            The override Cache-Control header.
     */
    public void setCacheControl(String cacheControl) {
        this.cacheControl = cacheControl;
    }

    /**
     * Gets the Content-Disposition header. If it's not specified, return null.
     * 
     * @return The override Content-Disposition header.
     */
    public String getContentDisposition() {
        return contentDisposition;
    }

    /**
     * Sets the Content-Disposition header.
     * 
     * @param contentDisposition
     *            The override Content-Disposition header.
     */
    public void setContentDisposition(String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }

    /**
     * Gets the Content-Encoding header.
     * 
     * @return The override Content-Encoding header.
     */
    public String getContentEncoding() {
        return contentEncoding;
    }

    /**
     * Sets the Content-Encoding header.
     * 
     * @param contentEncoding
     *            The override Content-Encoding header.
     */
    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }
}
