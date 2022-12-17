import request from '@/utils/request'

export function testDbConnect(data) {
  return request({
    url: '/system/api/database/testConnect',
    method: 'post',
    data
  })
}

export function testServerConnect(data) {
  return request({
    url: '/system/api/serverDeploy/testConnect',
    method: 'post',
    data
  })
}
