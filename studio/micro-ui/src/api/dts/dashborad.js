import request from '@/utils/request'

// dashborad

export function chartInfo(data) {
  return request({
    url: '/data/dts/chartInfo',
    method: 'post',
    data
  })
}
