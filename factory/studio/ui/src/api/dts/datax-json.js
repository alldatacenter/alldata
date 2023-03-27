import request from '@/utils/request'

// buildJobJson
export function buildJobJson(data) {
  return request({
    url: '/data/dts/flinkxJson/buildJson',
    method: 'post',
    data
  })
}
