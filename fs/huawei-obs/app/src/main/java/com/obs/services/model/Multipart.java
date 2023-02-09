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
 */

package com.obs.services.model;

import java.util.Date;

import com.obs.services.internal.utils.ServiceUtils;

/**
 * Part information in a multipart upload
 */
public class Multipart extends HeaderResponse {
    private Integer partNumber;

    private Date lastModified;

    private String etag;

    private Long size;

    public Multipart() {

    }

    /**
     * Constructor
     * 
     * @param partNumber
     *            Part number
     * @param lastModified
     *            Last modification time of the part
     * @param etag
     *            Part ETag
     * @param size
     *            Part size, in bytes
     */
    public Multipart(Integer partNumber, Date lastModified, String etag, Long size) {
        this.partNumber = partNumber;
        this.lastModified = ServiceUtils.cloneDateIgnoreNull(lastModified);
        this.etag = etag;
        this.size = size;
    }

    /**
     * Obtain the part number.
     * 
     * @return Part number
     */
    public Integer getPartNumber() {
        return partNumber;
    }

    /**
     * Obtain the last modification time of the part.
     * 
     * @return Last modification time of the part
     */
    public Date getLastModified() {
        return ServiceUtils.cloneDateIgnoreNull(this.lastModified);
    }

    /**
     * Obtain the ETag of the part.
     * 
     * @return Part ETag
     */
    public String getEtag() {
        return etag;
    }

    /**
     * Obtain the part size, in bytes.
     * 
     * @return Part size
     */
    public Long getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "Multipart [partNumber=" + partNumber + ", lastModified=" + lastModified + ", etag=" + etag + ", size="
                + size + "]";
    }
}
