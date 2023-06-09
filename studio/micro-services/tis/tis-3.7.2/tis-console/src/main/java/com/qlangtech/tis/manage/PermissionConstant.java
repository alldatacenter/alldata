/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.manage;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-3-16
 */
public interface PermissionConstant {

  // // 不需要校验权限
  public static final String UN_CHECK = "uncheck";

  // 索引配置提取
  String PERMISSION_PLUGIN_GET_CONFIG = "plugin_get_config";

  String PERMISSION_INDEX_QUERY = "index_query";

  // 集群查询，更新状态查询
  String PERMISSION_CLUSTER_STATE_COLLECT = "cluster_state_collect";

  // 增量流程编辑权限
  String PERMISSION_INCR_PROCESS_CONFIG_EDIT = "incr_process_config_edit";

  // 控制增量流程状态
  String PERMISSION_INCR_PROCESS_MANAGE = "incr_process_config_edit";

  // 数据源编辑
  String PERMISSION_DATASOURCE_EDIT = "datasource_edit";

  // 数据流编辑
  String DATAFLOW_ADD = "dataflow_add";

  String DATAFLOW_UPDATE = "dataflow_update";

  String DATAFLOW_DELETE = "dataflow_delete";

  // 数据流控制，例如：触发等
  String DATAFLOW_MANAGE = "dataflow_manage";

  String DATAX_MANAGE = "dataX_manage";

  //
  // // 应用视图
  // public static final String PERMISSION_CORE_VIEW = "core_view";
  // // 应用管理
  // public static final String PERMISSION_BASE_DATA_MANAGE =
  // "base_core_data_manage";
  // // ZK管理 zk_manage
  // public static final String PERMISSION_ZK_MANAGE = "zk_manage";
  // // 线上服务器日志 realtime_log_view
  // public static final String PERMISSION_REALTIME_LOG_VIEW =
  // "realtime_log_view";
  // // 索引查询响应时间 query_react_time_view
  // public static final String PERMISSION_QUERY_REACT_TIME_VIEW =
  // "query_react_time_view";
  // // 索引信息查询 index_query
  // public static final String PERMISSION_INDEX_QUERY = "index_query";
  // // 项目资源配置 core_config_resource_manage
  // public static final String PERMISSION_CORE_CONFIG_RESOURCE_MANAGE =
  // "core_config_resource_manage";
  //
  // // 快照snapshot一览
  // // public static final String PERMISSION_CORE_SNAPSHOT_LIST =
  // "snapshot_list";
  //
  // // ZK发布 core_publish
  // public static final String PERMISSION_CORE_PUBLISH = "core_publish";
  // // 服务器组管理 core_group_manage
  // public static final String PERMISSION_CORE_GROUP_MANAGE =
  // "core_group_manage";
  // 设置定时触发时间(全量/增量)
  public static final String TRIGGER_JOB_SET = "trigger_job_set";

  // 停止/启动定时任务
  public static final String TRIGGER_JOB_PAUSE_RUN_SET = "trigger_job_pause_run_set";

  // 上传配置文件
  public static final String CONFIG_UPLOAD = "config_upload";

  // 同步daily配置
  String CONFIG_SYNCHRONIZE_FROM_DAILY = "config_synchronize_from_daily";

  // public static final String CONFIG_CREATE_SCHEMA = "config_create_schema";// 创建schema
  // 编辑配置文件
  String CONFIG_EDIT = "config_edit";

  // 配置文件snapshot切换
  String CONFIG_SNAPSHOT_CHANGE = "config_snapshot_change";

  // 应用初始化创建Core
  public static final String APP_INITIALIZATION = "app_initialization";

  // 更新schema（全部,组）
  public static final String APP_SCHEMA_UPDATE = "app_schema_update";

  // 更新solrconfig（全部,组,服务器）
  public static final String APP_SOLRCONFIG_UPDATE = "app_solrconfig_update";

  // 副本添加减少
  String APP_REPLICA_MANAGE = "app_replica_manage";

