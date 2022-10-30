/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.common.statusdef;

/*
 * The enable status enumeration class
 */
public enum EnableStatus {
    STATUS_UNDEFINE(-2, "Undefined."),
    STATUS_DISABLE(0, "Disable."),
    STATUS_ENABLE(2, "Enable.");

    private int code;
    private String description;

    EnableStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public boolean isEnable() {
        return this == EnableStatus.STATUS_ENABLE;
    }

    public String getDescription() {
        return description;
    }

    public static EnableStatus valueOf(int code) {
        for (EnableStatus status : EnableStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException(String.format("unknown Enable status code %s", code));
    }

}
