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
package com.datasophon.api.enums;

import com.alibaba.fastjson.JSONObject;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 *  status enum
 */
public enum Status {

    SUCCESS(200, "success", "成功"),

    INTERNAL_SERVER_ERROR_ARGS(10000, "Internal Server Error: {0}", "服务端异常: {0}"),

    USER_NAME_EXIST(10003, "user name already exists", "用户名已存在"),
    USER_NAME_NULL(10004,"user name is null", "用户名不能为空"),
    USER_NOT_EXIST(10010, "user {0} not exists", "用户[{0}]不存在"),
    USER_NAME_PASSWD_ERROR(10013,"user name or password error", "用户名或密码错误"),
    LOGIN_SESSION_FAILED(10014,"create session failed!", "创建session失败"),
    REQUEST_PARAMS_NOT_VALID_ERROR(10101, "request parameter {0} is not valid", "请求参数[{0}]无效"),
    CREATE_USER_ERROR(10090,"create user error", "创建用户错误"),
    QUERY_USER_LIST_PAGING_ERROR(10091,"query user list paging error", "分页查询用户列表错误"),
    UPDATE_USER_ERROR(10092,"update user error", "更新用户错误"),
    LOGIN_SUCCESS(10042,"login success", "登录成功"),
    IP_IS_EMPTY(10125,"ip is empty", "IP地址不能为空"),
    DELETE_USER_BY_ID_ERROR(10093,"delete user by id error", "删除用户错误"),

    START_CHECK_HOST(10000,"start check host","开始主机校验"),
    CHECK_HOST_SUCCESS(10001,"check host success","主机校验成功"),
    NEED_JAVA_ENVIRONMENT(10002,"need java environment","缺少Java环境"),
    CONNECTION_FAILED(10003,"connection failed","主机连接失败"),
    NEED_HOSTNAME(10004,"need hostname","无法获取主机名"),
    CAN_NOT_GET_IP(10005,"can not get ip","无法获取ip地址"),
    INSTALL_SERVICE(10006,"Install Service ","安装服务"),

    CLUSTER_CODE_EXISTS(10007,"cluster code exists","集群编码已存在"),
    ALERT_GROUP_TIPS_ONE(10008,"an alarm group has been bound to an alarm indicator, delete the bound alarm indicator first","当前告警组已绑定告警指标，请先删除绑定的告警指标"),
    GROUP_NAME_DUPLICATION(10009,"group name duplication, delete the bound alarm indicator first","重复组名"),
    USER_GROUP_TIPS_ONE(10011,"the current user group has users,delete the users first","当前用户组存在用户，请先删除用户"),
    HOST_EXIT_ONE_RUNNING_ROLE(10012,"at least one role is running on the host:","主机存在正在运行的角色:"),
    REPEAT_NODE_LABEL(10015,"repeat node label","重复节点标签"),
    ADD_YARN_NODE_LABEL_FAILED(10016,"add yarn node label failed","添加yarn节点标签失败"),
    NODE_LABEL_IS_USING(10017,"node label is using","节点标签正在使用"),
    CONFIG_CAPACITY_SCHEDULER_FAILED(10018,"config capacity-scheduler.xml failed","配置capacity-scheduler.xml失败"),
    FAILED_REFRESH_THE_QUEUE_TO_YARN(10019,"description Failed to refresh the queue to Yarn","刷新队列到Yarn失败"),
    RACK_IS_USING(10020,"rack is using","机架正在使用"),
    NO_SERVICE_EXECUTE(10021,"there is no service to execute","没有要执行的服务"),
    EXIT_RUNNING_ROLE_INSTANCE (10022,"It has running role instance,stop it first","它有正在运行的角色实例，请先停止它"),
    REPEAT_ROLE_GROUP_NAME (10023,"repeat role group name","重复角色组名称"),
    THE_CURRENT_ROLE_GROUP_BE_USING (10024,"the current role group is in use,do not delete it","当前角色组正在使用，请勿删除"),
    EXIT_RUNNING_INSTANCES (10025,"there are running instances and ignore it when delete","存在正在运行的实例，删除时忽略它"),
    ROLE_GROUP_HAS_NO_OUTDATED_SERVICE (10026,"this role group has no outdated service","该角色组没有过时服务"),
    DUPLICATE_USER_NAME (10027,"duplicate user name","用户名重复"),
    QUEUE_NAME_ALREADY_EXISTS (10028,"the queue name already exists","队列名已存在"),
    THREE_JOURNALNODE_DEPLOYMENTS_REQUIRED (10030,"three JournalNode deployments are required","JournalNode需要部署三台"),
    TWO_NAMENODES_NEED_TO_BE_DEPLOYED (10031,"two Namenodes need to be deployed","NameNode需要部署两台"),
    TWO_ZKFC_DEVICES_ARE_REQUIRED (10032,"two ZKFC devices are required","ZKFC需要部署两台"),
    TWO_RESOURCEMANAGER_ARE_DEPLOYED (10033,"two ResourceManager are deployed","ResourceManager需要部署两台"),
    SELECT_LEAST_ONE_HOST (10034,"select at least one host","至少选择一台主机"),

    USER_NO_OPERATION_PERM(30001, "user has no operation privilege", "当前用户没有操作权限"),
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

    public JSONObject toJson(){
        JSONObject json = new JSONObject();
        json.put("code",this.code);
        json.put("msg",getMsg());
        return json;
    }

    public String getMsg() {
        if (Locale.SIMPLIFIED_CHINESE.getLanguage().equals(LocaleContextHolder.getLocale().getLanguage())) {
            return this.zhMsg;
        } else {
            return this.enMsg;
        }
    }
}
