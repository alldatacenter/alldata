/**
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.hive.model;

/**
 * Hive Data Types for model and bridge.
 */
public enum HiveDataTypes {

    // Enums
    HIVE_OBJECT_TYPE,
    HIVE_PRINCIPAL_TYPE,
    HIVE_RESOURCE_TYPE,

    // Structs
    HIVE_SERDE,
    HIVE_ORDER,
    HIVE_RESOURCEURI,

    // Classes
    HIVE_DB,
    HIVE_STORAGEDESC,
    HIVE_TABLE,
    HIVE_COLUMN,
    HIVE_PARTITION,
    HIVE_INDEX,
    HIVE_ROLE,
    HIVE_TYPE,
    HIVE_PROCESS,
    HIVE_COLUMN_LINEAGE,
    HIVE_PROCESS_EXECUTION,
    // HIVE_VIEW,
    ;

    public String getName() {
        return name().toLowerCase();
    }
}
