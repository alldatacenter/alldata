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


package com.qcloud.cos.model;

import java.io.Serializable;


public class BucketPolicy implements Serializable {
    private static final long serialVersionUID = 1L;
    /** The raw, policy JSON text, as returned by COS */
    private String policyText;

    /**
     * Gets the raw policy JSON text as returned by COS. If no policy has been applied to the
     * specified bucket, the policy text will be null.
     * 
     * @return The raw policy JSON text as returned by COS. If no policy has been applied to the
     *         specified bucket, this method returns null policy text.
     * 
     * @see BucketPolicy#setPolicyText(String)
     */
    public String getPolicyText() {
        return policyText;
    }

    /**
     * Sets the raw policy JSON text. A bucket will have no policy text unless the policy text is
     * explicitly provided through this method.
     *
     * @param policyText The raw policy JSON text.
     * 
     * @see BucketPolicy#getPolicyText()
     */
    public void setPolicyText(String policyText) {
        this.policyText = policyText;
    }
}
