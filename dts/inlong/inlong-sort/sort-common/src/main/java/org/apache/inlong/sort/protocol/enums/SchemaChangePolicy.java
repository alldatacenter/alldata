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

package org.apache.inlong.sort.protocol.enums;

/**
 * The class defines the support policy of schema change.
 */
public enum SchemaChangePolicy {

    /**
     * Under this policy it supports the schema change.
     * That is to say, it will execute the schema change on the data source.
     */
    ENABLE(1),
    /**
     * Under this policy it will ignore the schema change and not print any log of schema change.
     */
    IGNORE(2),
    /**
     * Under this policy it will ignore the schema change and print the changed information in the log to alert users.
     */
    LOG(3),
    /**
     * Under this policy, it will throw an error and print the changed information in the log  to alert users.
     */
    ERROR(4);
    /**
     * The code represents this policy {@link SchemaChangePolicy}
     */
    private final int code;

    SchemaChangePolicy(int code) {
        this.code = code;
    }

    public static SchemaChangePolicy getInstance(int code) {
        for (SchemaChangePolicy policy : values()) {
            if (policy.code == code) {
                return policy;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported policy of schema-change: %s for InLong", code));
    }

    /**
     * Get the code of {@link SchemaChangePolicy}
     *
     * @return The code of {@link SchemaChangePolicy}
     */
    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", this.name(), getCode());
    }
}
