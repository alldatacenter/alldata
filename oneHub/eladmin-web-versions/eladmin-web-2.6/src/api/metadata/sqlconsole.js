import request from '@/utils/request'

export function runSql (data) {
  return request({
    url: '/data/console/sql/run',
    method: 'post',
    data: data
  })
}

export function stopSql (data) {
  return request({
    url: '/data/console/sql/stop',
    method: 'post',
    data: data
  })
}
