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
 * Enum of data proxy error code.
 */
public enum DataProxyErrCode {

    SUCCESS(0, "Ok"),

    SINK_SERVICE_UNREADY(1, "Service not ready"),

    MISS_REQUIRED_GROUPID_ARGUMENT(100, "Parameter groupId is required"),
    MISS_REQUIRED_STREAMID_ARGUMENT(101, "Parameter streamId is required"),
    MISS_REQUIRED_DT_ARGUMENT(102, "Parameter dt is required"),
    MISS_REQUIRED_BODY_ARGUMENT(103, "Parameter body is required"),
    BODY_EXCEED_MAX_LEN(104, "Body length exceed the maximum length"),

    UNSUPPORTED_MSG_TYPE(110, "Unsupported msgType"),
    EMPTY_MSG(111, "Empty message"),
    UNSUPPORTED_EXTEND_FIELD_VALUE(112, "Unsupported extend field value"),
    UNCONFIGURED_GROUPID_OR_STREAMID(113, "Unconfigured groupId or streamId"),
    PUT_EVENT_TO_CHANNEL_FAILURE(114, "Put event to Channels failure"),

    TOPIC_IS_BLANK(115, "Topic is null"),
    NO_AVAILABLE_PRODUCER(116, "No available producer info"),
    PRODUCER_IS_NULL(117, "Producer is null"),
    SEND_REQUEST_TO_MQ_FAILURE(118, "Send request to MQ failure"),
    MQ_RETURN_ERROR(119, "MQ client return error"),

    DUPLICATED_MESSAGE(120, "Duplicated message"),

    UNKNOWN_ERROR(Integer.MAX_VALUE, "Unknown error");

    private final int errCode;
    private final String errMsg;

    DataProxyErrCode(int errorCode, String errorMsg) {
        this.errCode = errorCode;
        this.errMsg = errorMsg;
    }

    public static DataProxyErrCode valueOf(int value) {
        for (DataProxyErrCode errCode : DataProxyErrCode.values()) {
            if (errCode.getErrCode() == value) {
                return errCode;
            }
        }

        return UNKNOWN_ERROR;
    }

    public int getErrCode() {
        return errCode;
    }

    public String getErrCodeStr() {
        return String.valueOf(errCode);
    }

    public String getErrMsg() {
        return errMsg;
    }
}
