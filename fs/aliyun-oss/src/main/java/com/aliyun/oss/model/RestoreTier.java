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
 * The restore priority.
 */
public enum RestoreTier {
    /**
     * The restore job will be done in one hour.
     */
    RESTORE_TIER_EXPEDITED("Expedited"),

    /**
     * The restore job will be done in five hours.
     */
    RESTORE_TIER_STANDARD("Standard"),

    /**
     * The restore job will be done in ten hours.
     */
    RESTORE_TIER_BULK("Bulk");


    private String tierString;

    RestoreTier(String tierString) {
        this.tierString = tierString;
    }

    @Override
    public String toString() {
        return this.tierString;
    }

    public static RestoreTier parse(String tier) {
        for (RestoreTier o : RestoreTier.values()) {
            if (o.toString().toLowerCase().equals(tier.toLowerCase())) {
                return o;
            }
        }

        throw new IllegalArgumentException("Unable to parse the RestoreTier text :" + tier);
    }
}
