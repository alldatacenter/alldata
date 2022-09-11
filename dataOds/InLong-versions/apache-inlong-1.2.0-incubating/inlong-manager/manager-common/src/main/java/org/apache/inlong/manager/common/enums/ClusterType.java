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

import org.apache.inlong.manager.common.exceptions.BusinessException;

/**
 * Enum of cluster type.
 */
public enum ClusterType {

    TUBE,
    PULSAR,
    DATA_PROXY,

    ;

    public static final String CLS_TUBE = "TUBE";
    public static final String CLS_PULSAR = "PULSAR";
    public static final String CLS_DATA_PROXY = "DATA_PROXY";

    /**
     * Get the SinkType enum via the given sinkType string
     */
    public static ClusterType forType(String type) {
        for (ClusterType clsType : values()) {
            if (clsType.name().equals(type)) {
                return clsType;
            }
        }
        throw new BusinessException(String.format(ErrorCodeEnum.MQ_TYPE_NOT_SUPPORTED.getMessage(), type));
    }

}
