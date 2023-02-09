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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.obs.services.internal.ObsConstraint;
import com.obs.services.internal.utils.ServiceUtils;

/**
 * Parameters in a request for browser-based authorized access
 *
 */
public class PostSignatureRequest {

    private Date requestDate;

    private Date expiryDate;

    private String bucketName;

    private String objectKey;

    private long expires = ObsConstraint.DEFAULT_EXPIRE_SECONEDS;

    private Map<String, Object> formParams;

    private List<String> conditions;

    public PostSignatureRequest() {

    }

    /**
     * Constructor
     * 
     * @param expires
     *            Expiration time (in seconds)
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     */
    public PostSignatureRequest(long expires, String bucketName, String objectKey) {
        this.expires = expires;
        this.bucketName = bucketName;
        this.objectKey = objectKey;
    }

    /**
     * Constructor
     * 
     * @param expiryDate
     *            Expiration date
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     */
    public PostSignatureRequest(Date expiryDate, String bucketName, String objectKey) {
        this.expiryDate = ServiceUtils.cloneDateIgnoreNull(expiryDate);
        this.bucketName = bucketName;
        this.objectKey = objectKey;
    }

    /**
     * 
     * @param expires
     *            Expiration time (in seconds)
     * @param requestDate
     *            Request time
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     */
    public PostSignatureRequest(long expires, Date requestDate, String bucketName, String objectKey) {
        this.expires = expires;
        this.requestDate = ServiceUtils.cloneDateIgnoreNull(requestDate);
        this.bucketName = bucketName;
        this.objectKey = objectKey;
    }

    /**
     * 
     * @param expiryDate
     *            Expiration date
     * @param requestDate
     *            Request time
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     */
    public PostSignatureRequest(Date expiryDate, Date requestDate, String bucketName, String objectKey) {
        this.expiryDate = ServiceUtils.cloneDateIgnoreNull(expiryDate);
        this.requestDate = ServiceUtils.cloneDateIgnoreNull(requestDate);
        this.bucketName = bucketName;
        this.objectKey = objectKey;
    }

    /**
     * Set the request time.
     * 
     * @return Request time
     */
    public Date getRequestDate() {
        return ServiceUtils.cloneDateIgnoreNull(this.requestDate);
    }

    /**
     * Set the request time.
     * 
     * @param requestDate
     *            Request time
     */
    public void setRequestDate(Date requestDate) {
        this.requestDate = ServiceUtils.cloneDateIgnoreNull(requestDate);
    }

    /**
     * Set the expiration date.
     * 
     * @return Expiration date
     */
    public Date getExpiryDate() {
        return ServiceUtils.cloneDateIgnoreNull(this.expiryDate);
    }

    /**
     * Obtain the expiration date.
     * 
     * @param expiryDate
     *            Expiration date
     */
    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = ServiceUtils.cloneDateIgnoreNull(expiryDate);
    }

    /**
     * Obtain the validity period. The default value is 5 minutes (value "300").
     * 
     * @return Validity period
     */
    public long getExpires() {
        return expires;
    }

    /**
     * Set the validity period (in seconds).
     * 
     * @param expires
     *            Validity period
     */
    public void setExpires(long expires) {
        this.expires = expires;
    }

    /**
     * Obtain the form parameters of the request.
     * 
     * @return Form parameters of the request
     */
    public Map<String, Object> getFormParams() {
        if (formParams == null) {
            formParams = new HashMap<String, Object>();
        }
        return formParams;
    }

    /**
     * Set the form parameters of the request.
     * 
     * @param formParams
     *            Form parameters of the request
     */
    public void setFormParams(Map<String, Object> formParams) {
        this.formParams = formParams;
    }

    /**
     * Obtain the bucket name.
     * 
     * @return Bucket name
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Set the bucket name.
     * 
     * @param bucketName
     *            Bucket name
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Obtain the object name.
     * 
     * @return Object name
     */
    public String getObjectKey() {
        return objectKey;
    }

    /**
     * Set the object name.
     * 
     * @param objectKey
     *            Object name
     */
    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    /**
     * Obtain the restrictions of the form. If the value is set, it will be used
     * to calculate "policy", while the form parameter configuration of the
     * request will be ignored.
     * 
     * @return Restrictions of the form
     */
    public List<String> getConditions() {
        if (this.conditions == null) {
            this.conditions = new ArrayList<String>();
        }
        return conditions;
    }

    /**
     * Set the restrictions of the form. If the value is set, it will be used to
     * calculate "policy", while the form parameter configuration of the request
     * will be ignored.
     * 
     * @param conditions
     *            Restrictions of the form
     */
    public void setConditions(List<String> conditions) {
        this.conditions = conditions;
    }

    @Override
    public String toString() {
        return "PostSignatureRequest [requestDate=" + requestDate + ", expiryDate=" + expiryDate + ", bucketName="
                + bucketName + ", objectKey=" + objectKey + ", expires=" + expires + ", formParams=" + formParams
                + ", conditions=" + conditions + "]";
    }

}
