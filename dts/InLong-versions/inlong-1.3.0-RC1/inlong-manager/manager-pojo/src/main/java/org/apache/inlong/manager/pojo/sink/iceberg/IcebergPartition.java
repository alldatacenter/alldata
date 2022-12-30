/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.pojo.sink.iceberg;

import org.apache.inlong.manager.common.util.Preconditions;

/**
 * Iceberg partition type
 */
public enum IcebergPartition {
    IDENTITY,
    BUCKET,
    TRUNCATE,
    YEAR,
    MONTH,
    DAY,
    HOUR,
    NONE,
    ;

    /**
     * Get partition type from name
     */
    public static IcebergPartition forName(String name) {
        Preconditions.checkNotNull(name, "IcebergPartition should not be null");
        for (IcebergPartition value : values()) {
            if (value.toString().equalsIgnoreCase(name)) {
                return value;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported IcebergPartition : %s", name));
    }

    @Override
    public String toString() {
        return name();
    }
}
