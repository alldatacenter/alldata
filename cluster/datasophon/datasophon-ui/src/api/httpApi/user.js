/*
 * @Author: mjzhu
 * @Date: 2022-05-24 10:22:10
 * @LastEditTime: 2022-06-14 19:59:16
 * @FilePath: \ddh-ui\src\api\httpApi\user.js
 */
import paths from '@/api/baseUrl'// 后台服务地址

let path = paths.path() + '/ddh'
console.log(path, '请求的地址')
export default {
  login: path + '/login',
  loginOut: path + '/signOut',
  getUserList: path + '/api/user/list',// 用户列表
  addUser: path + '/api/user/save',// 添加用户
  deleteUser: path + '/api/user/delete',// 删除用户
  updateUser: path + '/api/user/update',// 更新用户
  queryAllUser: path + '/api/user/all',
  getTenant:path + '/cluster/user/list',
  getTenantGroup:path + '/cluster/group/list',
}
