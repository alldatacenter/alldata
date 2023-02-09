/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 
 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.internal;

import java.util.Date;


/**
 * Interface for service responses that receive the x-cos-expiration header.
 *
 * @see Headers#EXPIRATION
 */
public interface ObjectExpirationResult {

    /**
     * Returns the expiration date of the object, or null if the object is not
     * configured to expire.
     */
    public Date getExpirationTime();

    /**
     * Sets the expiration date of the object.
     *
     * @param expiration
     *            The date the object will expire.
     */
    public void setExpirationTime(Date expiration);

    /**
     * Returns the bucket lifecycle configuration rule ID for the expiration of
     * this object.
     *
     * @see Rule#getId()
     */
    public String getExpirationTimeRuleId();

    /**
     * Sets the bucket lifecycle configuration rule ID for the expiration of
     * this object.
     *
     * @param ruleId
     *            The rule ID of this object's expiration configuration
     */
    public void setExpirationTimeRuleId(String ruleId);

}
