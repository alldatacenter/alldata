import request from '@/utils/request'

// dashborad

export function chartInfo(data) {
  return request({
    url: '/system/api/chartInfo',
    method: 'post',
    data
  })
}
