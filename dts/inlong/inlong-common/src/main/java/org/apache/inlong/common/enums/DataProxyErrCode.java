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

import org.apache.commons.lang3.math.NumberUtils;

/**
 * Enum of data proxy error code.
 */
public enum DataProxyErrCode {

    SUCCESS(0, "Ok"),

    SINK_SERVICE_UNREADY(1, "Service sink not ready"),
    SERVICE_CLOSED(2, "Service closed"),
    CONF_SERVICE_UNREADY(3, "Configure Service not ready"),
    ILLEGAL_VISIT_IP(10, "Illegal visit ip"),

    HTTP_DECODE_REQ_FAILURE(31, "Decode request failure"),
    HTTP_UNSUPPORTED_METHOD(32, "Un-supported method"),
    HTTP_REQ_URI_BLANK(33, "Request uri is blank"),
    HTTP_DECODE_REQ_URI_FAILURE(34, "Decode uri failure"),
    HTTP_UNSUPPORTED_SERVICE_URI(35, "Un-supported service uri"),
    HTTP_UNSUPPORTED_CONTENT_TYPE(36, "Un-supported content type"),

    FIELD_MAGIC_NOT_EQUAL(94, "Magic value not equal"),
    FIELD_LENGTH_VALUE_NOT_EQUAL(95, "Field length value not equal"),
    UNCOMPRESS_DATA_ERROR(96, "Uncompress data error"),

    MISS_REQUIRED_GROUPID_ARGUMENT(100, "Parameter groupId is required"),
    MISS_REQUIRED_STREAMID_ARGUMENT(101, "Parameter streamId is required"),
    MISS_REQUIRED_DT_ARGUMENT(102, "Parameter dt is required"),
    MISS_REQUIRED_BODY_ARGUMENT(103, "Parameter body is required"),
    BODY_EXCEED_MAX_LEN(104, "Body length exceed the maximum length"),
    BODY_LENGTH_ZERO(105, "Body length is 0"),
    BODY_LENGTH_LESS_ZERO(106, "Body length less than 0"),
    ATTR_LENGTH_LESS_ZERO(107, "Attribute length less than 0"),
    ERROR_PROCESS_LOGIC(108, "Wrong data processing logic"),
    SPLIT_ATTR_ERROR(109, "Split attributes failure"),

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
    GROUPID_OR_STREAMID_NOT_CONFIGURE(121, "GroupId or StreamId not found in configure"),
    GROUPID_OR_STREAMID_INCONSTANT(122, "GroupId or StreamId inconstant"),

    ATTR_ORDER_CONTROL_CONFLICT_ERROR(150, "Require order send but isAck is false"),
    ATTR_PROXY_CONTROL_CONFLICT_ERROR(151, "Require proxy send but isAck is false"),

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

    public static String getErrMsg(String errCode) {
        int codeVal = NumberUtils.toInt(errCode, Integer.MAX_VALUE);
        return valueOf(codeVal).errMsg;
    }

}
