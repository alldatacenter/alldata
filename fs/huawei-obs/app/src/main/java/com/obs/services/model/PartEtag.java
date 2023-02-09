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

import java.io.Serializable;

/**
 * Part information, including the ETag and part number
 */
public class PartEtag implements Serializable {
    private static final long serialVersionUID = -2946156755118245847L;

    private String etag;

    private Integer partNumber;

    public PartEtag() {

    }

    /**
     * Constructor
     * 
     * @param etag
     *            Part ETag
     * @param partNumber
     *            Part number
     */
    public PartEtag(String etag, Integer partNumber) {
        this.etag = etag;
        this.partNumber = partNumber;
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
     * Set the ETag of the part.
     * 
     * @param etag
     *            Part ETag
     */
    public void setEtag(String etag) {
        this.etag = etag;
    }

    /**
     * Obtain the ETag of the part.
     * 
     * @return Part ETag
     */
    @Deprecated
    public String geteTag() {
        return this.getEtag();
    }

    /**
     * Set the ETag of the part.
     * 
     * @param etag
     *            Part ETag
     */
    @Deprecated
    public void seteTag(String etag) {
        this.setEtag(etag);
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
     * Set the part number.
     * 
     * @param partNumber
     *            Part number
     */
    public void setPartNumber(Integer partNumber) {
        this.partNumber = partNumber;
    }

    @Override
    public String toString() {
        return "PartEtag [etag=" + etag + ", partNumber=" + partNumber + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((etag == null) ? 0 : etag.hashCode());
        result = prime * result + partNumber;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else {
            if (obj instanceof PartEtag) {
                PartEtag partEtag = (PartEtag) obj;
                if (partEtag.etag.equals(this.etag) && partEtag.partNumber.equals(this.partNumber)) {
                    return true;
                }
            }
        }
        return false;
    }
}
