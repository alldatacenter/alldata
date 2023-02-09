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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.obs.services.internal.ObsConstraint;
import com.obs.services.internal.utils.ServiceUtils;

/**
 * Request parameters of policy-based temporary access authorization
 *
 */
public class PolicyTempSignatureRequest extends AbstractTemporarySignatureRequest {

    private Date expiryDate;

    private long expires = ObsConstraint.DEFAULT_EXPIRE_SECONEDS;

    private List<PolicyConditionItem> conditions;

    public PolicyTempSignatureRequest() {
    }

    /**
     * Constructor
     * 
     * @param method
     *            HTTP/HTTPS request method
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     */
    public PolicyTempSignatureRequest(HttpMethodEnum method, String bucketName, String objectKey) {
        super(method, bucketName, objectKey);
    }

    /**
     * Constructor
     * 
     * @param method
     *            HTTP/HTTPS request method
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param expiryDate
     *            Expiration data
     */
    public PolicyTempSignatureRequest(HttpMethodEnum method, String bucketName, String objectKey, Date expiryDate) {
        super(method, bucketName, objectKey);
        this.expiryDate = ServiceUtils.cloneDateIgnoreNull(expiryDate);
    }

    /**
     * Constructor
     * 
     * @param method
     *            HTTP/HTTPS request method
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param expires
     *            Validity period
     */
    public PolicyTempSignatureRequest(HttpMethodEnum method, String bucketName, String objectKey, long expires) {
        super(method, bucketName, objectKey);
        this.expires = expires;
    }

    /**
     * Generate a policy based on the validity period and policy condition.
     * 
     * @return
     */
    public String generatePolicy() {
        Date requestDate = new Date();
        SimpleDateFormat expirationDateFormat = ServiceUtils.getExpirationDateFormat();
        Date expiry = this.expiryDate;
        if (expiry == null) {
            expiry = new Date(requestDate.getTime()
                    + (this.expires <= 0 ? ObsConstraint.DEFAULT_EXPIRE_SECONEDS : this.expires) * 1000);
        }
        String expiration = expirationDateFormat.format(expiry);
        StringBuilder policy = new StringBuilder();
        policy.append("{\"expiration\":").append("\"").append(expiration).append("\",").append("\"conditions\":[");
        if (this.conditions != null && !this.conditions.isEmpty()) {
            policy.append(ServiceUtils.join(this.conditions, ","));
        }
        policy.append("]}");
        return policy.toString();
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
     * Obtain the validity period. The default value is 5 minutes (300 seconds).
     * 
     * @return Validity period
     */
    public long getExpires() {
        return expires;
    }

    /**
     * Set the validity period (seconds).
     * 
     * @param expires
     *            Validity period
     */
    public void setExpires(long expires) {
        this.expires = expires;
    }

    /**
     * Obtain the condition set of the policy.
     * 
     * @return Policy condition set
     */
    public List<PolicyConditionItem> getConditions() {
        return conditions;
    }

    /**
     * Set the condition set of the policy.
     * 
     * @param conditions
     *            Policy condition set
     */
    public void setConditions(List<PolicyConditionItem> conditions) {
        this.conditions = conditions;
    }
}
