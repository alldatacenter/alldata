import request from '@/utils/request'

// 刷新参数缓存
export function refreshAuthorize() {
  return request({
    url: '/data/metadata/authorizes/refresh',
    method: 'get'
  })
}

export function getAuthorizedMetadata(id) {
  return request({
    url: '/data/metadata/authorizes/getAuthorizedMetadata/' + id,
    method: 'get'
  })
}

export function metadataAuthorize(data) {
  return request({
    url: '/data/metadata/authorizes/metadata',
    method: 'post',
    data: data
  })
}
