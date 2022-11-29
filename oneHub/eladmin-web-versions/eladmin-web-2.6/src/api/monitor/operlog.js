import request from '@/utils/request'

export function pageLog (data) {
  return request({
    url: '/system/logs/page',
    method: 'get',
    params: data
  })
}

export function getLog (id) {
  return request({
    url: '/system/logs/' + id,
    method: 'get'
  })
}

export function delLog (id) {
  return request({
    url: '/system/logs/' + id,
    method: 'delete'
  })
}

export function delLogs (ids) {
  return request({
    url: '/system/logs/batch/' + ids,
    method: 'delete'
  })
}
