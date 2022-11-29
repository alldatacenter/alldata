import request from '@/utils/request'

export function getContrastTree(data) {
  return request({
    url: '/data/standard/contrasts/tree',
    method: 'get',
    params: data
  })
}

export function addContrast(data) {
  return request({
    url: '/data/standard/contrasts',
    method: 'post',
    data: data
  })
}

export function updateContrast(data) {
  return request({
    url: '/data/standard/contrasts/' + data.id,
    method: 'put',
    data: data
  })
}

export function delContrast(id) {
  return request({
    url: '/data/standard/contrasts/' + id,
    method: 'delete'
  })
}

export function contrastStat(data) {
  return request({
    url: '/data/standard/contrasts/stat',
    method: 'get',
    params: data
  })
}
