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
 * Predefined UDF access control list.
 *
 */
public enum CannedUdfAcl {

    /**
     * Only the owner has the access. It's the default Acl.
     */
    Private("private"),

    /**
     * All users have the access, not recommended.
     */
    Public("public");

    private String cannedAclString;

    private CannedUdfAcl(String cannedAclString) {
        this.cannedAclString = cannedAclString;
    }

    @Override
    public String toString() {
        return this.cannedAclString;
    }

    public static CannedUdfAcl parse(String acl) {
        for (CannedUdfAcl cacl : CannedUdfAcl.values()) {
            if (cacl.toString().equals(acl)) {
                return cacl;
            }
        }

        throw new IllegalArgumentException("Unable to parse the provided acl " + acl);
    }
}
