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
package io.datavines.core.enums;

import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;
import java.util.Optional;

public enum Status {

    /**
     * xx-xx-xxxx
     * 10 系统内置
     * - 01 系统
     * - 02 用户
     * 11 工作空间
     * 12 数据源
     * 13 任务
     * 14 SLA 告警
     */
    SUCCESS(200, "success", "成功"),
    FAIL(400, "Bad Request", "错误的请求"),
    REQUEST_ERROR(10010001, "Request Error", "请求错误"),
    INVALID_TOKEN(10010002, "Invalid Token ：{0}", "无效的Token ：{0}"),
    TOKEN_IS_NULL_ERROR(10010003, "Token is Null Error", "Token为空错误"),
    PLEASE_LOGIN(10010004, "Please login", "请登录"),
    USERNAME_HAS_BEEN_REGISTERED_ERROR(10020001, "The username {0} has been registered", "用户名 {0} 已被注册过"),
    REGISTER_USER_ERROR(10020002, "Register User {0} Error", "注册用户{0}失败"),
    USERNAME_OR_PASSWORD_ERROR(10020003, "Username or Email Error", "用户名或者密码错误"),
    USER_IS_NOT_EXIST_ERROR(10020004, "User is not exist", "用户不存在错误"),
    CREATE_VERIFICATION_IMAGE_ERROR(10020005, "create verification image error", "创建验证码错误"),
    EXPIRED_VERIFICATION_CODE(10020006, "expired verification code", "验证码已过期，请重新刷新"),
    INVALID_VERIFICATION_CODE(10020007, "invalid verification code", "错误的验证码，请重新输入"),

    WORKSPACE_EXIST_ERROR(11010001, "WorkSpace {0} is Exist error", "工作空间 {0} 已存在错误"),
    CREATE_WORKSPACE_ERROR(11010002, "Create WorkSpace {0} Error", "创建工作空间 {0} 错误"),
    WORKSPACE_NOT_EXIST_ERROR(11010003, "WorkSpace {0} is Not Exist Error", "工作空间 {0} 不存在错误"),
    UPDATE_WORKSPACE_ERROR(11010004, "Update WorkSpace {0} Error", "更新工作空间 {0} 错误"),
    USER_IS_IN_WORKSPACE_ERROR(11010005, "User is in Workspace", "用户已经在工作空间错误"),
    USER_HAS_NO_AUTHORIZE_TO_REMOVE(11010006, "User has no authorize to remove", "用户没有权限移除错误"),
    USER_HAS_ONLY_ONE_WORKSPACE(11010007, "User has only one Workspace, can not exist", "用户只有一个工作空间，无法移除或退出"),

    DATASOURCE_EXIST_ERROR(12010001, "DataSource {0} is Exist error", "数据源 {0} 已存在错误"),
    CREATE_DATASOURCE_ERROR(12010002, "Create DataSource {0} Error", "创建数据源 {0} 错误"),
    DATASOURCE_NOT_EXIST_ERROR(12010003, "DataSource {0} is Not Exist Error", "数据源 {0} 不存在错误"),
    UPDATE_DATASOURCE_ERROR(12010004, "Update DataSource {0} Error", "更新数据源 {0} 错误"),
    GET_DATABASE_LIST_ERROR(12010005, "Get DataSource {0} Database List Error", "获取数据源 {0} 数据库列表错误"),
    GET_TABLE_LIST_ERROR(12010006, "Get DataSource {0} Database {1} Table List Error", "获取数据源 {0} 数据库 {1} 表列表错误"),
    GET_COLUMN_LIST_ERROR(12010007, "Get DataSource {0} Database {1} Table {2} Column List Error", "获取数据源 {0} 数据库 {1} 表 {2} 字段列表错误"),
    EXECUTE_SCRIPT_ERROR(12010008, "Execute Script {0} Error", "执行脚本 {0} 错误"),

    TASK_NOT_EXIST_ERROR(13010001, "Task {0} Not Exist Error", "任务{0}不存在错误"),
    TASK_LOG_PATH_NOT_EXIST_ERROR(13010002, "Task {0} Log Path  Not Exist Error", "任务 {0} 的日志路径不存在错误"),
    TASK_EXECUTE_HOST_NOT_EXIST_ERROR(13010003, "Task Execute Host {0} Not Exist Error", "任务 {0} 的执行服务地址不存在错误"),

