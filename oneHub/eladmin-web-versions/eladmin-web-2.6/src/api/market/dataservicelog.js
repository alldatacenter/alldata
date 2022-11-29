import request from '@/utils/request'

export function pageDataServiceLog(data) {
  return request({
    url: '/data/service/serviceLogs/page',
    method: 'get',
    params: data
  })
}

export function getDataServiceLog(id) {
  return request({
    url: '/data/service/serviceLogs/' + id,
    method: 'get'
  })
}

export function delDataServiceLog(id) {
  return request({
    url: '/data/service/serviceLogs/' + id,
    method: 'delete'
  })
}

export function delDataServiceLogs(ids) {
  return request({
    url: '/data/service/serviceLogs/batch/' + ids,
    method: 'delete'
  })
}
