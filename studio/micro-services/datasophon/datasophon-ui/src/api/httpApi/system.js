/*
 * @Author: mjzhu
 * @describe: 
 * @Date: 2022-06-20 16:28:22
 * @LastEditTime: 2022-08-15 14:09:36
 * @FilePath: \ddh-ui\src\api\httpApi\system.js
 */

import paths from '@/api/baseUrl'// 后台服务地址

let path = paths.path() + '/ddh'
export default {
  getServiceListByCluster: path + '/cluster/service/instance/list', // 选择服务的列表
  instanceList: path + '/cluster/service/role/instance/list', // 选择服务的列表
  getConfigInfo: path + '/cluster/service/instance/config/info', // 查询服务配置 
  getConfigVersion: path + '/cluster/service/instance/config/getConfigVersion', // 查询服务版本 
  configVersionCompare:path + '/cluster/service/instance/configVersionCompare', // 服务版本比对 ,
  getHostListByPage: path + '/api/cluster/host/list', // 分页查询集群主机  
  generateServiceCommand: path + '/api/cluster/service/command/generateServiceCommand', // 生成服务操作指令 
  generateServiceRoleCommand: path + '/api/cluster/service/command/generateServiceRoleCommand', // 生成服务角色操作指令 
  getAlertList: path + '/cluster/alert/history/getAlertList', // 查询服务告警列表 
  deleteExample: path + '/cluster/service/role/instance/delete', // 删除角色实例 
  restartObsoleteService:path + '/cluster/service/role/instance/restartObsoleteService', // 重启过时服务 ,
  decommissionNode:path + '/cluster/service/role/instance/decommissionNode', // 退役该节点 ,
  getWebUis: path + '/cluster/webuis/getWebUis', // 查询webuis 
  getServiceRoleType:path + '/cluster/service/instance/getServiceRoleType', // 角色组 查询角色类型 ,
  getRoleGroupList:path + '/cluster/service/instance/role/group/list', // 角色组 查询角色组列表 ,
  editRoleGroupBind:path + '/cluster/service/instance/role/group/bind', // 角色组 分配角色组 ,
  addRoleGroupSave:path + '/cluster/service/instance/role/group/save', // 角色组 保存角色组 ,


  // 告警模块
  getAlarmGroupList: path + '/alert/group/list', // 告警组列表  
  getAlarmMerticList: path + '/cluster/alert/quota/list', // 告警指标列表
  getAlarmCate: path + '/api/frame/service/list', // 查询服务列表 告警组类别
  getAlarmRole: path + '/api/frame/service/role/getServiceRoleByServiceName', // 查询服务列表 告警组类别
  saveGroup: path + '/alert/group/save', // 查询服务列表 告警组类别
  saveMetric: path + '/cluster/alert/quota/save', // 告警指标保存
  updateMetric: path + '/cluster/alert/quota/update', // 查询服务列表 告警组类别
  deleteGroup: path + '/alert/group/delete', // 告警组删除 
  deleteMetric: path + '/cluster/alert/quota/delete', // 告警指标删除
  getAllAlertList: path + '/cluster/alert/history/getAllAlertList', // 查询所有告警
  quotaStart: path + '/cluster/alert/quota/start', // 启用告警指标
}
