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
 * The enum of {@link OSSObject}'s access control permission.
 */
public enum ObjectPermission {

    /**
     * Private object. Only the owner has the full control of the object. Other
     * users don't have access permission unless they're explicitly granted with
     * some permission.
     */
    Private("private"),

    /**
     * Owner has the full control of the object. Other users only have read
     * access.
     */
    PublicRead("public-read"),

    /**
     * The object is public to everyone and all users have read write
     * permission.
     */
    PublicReadWrite("public-read-write"),

    /**
     * The object's ACL inherits the bucket's ACL. For example, if the bucket is
     * private, then the object is also private.
     */
    Default("default"),

    /**
     * The object's ACL is unknown, which indicates something not right about
     * the object. Please contact OSS support for more information when this
     * happens.
     */
    Unknown("");

    private String permissionString;

    private ObjectPermission(String permissionString) {
        this.permissionString = permissionString;
    }

    @Override
    public String toString() {
        return permissionString;
    }

    public static ObjectPermission parsePermission(String str) {
        final ObjectPermission[] knownPermissions = { Private, PublicRead, PublicReadWrite, Default };
        for (ObjectPermission permission : knownPermissions) {
            if (permission.permissionString.equals(str)) {
                return permission;
            }
        }

        return Unknown;
    }
}
