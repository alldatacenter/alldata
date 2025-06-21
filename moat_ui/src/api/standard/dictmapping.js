import request from '@/utils/request'

export function getDictMapping(id) {
  return request({
    url: '/data/standard/mappings/' + id,
    method: 'get'
  })
}

export function dictAutoMapping(id) {
  return request({
    url: '/data/standard/mappings/auto/' + id,
    method: 'post'
  })
}

export function dictManualMapping(data) {
  return request({
    url: '/data/standard/mappings/manual',
    method: 'post',
    data: data
  })
}

export function dictCancelMapping(id) {
  return request({
    url: '/data/standard/mappings/cancel/' + id,
    method: 'post'
  })
}
