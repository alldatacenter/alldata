/**
* Copyright 2019 Huawei Technologies Co.,Ltd.
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use
* this file except in compliance with the License.  You may obtain a copy of the
* License at
* 
* http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software distributed
* under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations under the License.
**/

package com.obs.services.model;

/**
 * Response headers that can be rewritten during object download
 */
public class ObjectRepleaceMetadata {

    private String contentType;

    private String contentLanguage;

    private String expires;

    private String cacheControl;

    private String contentDisposition;

    private String contentEncoding;

    /**
     * Obtain the rewritten "Content-Type" header in the response.
     * 
     * @return "Content-Type" header in the response
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Rewrite the "Content-Type" header in the response.
     * 
     * @param contentType
     *            "Content-Type" header in the response
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Obtain the rewritten "Content-Language" header in the response.
     * 
     * @return "Content-Language" header in the response
     */
    public String getContentLanguage() {
        return contentLanguage;
    }

    /**
     * Rewrite the "Content-Language" header in the response.
     * 
     * @param contentLanguage
     *            "Content-Language" header in the response
     */
    public void setContentLanguage(String contentLanguage) {
        this.contentLanguage = contentLanguage;
    }

    /**
     * Obtain the rewritten "Expires" header in the response.
     * 
     * @return "Expires" header in the response
     */
    public String getExpires() {
        return expires;
    }

    /**
     * Rewrite the "Expires" header in the response.
     * 
     * @param expires
     *            Rewritten "Expires" header in the response
     */
    public void setExpires(String expires) {
        this.expires = expires;
    }

    /**
     * Obtain the rewritten "Cache-Control" header in the response.
     * 
     * @return "Cache-Control" header in the response
     */
    public String getCacheControl() {
        return cacheControl;
    }

    /**
     * Rewrite the "Cache-Control" header in the response.
     * 
     * @param cacheControl
     *            "Cache-Control" header in the response
     */
    public void setCacheControl(String cacheControl) {
        this.cacheControl = cacheControl;
    }

    /**
     * Obtain the rewritten "Content-Disposition" header in the response.
     * 
     * @return "Content-Disposition" header in the response
     */
    public String getContentDisposition() {
        return contentDisposition;
    }

    /**
     * Rewrite the "Content-Disposition" header in the response.
     * 
     * @param contentDisposition
     *            "Content-Disposition" header in the response
     */
    public void setContentDisposition(String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }

    /**
     * Obtain the rewritten "Content-Encoding" header in the response.
     * 
     * @return "Content-Encoding" header in the response
     */
    public String getContentEncoding() {
        return contentEncoding;
    }

    /**
     * Rewrite the "Content-Encoding" header in the response.
     * 
     * @param contentEncoding
     *            "Content-Encoding" header in the response
     */
    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    @Override
    public String toString() {
        return "ObjectRepleaceMetadata [contentType=" + contentType + ", contentLanguage=" + contentLanguage
                + ", expires=" + expires + ", cacheControl=" + cacheControl + ", contentDisposition="
                + contentDisposition + ", contentEncoding=" + contentEncoding + "]";
    }

}
