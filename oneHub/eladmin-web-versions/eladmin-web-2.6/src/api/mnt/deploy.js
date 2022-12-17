import request from '@/utils/request'

export function add(data) {
  return request({
    url: '/system/api/deploy',
    method: 'post',
    data
  })
}

export function del(ids) {
  return request({
    url: '/system/api/deploy',
    method: 'delete',
    data: ids
  })
}

export function edit(data) {
  return request({
    url: '/system/api/deploy',
    method: 'put',
    data
  })
}

export function getApps() {
  return request({
    url: '/system/api/app',
    method: 'get'
  })
}

export function getServers() {
  return request({
    url: '/system/api/serverDeploy',
    method: 'get'
  })
}

/**
 * 启动服务
 * @param data 选中行
 */
export function startServer(data) {
  return request({
    url: '/system/api/deploy/startServer',
    method: 'post',
    data
  })
}

/**
 * 停止服务
 * @param data  选中行
 */
export function stopServer(data) {
  return request({
    url: '/system/api/deploy/stopServer',
    method: 'post',
    data
  })
}

/**
 * 停止服务
 * @param data  选中行
 */
export function serverStatus(data) {
  return request({
    url: '/system/api/deploy/serverStatus',
    method: 'post',
    data
  })
}

export default { add, edit, del, stopServer, serverStatus, startServer, getServers, getApps }
