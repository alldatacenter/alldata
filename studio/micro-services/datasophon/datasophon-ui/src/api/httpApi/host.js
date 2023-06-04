/*
 * @Author: mjzhu
 * @Date: 2022-05-24 10:22:10
 * @LastEditTime: 2022-08-15 14:09:55
 * @FilePath: \ddh-ui\src\api\httpApi\host.js
 */
import paths from '@/api/baseUrl'// 后台服务地址

let path = paths.path() + '/ddh'
export default {
  reStartDispatcherHostAgent: path + '/host/install/reStartDispatcherHostAgent', // 主机agent分发重试
  dispatcherHostAgentList: path + '/host/install/dispatcherHostAgentList', // 主机agent分发进度列表
  rehostCheck: path+ '/host/install/rehostCheck', // 重试主机环境校验
  analysisHostList: path + '/host/install/analysisHostList', // 解析主机列表
  hostCheckCompleted: path + '/host/install/hostCheckCompleted', // 查询主机环境校验是否完成
  dispatcherHostAgentCompleted: path + '/host/install/dispatcherHostAgentCompleted', // 查询主机agent分发是否完成
  getRack: path + '/api/cluster/host/getRack', // 查询机架
  updateRack: path + '/api/cluster/host/update', // 分配机架
  deleteRack: path + '/api/cluster/host/delete', // 分配机架
  getRoleListByHostname: path + '/api/cluster/host/getRoleListByHostname', // 根据主机查询角色列表
}
