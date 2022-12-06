import request from '@/utils/request'

// datax插件api

export function getList(params) {
  return request({
    url: '/api/dataxPlugin',
    method: 'get',
    params
  })
}

export function fetchPlugin(params) {
  return request({
    url: '/api/dataxPlugin/' + params,
    method: 'get'
  })
}

export function updatePlugin(data) {
  return request({
    url: '/api/dataxPlugin/',
    method: 'put',
    data
  })
}

export function createPlugin(data) {
  return request({
    url: '/api/dataxPlugin/',
    method: 'post',
    data
  })
}

export function deletePlugin(data) {
  return request({
    url: '/api/dataxPlugin/',
    method: 'delete',
    params: data
  })
}
