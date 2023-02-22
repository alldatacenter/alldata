/*
 * @Author: mjzhu
 * @Date: 2022-05-24 10:22:10
 * @LastEditTime: 2022-11-25 16:51:51
 * @FilePath: \ddh-ui\src\api\httpApi\cluster.js
 */
import paths from '@/api/baseUrl'// 后台服务地址

let path = paths.path() + '/ddh'
export default {
  getColonyList: path+ '/api/cluster/list', // 获取集群列表
  saveColony: path + '/api/cluster/save', // 集群保存
  updateColony: path + '/api/cluster/update', // 集群更新
  deleteColony: path + '/api/cluster/delete', // 集群删除
  authCluster: path +'/api/cluster/user/saveClusterManager', // 集群授权
  getFrameList: path + '/api/frame/list',// 获取服务框架列表
  runningClusterList: path + '/api/cluster/runningClusterList',// 正在运行状态集群列表
  getDashboardUrl: path + '/cluster/service/dashboard/getDashboardUrl',// 查询总览地址
  reNameGroup: path + '/cluster/service/instance/role/group/rename',
  delGroup: path + '/cluster/service/instance/role/group/delete',
  saveLabel: path + '/cluster/node/label/save',
  assginLabel: path + '/cluster/node/label/assign',
  deleteLabel: path + '/cluster/node/label/delete',
  getLabelList: path + '/cluster/node/label/list',
  saveRack: path + '/cluster/rack/save',
  assginRack: path + '/api/cluster/host/assignRack',
  deleteRack: path + '/cluster/rack/delete',
  getRackList: path + '/cluster/rack/list',
}
