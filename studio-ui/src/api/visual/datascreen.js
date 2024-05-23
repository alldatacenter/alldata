import request from '@/utils/request'

export function pageDataScreen(data) {
  return request({
    url: '/data/visual/screens/page',
    method: 'get',
    params: data
  })
}

export function getDataScreen(id) {
  return request({
    url: '/data/visual/screens/' + id,
    method: 'get'
  })
}

export function delDataScreen(id) {
  return request({
    url: '/data/visual/screens/' + id,
    method: 'delete'
  })
}

export function addDataScreen(data) {
  return request({
    url: '/data/visual/screens',
    method: 'post',
    data: data
  })
}

export function updateDataScreen(data) {
  return request({
    url: '/data/visual/screens/' + data.id,
    method: 'put',
    data: data
  })
}

export function copyDataScreen(id) {
  return request({
    url: '/data/visual/screens/copy/' + id,
    method: 'post'
  })
}

export function buildDataScreen(data) {
  return request({
    url: '/data/visual/screens/build/' + data.id,
    method: 'put',
    data: data
  })
}