    JOB_PARAMETER_IS_NULL_ERROR(14010001, "Job {0} Parameter is Null Error", "作业 {0} 参数为空错误"),
    CREATE_JOB_ERROR(14010002, "Create Job {0} Error", "创建作业 {0} 错误"),
    JOB_NOT_EXIST_ERROR(14010003, "Job {0} Not Exist Error", "作业 {0} 不存在错误"),
    JOB_EXIST_ERROR(14010004, "Job {0} is Exist error", "作业 {0} 已存在错误"),
    UPDATE_JOB_ERROR(14010005, "Update Job {0} Error", "更新作业 {0} 错误"),
    METRIC_JOB_RELATED_ENTITY_NOT_EXIST(14010006, "The entity that want to check is null", "找不到要进行检查的实体"),
    METRIC_NOT_SUITABLE_ENTITY_TYPE(14010007, "The Metric [{0}] is not Suit Entity Type [{1}]", "规则[{0}]不能用于检查数据类型[{1}] "),
    ENTITY_TYPE_NOT_EXIST(14010008, "The Column Type is not Exist", "列类型不存在"),
    METRIC_IS_NOT_EXIST(14010009, "The Metric {0} is not exist", "规则 {0} 不存在"),
    MULTI_TABLE_ACCURACY_NOT_SUPPORT_LOCAL_ENGINE(14010010, "Local Engine not support multi table accuracy in one datasource", "Local引擎不支持跨表准确性检查"),
    JOB_SCHEDULE_EXIST_ERROR(14020001, "Job Schedule is Exist error, id must be not null", "作业定时任务已存在,ID 不能为空"),
    CREATE_JOB_SCHEDULE_ERROR(14020002, "Create Job Schedule {0} Error", "创建作业定时任务 {0} 错误"),
    JOB_SCHEDULE_NOT_EXIST_ERROR(14020003, "Job Schedule {0} is not Exist error", "作业定时任务 {0} 不存在错误"),
    UPDATE_JOB_SCHEDULE_ERROR(14020004, "Update Job Schedule {0} Error", "更新作业定时任务 {0} 错误"),
    ADD_QUARTZ_ERROR(14020005, "Create Quartz Error {0}", "创建定时器错误: {0}"),
    SCHEDULE_PARAMETER_IS_NULL_ERROR(14020006, "Schedule {0} Parameter is Null Error", "定时器参数 {0} 为空错误"),
    SCHEDULE_TYPE_NOT_VALIDATE_ERROR(14020007, "Schedule type {0} is not Validate Error", "定时器类型参数 {0} 错误"),
    SCHEDULE_CYCLE_NOT_VALIDATE_ERROR(14020008, "Schedule Param Cycle {0} is not Validate Error", "定时器周期参数 {0} 错误"),
    SCHEDULE_CRON_IS_INVALID_ERROR(14020008, "Schedule cron {0} is not Validate Error", "定时器 Crontab 表达式 {0} 错误"),

    CREATE_TENANT_ERROR(15010001, "Create Tenant {0} Error", "创建 Linux 用户 {0} 错误"),
    TENANT_NOT_EXIST_ERROR(15010002, "Tenant {0} Not Exist Error", "Linux 用户 {0} 不存在错误"),
    TENANT_EXIST_ERROR(15010003, "Tenant {0} is Exist error", "Linux 用户 {0} 已存在错误"),
    UPDATE_TENANT_ERROR(15010004, "Update Tenant {0} Error", "更新 Linux 用户 {0} 错误"),

    CREATE_ENV_ERROR(16010001, "Create Env {0} Error", "创建运行环境参数 {0} 错误"),
    ENV_NOT_EXIST_ERROR(16010002, "Env {0} Not Exist Error", "运行环境参数 {0} 不存在错误"),
    ENV_EXIST_ERROR(16010003, "Env {0} is Exist error", "运行环境参数 {0} 已存在错误"),
    UPDATE_ENV_ERROR(16010004, "Update Env {0} Error", "更新运行环境参数 {0} 错误"),

