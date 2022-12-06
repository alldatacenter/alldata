import request from '@/utils/request'

// dashborad

export function chartInfo(data) {
  return request({
    url: '/api/chartInfo',
    method: 'post',
    data
  })
}
