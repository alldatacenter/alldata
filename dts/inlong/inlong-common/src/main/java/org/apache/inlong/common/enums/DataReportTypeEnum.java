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

package org.apache.inlong.common.enums;

/**
 * Enum of data report type
 */
public enum DataReportTypeEnum {

    NORMAL_SEND_TO_DATAPROXY(0),
    PROXY_SEND_TO_DATAPROXY(1),
    DIRECT_SEND_TO_MQ(2);

    private final int type;

    DataReportTypeEnum(int type) {
        this.type = type;
    }

    public static DataReportTypeEnum getReportType(int type) {
        switch (type) {
            case 0:
                return NORMAL_SEND_TO_DATAPROXY;
            case 1:
                return PROXY_SEND_TO_DATAPROXY;
            case 2:
                return DIRECT_SEND_TO_MQ;
            default:
                throw new RuntimeException("Unsupported data report type " + type);
        }
    }

    public int getType() {
        return type;
    }
}
