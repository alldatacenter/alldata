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

import lombok.Getter;
import org.apache.inlong.manager.common.exceptions.BusinessException;

/**
 * Enum of MQ type.
 */
public enum MQType {

    TUBE("TUBE"),
    PULSAR("PULSAR"),
    TDMQ_PULSAR("TDMQ_PULSAR"),
    NONE("NONE");

    public static final String MQ_TUBE = "TUBE";
    public static final String MQ_PULSAR = "PULSAR";
    public static final String MQ_TDMQ_PULSAR = "TDMQ_PULSAR";
    public static final String MQ_NONE = "NONE";

    @Getter
    private final String type;

    MQType(String type) {
        this.type = type;
    }

    public static MQType forType(String type) {
        for (MQType mqType : values()) {
            if (mqType.getType().equals(type)) {
                return mqType;
            }
        }
        throw new BusinessException(String.format(ErrorCodeEnum.MQ_TYPE_NOT_SUPPORTED.getMessage(), type));
    }

}
