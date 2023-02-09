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

/**
 * Specifies constants defining a canned access control list.
 * <p>
 * Canned access control lists are commonly used access control lists (ACL) that can be
 * used as a shortcut when applying an access control list to Qcloud COS buckets
 * and objects. Only a few commonly used configurations are available, but they
 * offer an alternative to manually creating a custom ACL. If more specific
 * access control is desired, users can create a custom {@link AccessControlList}.
 * </p>
 * 
 * @see AccessControlList
 */
public enum CannedAccessControlList {
    /**
     * Specifies the owner is granted {@link Permission#FullControl}. No one else has access rights.
     * <p>
     * This is the default access control policy for any new buckets or objects.
     * </p>
     */
    Private("private"),

    /**
     * Specifies the owner is granted {@link Permission#FullControl} and the
     * {@link GroupGrantee#AllUsers} group grantee is granted
     * {@link Permission#Read} access.
     * <p>
     * If this policy is used on an object, it can be read from a browser without
     * authentication.
     * </p>
     */
    PublicRead("public-read"),
    /**
     * Specifies the owner is granted {@link Permission#FullControl} and the
     * {@link GroupGrantee#AllUsers} group grantee is granted
     * {@link Permission#Read} and {@link Permission#Write} access.
     * <p>
     * This access policy is not recommended for general use.
     * </p>
     */
    PublicReadWrite("public-read-write"),
    
    /**
     * inherit from bucket permission
     */
    Default("default");    
    
    /** The Qcloud COS x-cos-acl header value representing the canned acl */
    private final String cannedAclHeader;

    private CannedAccessControlList(String cannedAclHeader) {
        this.cannedAclHeader = cannedAclHeader;
    }
    
    /**
     * Returns the Qcloud COS x-cos-acl header value for this canned acl.
     */
    public String toString() {
        return cannedAclHeader;
    }
    
}
