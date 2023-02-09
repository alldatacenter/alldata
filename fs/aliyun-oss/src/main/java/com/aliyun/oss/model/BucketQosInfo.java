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

public class BucketQosInfo extends GenericResult {

    private Integer totalUplaodBw;
    private Integer intranetUploadBw;
    private Integer extranetUploadBw;
    private Integer totalDownloadBw;
    private Integer intranetDownloadBw;
    private Integer extranetDownloadBw;
    private Integer totalQps;
    private Integer intranetQps;
    private Integer extranetQps;

    /**
     * Sets the total upload bandwith its unit is Gbps.
     * 
     * @param totalUplaodBw
     *          The value of total upload bandwith, 
     *          and the special number -1 means no config here and 0 means forbidden.
     */
    public void setTotalUploadBw(Integer totalUplaodBw) {
        this.totalUplaodBw = totalUplaodBw;
    }

    /**
     * Gets the total upload bandwith, its unit is Gbps.
     * 
     * @return totalUplaodBw
     *          The value of total upload bandwith, 
     *          and the special number -1 means no config here and 0 means forbidden.
     */
    public Integer getTotalUploadBw() {
        return totalUplaodBw;
    }

    /**
     * Sets the intranet upload bandwith, its unit is Gbps.
     * 
     * @param intranetUploadBw
     *          The value of the intranet upload bandwith, 
     *          and the special number -1 means no config here and 0 means forbidden.
     */
    public void setIntranetUploadBw(Integer intranetUploadBw) {
        this.intranetUploadBw = intranetUploadBw;
    }

    /**
     * Gets the intranet upload bandwith, its unit is Gbps.
     * 
     * @return intranetUploadBw
     *          The value of the intranet upload bandwith, 
     *          and the special number -1 means no config here and 0 means forbidden.
     */
    public Integer getIntranetUploadBw() {
       return intranetUploadBw;
    }

    /**
     * Sets the extranet upload bandwith, its unit is Gbps.
     * 
     * @param extranetUploadBw
     *          The value of extranet upload bandwith, 
     *          and the special number -1 means no config here and 0 means forbidden.
     */
    public void setExtranetUploadBw(Integer extranetUploadBw) {
        this.extranetUploadBw = extranetUploadBw;
    }

    /**
     * Gets the extranet upload bandwith, its unit is Gbps.
     * 
     * @return extranetUploadBw
     *          The value of extranet upload bandwith, 
     *          and the special number -1 means no config here and 0 means forbidden.
     */
    public Integer getExtranetUploadBw() {
       return extranetUploadBw;
    }

    /**
     * Sets the total download bandwith, its unit is Gbps.
     * 
     * @param totalDownloadBw
     *          The value of the total download bandwith, 
     *          and the special number -1 means no config here and 0 means forbidden.
     */
    public void setTotalDownloadBw(Integer totalDownloadBw) {
        this.totalDownloadBw = totalDownloadBw;
    }

    /**
     * Gets the total download bandwith, its unit is Gbps.
     * 
     * @return totalDownloadBw
     *          The value of total download bandwith, 
     *          and the special number -1 means no config here and 0 means forbidden.
     */
    public Integer getTotalDownloadBw() {
        return totalDownloadBw;
    }

    /**
     * Sets the intranet download bandwith, its unit is Gbps.
     * 
     * @param intranetDownloadBw
     *          The value of intranet download bandwith, 
     *          and the special number -1 means no config here and 0 means forbidden.
     */
    public void setIntranetDownloadBw(Integer intranetDownloadBw) {
        this.intranetDownloadBw = intranetDownloadBw;
    }

    /**
     * Gets the intranet download bandwith, its unit is Gbps.
     * 
     * @return intranetDownloadBw
     *          The value of intranet download bandwith, 
     *          and the special number -1 means no config here and 0 means forbidden.
     */
    public Integer getIntranetDownloadBw() {
        return intranetDownloadBw;
    }

    /**
     * Sets the extranet download bandwith, its unit is Gbps.
     * 
     * @param extranetDownloadBw
     *          The value of extranet download bandwith, 
     *          and the special number -1 means no config here and 0 means forbidden
     */
    public void setExtranetDownloadBw(Integer extranetDownloadBw) {
        this.extranetDownloadBw = extranetDownloadBw;
    }

    /**
     * Gets the extranet download bandwith, its unit is Gbps.
     * 
     * @return extranetDownloadBw
     *          The value of extranet download bandwith, 
     *          and the special number -1 means no config here and 0 means forbidden.
     */
    public Integer getExtranetDownloadBw() {
        return extranetDownloadBw;
    }

    /**
     * Sets the total qps, its unit is query times per sencond.
     * 
     * @param totalQps
     *          The value of total qps, 
     *          and the special number -1 means no config here and 0 means forbidden.
     */
    public void setTotalQps(Integer totalQps) {
        this.totalQps = totalQps;
    }

    /**
     * Gets the total qps, its unit is query times per sencond.
     * 
     * @return totalQps
     *          The value of total qps,
     *          and the special number -1 means no config here and 0 means forbidden.
     */
    public Integer getTotalQps() {
        return totalQps;
    }

    /**
     * Sets the intranet qps, its unit is query times per sencond.
     * @param intranetQps
     *          The value of intranet qps, 
     *          and the special number -1 means no config here and 0 means forbidden.
     */
    public void setIntranetQps(Integer intranetQps) {
        this.intranetQps = intranetQps;
    }

    /**
     * Gets the intranet qps, its unit is query times per sencond.
     * 
     * @return intranetQps
     *          The value of intranet qps, 
     *          and the special number -1 means no config here and 0 means forbidden.
     */
    public Integer getIntranetQps() {
        return intranetQps;
    }

    /**
     * Sets the  extranet qps, its unit is query times per sencond.
     * 
     * @param extranetQps
     *          The value of extranet qps, 
     *          and the special number -1 means no config here and 0 means forbidden.
     */
    public void setExtranetQps(Integer extranetQps) {
        this.extranetQps = extranetQps;
    }

    /**
     * Gets the  extranet qps, its unit is query times per sencond.
     * 
     * @return extranetQps
     *         The value of extranet qps, 
     *         and the special number -1 means no config here and 0 means forbidden.
     */
    public Integer getExtranetQps() {
        return extranetQps;
    }
}