    CREATE_ERROR_DATA_STORAGE_ERROR(17010001, "Create Error Data Storage {0} Error", "创建错误数据存储 {0} 错误"),
    ERROR_DATA_STORAGE_NOT_EXIST_ERROR(17010002, "Error Data Storage {0} Not Exist Error", "错误数据存储 {0} 不存在"),
    ERROR_DATA_STORAGE_EXIST_ERROR(17010003, "Error Data Storage {0} is Exist error", "错误数据存储 {0} 已存在"),
    UPDATE_ERROR_DATA_STORAGE_ERROR(17010004, "Update Error Data Storage {0} Error", "更新 错误数据存储 {0} 错误"),

    SLA_ALREADY_EXIST_ERROR(18010001, "SLA {0} Already exist", "SLA {0} 已经存在"),
    SLA_SENDER_ALREADY_EXIST_ERROR(18020001, "SLA Sender {0}  Already exist", "SLA 发送器 {0} 已经存在"),
    SLA_JOB_IS_NOT_EXIST_ERROR(18010003, "SLA job {0} is not exist", "SLA job {0} 存在"),

    CATALOG_FETCH_DATASOURCE_NULL_ERROR(19010001, "获取元数据时数据源为空", "DataSource must not be null when fetch metadata"),

    CATALOG_FETCH_METADATA_PARAMETER_ERROR(19010002, "获取元数据参数错误", "Fetch Metadata Parameter Error"),
    CATALOG_TAG_CATEGORY_CREATE_ERROR(20010001, "Create Tag Category {0} Error", "创建标签类别 {0} 错误"),
    CATALOG_TAG_CATEGORY_NOT_EXIST_ERROR(20010002, "Tag Category {0} Not Exist Error", "标签类别 {0} 不存在"),
    CATALOG_TAG_CATEGORY_EXIST_ERROR(20010003, "Tag Category {0} is Exist error", "标签类别 {0} 已存在"),
    CATALOG_TAG_CREATE_ERROR(20020001, "Create Tag {0} Error", "创建标签 {0} 错误"),
    CATALOG_TAG_NOT_EXIST_ERROR(20020002, "Tag {0} Not Exist Error", "标签 {0} 不存在"),
    CATALOG_TAG_EXIST_ERROR(20020003, "Tag {0} is Exist error", "标签 {0} 已存在"),
    CATALOG_ENTITY_TAG_EXIST_ERROR(20020004, "The tag is already associated with the entity", "该标签已与实体关联"),
    CATALOG_TASK_SCHEDULE_EXIST_ERROR(20030001, "Catalog Task Schedule is Exist error, id must be not null", "元数据抓取定时任务已存在,ID 不能为空"),
    CREATE_CATALOG_TASK_SCHEDULE_ERROR(20030002, "Create Catalog Task Schedule {0} Error", "创建元数据抓取定时任务 {0} 错误"),
    CATALOG_TASK_SCHEDULE_NOT_EXIST_ERROR(20030003, "Catalog Task Schedule {0} is not Exist error", "元数据抓取定时任务 {0} 不存在"),
    UPDATE_CATALOG_TASK_SCHEDULE_ERROR(20030004, "Update Catalog Task Schedule {0} Error", "更新元数据抓取定时任务 {0} 错误"),
    CATALOG_PROFILE_INSTANCE_FQN_ERROR(20030004, "Catalog instance fqn {0} Error", "数据实体全限定名 {0} 错误"),

    CREATE_ISSUE_ERROR(21010001, "Create Issue {0} Error", "创建Issue {0} 错误"),
    ISSUE_NOT_EXIST_ERROR(21010002, "Issue {0} Not Exist Error", "Issue {0} 不存在错误"),
    ISSUE_EXIST_ERROR(21010003, "Issue {0} is Exist error", "Issue {0} 已存在错误"),
    UPDATE_ISSUE_ERROR(21010004, "Update Issue {0} Error", "更新Issue {0} 错误"),
    ;

    private final int code;
    private final String enMsg;
    private final String zhMsg;

    Status(int code, String enMsg, String zhMsg) {
        this.code = code;
        this.enMsg = enMsg;
        this.zhMsg = zhMsg;
    }

    public int getCode() {
        return this.code;
    }

    public String getMsg() {
        if (Locale.SIMPLIFIED_CHINESE.getLanguage().equals(LocaleContextHolder.getLocale().getLanguage())) {
            return this.zhMsg;
        } else {
            return this.enMsg;
        }
    }

    /**
     * Retrieve Status enum entity by status code.
     */
    public static Optional<Status> findStatusBy(int code) {
        for (Status status : Status.values()) {
            if (code == status.getCode()) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}
