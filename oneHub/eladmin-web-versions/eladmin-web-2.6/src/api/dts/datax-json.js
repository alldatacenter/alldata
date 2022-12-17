import request from '@/utils/request'

// buildJobJson
export function buildJobJson(data) {
  return request({
    url: '/system/api/flinkxJson/buildJson',
    method: 'post',
    data
  })
}
