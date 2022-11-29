import request from '@/utils/request'

export function pageApiLog(data) {
  return request({
    url: '/data/api/apiLogs/page',
    method: 'get',
    params: data
  })
}

export function getApiLog(id) {
  return request({
    url: '/data/api/apiLogs/' + id,
    method: 'get'
  })
}

export function delApiLog(id) {
  return request({
    url: '/data/api/apiLogs/' + id,
    method: 'delete'
  })
}

export function delApiLogs(ids) {
  return request({
    url: '/data/api/apiLogs/batch/' + ids,
    method: 'delete'
  })
}
