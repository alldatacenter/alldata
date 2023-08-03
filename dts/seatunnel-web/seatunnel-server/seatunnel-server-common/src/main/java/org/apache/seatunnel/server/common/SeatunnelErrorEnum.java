/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.server.common;

public enum SeatunnelErrorEnum {
    SCRIPT_ALREADY_EXIST(
            10001, "script already exist", "You already have a script with the same name : '%s'"),
    NO_SUCH_SCRIPT(10002, "no such script", "No such script. Maybe deleted by others."),
    USER_ALREADY_EXISTS(10003, "user already exist", "The same username [%s] is exist."),
    NO_SUCH_USER(10004, "no such user", "No such user. Maybe deleted by others."),
    SCHEDULER_CONFIG_NOT_EXIST(
            10005,
            "scheduler config not exist",
            "This script's scheduler config not exist, please check your config."),
    JSON_TRANSFORM_FAILED(
            10006, "json transform failed", "Json transform failed, it may be a bug."),

    USERNAME_PASSWORD_NO_MATCHED(
            10007,
            "username and password no matched",
            "The user name and password do not match, please check your input"),

    TOKEN_ILLEGAL(10008, "token illegal", "The token is expired or invalid, please login again."),
    NO_SUCH_JOB(10009, "no such job", "No such job. Maybe deleted by others."),

    /** request dolphinscheduler failed */
    UNEXPECTED_RETURN_CODE(
            20000, "Unexpected return code", "Unexpected return code : [%s], error msg is [%s]"),
    QUERY_PROJECT_CODE_FAILED(
            20001, "query project code failed", "Request ds for querying project code failed"),
    NO_MATCHED_PROJECT(
            20002,
            "no matched project",
            "No matched project [%s], please check your configuration"),
    NO_MATCHED_SCRIPT_SAVE_DIR(
            20003,
            "no matched script save dir",
            "No matched script save dir [%s], please check your configuration"),
    GET_INSTANCE_FAILED(20004, "get instance failed", "Get instance failed"),

    ERROR_CONFIG(99994, "Component config error, please check", "%s"),
    NO_SUCH_ELEMENT(99995, "no such element", "No such element."),
    UNSUPPORTED_OPERATION(
            99996, "unsupported operation", "This operation [%s] is not supported now."),
    HTTP_REQUEST_FAILED(99997, "http request failed", "Http request failed, url is %s"),
    ILLEGAL_STATE(99998, "illegal state", "%s"),
    UNKNOWN(99999, "unknown exception", "Unknown exception"),

    UNSUPPORTED_CONNECTOR_TYPE(
            30000,
            "unsupported connector type",
            "unsupported connector type [%s], only support source/sink/transform connector now."),
    CONNECTOR_NOT_FOUND(30001, "connector not found", "[%s] connector [%s] can not be found."),

    JOB_METRICS_QUERY_KEY_ERROR(
            40000,
            "job metrics query key error",
            "metrics query key [%s] must contain ["
                    + Constants.METRICS_QUERY_KEY_SPLIT
                    + "] and the array length after split must equal 2"),
    LOAD_ENGINE_METRICS_JSON_ERROR(
            40001,
            "load engine metrics error",
            "load engine [%s] metrics json error, error msg is [%s]"),
    LOAD_ENGINE_JOB_STATUS_JSON_ERROR(
            40002,
            "load job state from engine error",
            "load job statue from engine [%s] error, error msg is [%s]"),
    UNSUPPORTED_ENGINE(40003, "unsupported engine", "unsupported engine [%s] version [%s]"),

    JOB_RUN_GENERATE_UUID_ERROR(50001, "generate uuid error", "generate uuid error"),
    /* datasource and virtual table */
    DATASOURCE_NOT_FOUND(60001, "datasource not found", "datasource [%s] not found"),
    VIRTUAL_TABLE_NOT_FOUND(60002, "virtual table not found", "virtual table [%s] not found"),
    VIRTUAL_TABLE_ALREADY_EXISTS(
            60003, "virtual table name already exists", "virtual table [%s] already exists"),
    DATASOURCE_NAME_ALREADY_EXISTS(
            60004, "datasource name already exists", "datasource [%s] already exists"),
    DATASOURCE_NOT_EXISTS(60005, "datasource not exists", "datasource [%s] not exists"),
    VIRTUAL_TABLE_NOT_EXISTS(60006, "virtual table not exists", "virtual table [%s] not exists"),
    DATASOURCE_PRAM_NOT_ALLOWED_NULL(
            60007, "datasource pram not allowed null", "datasource pram [%s] not allowed null"),
    VIRTUAL_TABLE_PRAM_NOT_ALLOWED_NULL(
            60008,
            "virtual table pram not allowed null",
            "virtual table pram [%s] not allowed null"),
    DATASOURCE_TYPE_NOT_SUPPORT(
            60009, "datasource type not support", "datasource type [%s] not support"),
    DATASOURCE_CONNECT_FAILED(60010, "datasource connect failed", "datasource connect failed"),
    DATASOURCE_CREATE_FAILED(60011, "datasource create failed", "datasource create failed"),
    VIRTUAL_TABLE_CREATE_FAILED(
            60012, "virtual table create failed", "virtual table create failed"),
    VIRTUAL_TABLE_ID_IS_NULL(60013, "virtual table id is null", "virtual table id is null"),
    VIRTUAL_TABLE_FIELD_EMPTY(
            60014, "virtual table field is empty", "virtual table field is empty"),
    DATASOURCE_CAN_NOT_DELETE(
            60015,
            "datasource can not be delete because it used by virtual table",
            "datasource can not be delete because it used by virtual table"),
    VIRTUAL_TABLE_CAN_NOT_DELETE(
            60016,
            "virtual table can not be delete because it used by job",
            "virtual table can not be delete because it used by job"),
    CAN_NOT_FOUND_CONNECTOR_FOR_DATASOURCE(
            60017,
            "can not found connector for datasource",
            "can not found connector for datasource [%s]"),
    DATA_SOURCE_HAD_USED(
            1600000,
            "data source already used ( workflowName - taskName):{0}",
            "datasource is using:{0}"),
    INVALID_DATASOURCE(-70001, "Datasource [{0}] invalid", "datasource [{0}] invalid"),
    MISSING_PARAM(1777000, "param miss [{0}]", "param miss [{0}]"),
    ;

    private final int code;
    private final String msg;
    private final String template;

    SeatunnelErrorEnum(int code, String msg, String template) {
        this.code = code;
        this.msg = msg;
        this.template = template;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public String getTemplate() {
        return template;
    }
}
