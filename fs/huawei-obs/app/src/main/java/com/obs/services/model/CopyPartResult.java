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

import java.util.Date;

import com.obs.services.internal.utils.ServiceUtils;

/**
 * Response to a request for copying a part
 */
public class CopyPartResult extends HeaderResponse {
    private int partNumber;

    private String etag;

    private Date lastModified;

    public CopyPartResult(int partNumber, String etag, Date lastModified) {
        this.partNumber = partNumber;
        this.etag = etag;
        this.lastModified = ServiceUtils.cloneDateIgnoreNull(lastModified);
    }

    /**
     * Obtain the part number of the to-be-copied part.
     * 
     * @return Part number
     */
    public int getPartNumber() {
        return partNumber;
    }

    /**
     * Obtain the ETag of the to-be-copied part.
     * 
     * @return ETag of the to-be-copied part
     */
    public String getEtag() {
        return etag;
    }

    /**
     * Obtain the last modification time of the to-be-copied part.
     * 
     * @return Last modification time of the to-be-copied part
     */
    public Date getLastModified() {
        return ServiceUtils.cloneDateIgnoreNull(this.lastModified);
    }

    @Override
    public String toString() {
        return "CopyPartResult [partNumber=" + partNumber + ", etag=" + etag + ", lastModified=" + lastModified + "]";
    }

}
