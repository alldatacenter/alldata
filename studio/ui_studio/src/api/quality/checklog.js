import request from '@/utils/request'

export function pageCheckLog(data) {
  return request({
    url: '/data/quality/scheduleLogs/page',
    method: 'get',
    params: data
  })
}
