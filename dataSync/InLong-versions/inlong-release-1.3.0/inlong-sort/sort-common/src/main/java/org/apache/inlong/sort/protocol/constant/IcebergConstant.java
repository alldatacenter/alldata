/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.inlong.sort.protocol.constant;

/**
 * Iceberg option constant
 */
public class IcebergConstant {
    /**
     * Iceberg supported catalog type
     */
    public enum CatalogType {
        /**
         * Data stored in hive metastore.
         */
        HIVE,
        /**
         * Data stored in hadoop filesystem.
         */
        HADOOP,
        /**
         * Data stored in hybris metastore.
         */
        HYBRIS;

        /**
         * get catalogType from name
         */
        public static CatalogType forName(String name) {
            for (CatalogType value : values()) {
                if (value.name().equals(name)) {
                    return value;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupport catalogType:%s", name));
        }
    }
}
