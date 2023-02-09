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
 * The storage class.
 */
public enum StorageClass {

    /**
     * Standard
     */
    Standard("Standard"),

    /**
     * Infrequent Access
     */
    IA("IA"),

    /**
     * Archive
     */
    Archive("Archive"),

    /**
     * ColdArchive
     */
    ColdArchive("ColdArchive"),

    /**
     * Unknown
     */
    Unknown("Unknown");

    private String storageClassString;

    private StorageClass(String storageClassString) {
        this.storageClassString = storageClassString;
    }

    @Override
    public String toString() {
        return this.storageClassString;
    }

    public static StorageClass parse(String storageClassString) {
        for (StorageClass st : StorageClass.values()) {
            if (st.toString().equals(storageClassString)) {
                return st;
            }
        }

        throw new IllegalArgumentException("Unable to parse " + storageClassString);
    }
}
