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

/**
 * The predefined Access Control List (ACL)
 * <p>
 * It defines some common permissions.
 * </p>
 *
 */
public enum CannedAccessControlList {

    /**
     * This is only for object, means the permission inherits the bucket's
     * permission.
     */
    Default("default"),

    /**
     * The owner has the {@link Permission#FullControl}, other
     * {@link GroupGrantee#AllUsers} does not have access.
     */
    Private("private"),

    /**
     * The owner has the {@link Permission#FullControl}, other
     * {@link GroupGrantee#AllUsers} have read-only access.
     */
    PublicRead("public-read"),

    /**
     * Both the owner and {@link GroupGrantee#AllUsers} have
     * {@link Permission#FullControl}. It's not safe and thus not recommended.
     */
    PublicReadWrite("public-read-write");

    private String cannedAclString;

    private CannedAccessControlList(String cannedAclString) {
        this.cannedAclString = cannedAclString;
    }

    @Override
    public String toString() {
        return this.cannedAclString;
    }

    public static CannedAccessControlList parse(String acl) {
        for (CannedAccessControlList cacl : CannedAccessControlList.values()) {
            if (cacl.toString().equals(acl)) {
                return cacl;
            }
        }

        throw new IllegalArgumentException("Unable to parse the provided acl " + acl);
    }
}