  // 触发全量dump
  String APP_TRIGGER_FULL_DUMP = "app_trigger_full_dump";

  // 重新构建
  String APP_REBUILD = "app_rebuild";

  // 查看索引build结果
  public static final String APP_BUILD_RESULT_VIEW = "app_build_result_view";

  // center
  public static final String CENTER_NODE_SERVERS_LIST = "center_node_servers_list";

  // node中的服务器一览
  // 应用一览
  public static final String APP_LIST = "app_list";

  // 应用更新
  public static final String APP_UPDATE = "app_update";

  // 应用添加
  public static final String APP_ADD = "app_add";

  // 应用删除
  public static final String APP_DELETE = "app_delete";

  // 设置服务器(添加/删除)
  public static final String APP_SERVER_SET = "app_server_set";

  // 服务器组维护(添加/删除)
  public static final String APP_SERVER_GROUP_SET = "app_server_group_set";

  // 全局依赖一览
  public static final String GLOBAL_DEPENDENCY_LIST = "global_dependency_list";

  // 全局依赖上传
  public static final String GLOBAL_DEPENDENCY_UPLOAD = "global_dependency_upload";

  // 全局参数一览
  //  public static final String GLOBAL_PARAMETER_LIST = "global_parameter_list";

  // 全局参数添加
  // public static final String GLOBAL_PARAMETER_ADD = "global_parameter_add";

  // 全局参数设置
  // public static final String GLOBAL_PARAMETER_SET = "global_parameter_set";

  // public static final String GLOBAL_SERVER_POOL_LIST = "global_server_pool_list";// 机器池中机器一览
  // 机器池中机器删除/添加（老版本）
  // public static final String GLOBAL_SERVER_POOL_SET = "global_server_pool_set";

  // Schema类型一览
  public static final String GLOBAL_SCHEMA_TYPE_LIST = "global_schema_type_list";

  // Schema类型添加
  public static final String GLOBAL_SCHEMA_TYPE_SET = "global_schema_type_set";

  // 系统操作日志一览
  public static final String GLOBAL_OPERATION_LOG_LIST = "global_operation_log_list";

  // 用户一览
  public static final String AUTHORITY_USER_LIST = "authority_user_list";

  // 用户角色更新
  public static final String AUTHORITY_USER_ROLE_SET = "authority_user_role_set";

  // 添加角色
  public static final String AUTHORITY_ROLE_ADD = "authority_role_add";

  // 更新角色
  public static final String AUTHORITY_ROLE_UPDATE = "authority_role_update";

  // 角色一览
  public static final String AUTHORITY_ROLE_LIST = "authority_role_list";

  // func一览
  public static final String AUTHORITY_FUNC_LIST = "authority_func_list";

  // 添加func
  public static final String AUTHORITY_FUNC_ADD = "authority_func_add";

  // 删除func
  public static final String AUTHORITY_FUNC_REMOVE = "authority_func_remove";

  // 定时任务一览
  public static final String TRIGGER_JOB_LIST = "trigger_job_list";

  // SNAPSHOT
  public static final String CONFIG_SNAPSHOT_LIST = "config_snapshot_list";

  // 历史一览
  // public static final String CONFIG_SCHEMA_UPDATE = "config_schema_update";// 编辑schema
  // 应用core维护页面
  public static final String APP_CORE_MANAGE_VIEW = "app_core_manage_view";

  // 部门维护（添加，删除，更新）
  public static final String APP_DEPARTMENT_MANAGE = "app_department_manage";

  // 部门一览
  public static final String APP_DEPARTMENT_LIST = "app_department_list";

  // 用户基本更新（update，add，delete）
  public static final String AUTHORITY_USER_MANAGE = "authority_user_manage";

  // 百岁add for 用户申请新应用 start
  // 创建新应用申请
  public static final String APP_APPLY_CREATE = "app_apply_create";

  // 编辑新应用，可以切换到申请的其他状态
  public static final String APP_APPLY_STATE_SET = "app_apply_state_set";
  // 百岁add for 用户申请新应用 end
}
