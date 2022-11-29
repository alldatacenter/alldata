import request from '@/utils/request'

export function pageLog (data) {
  return request({
    url: '/quartz/logs/page',
    method: 'get',
    params: data
  })
}

export function getLog (id) {
  return request({
    url: '/quartz/logs/' + id,
    method: 'get'
  })
}

export function delLog (id) {
  return request({
    url: '/quartz/logs/' + id,
    method: 'delete'
  })
}

export function delLogs (ids) {
  return request({
    url: '/quartz/logs/batch/' + ids,
    method: 'delete'
  })
}
