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
 * Interface for service responses that receive the x-cos-restore header.
 *
 * @see Headers#RESTORE
 */
public interface ObjectRestoreResult {

    /**
     * Returns the expiration date when the Object is scheduled to move to CAS, or null if the object is not
     * configured to expire.
     */
    public Date getRestoreExpirationTime();

    /**
     * Sets the expiration date when the Object is scheduled to move to CAS.
     *
     * @param expiration
     *            The date the object will expire.
     */
    public void setRestoreExpirationTime(Date expiration);

    /**
     * Sets a boolean value which indicates there is an ongoing restore request.
     * @param ongoingRestore
     */
    public void setOngoingRestore(boolean ongoingRestore);

    /**
     * Returns then  boolean value which indicates there is an ongoing restore request.
     */
    public Boolean getOngoingRestore();
}