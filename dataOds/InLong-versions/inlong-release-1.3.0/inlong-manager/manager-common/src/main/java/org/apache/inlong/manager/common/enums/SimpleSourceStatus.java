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

package org.apache.inlong.manager.common.enums;

/**
 * The simple stream source status, more readable for users
 */
public enum SimpleSourceStatus {

    INIT, NORMAL, FREEZING, FROZEN, FAILED, DELETING, DELETE;

    /**
     * Parse status of stream source.
     */
    public static SimpleSourceStatus parseByStatus(int status) {
        SourceStatus sourceStatus = SourceStatus.forCode(status);
        switch (sourceStatus) {
            case SOURCE_NEW:
            case TO_BE_ISSUED_ADD:
            case BEEN_ISSUED_ADD:
            case TO_BE_ISSUED_ACTIVE:
            case BEEN_ISSUED_ACTIVE:
                return INIT;
            case SOURCE_NORMAL:
                return NORMAL;
            case TO_BE_ISSUED_FROZEN:
            case BEEN_ISSUED_FROZEN:
                return FREEZING;
            case SOURCE_FROZEN:
                return FROZEN;
            case SOURCE_FAILED:
                return FAILED;
            case TO_BE_ISSUED_DELETE:
            case BEEN_ISSUED_DELETE:
                return DELETING;
            case SOURCE_DISABLE:
                return DELETE;
            default:
                throw new IllegalStateException(String.format("Unsupported source status [%s]", sourceStatus));
        }
    }

}
