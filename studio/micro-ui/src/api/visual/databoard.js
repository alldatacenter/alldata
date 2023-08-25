import request from '@/utils/request'

export function pageDataBoard(data) {
  return request({
    url: '/data/visual/boards/page',
    method: 'get',
    params: data
  })
}

export function getDataBoard(id) {
  return request({
    url: '/data/visual/boards/' + id,
    method: 'get'
  })
}

export function delDataBoard(id) {
  return request({
    url: '/data/visual/boards/' + id,
    method: 'delete'
  })
}

export function addDataBoard(data) {
  return request({
    url: '/data/visual/boards',
    method: 'post',
    data: data
  })
}

export function updateDataBoard(data) {
  return request({
    url: '/data/visual/boards/' + data.id,
    method: 'put',
    data: data
  })
}

export function copyDataBoard(id) {
  return request({
    url: '/data/visual/boards/copy/' + id,
    method: 'post'
  })
}

export function buildDataBoard(data) {
  return request({
    url: '/data/visual/boards/build/' + data.id,
    method: 'put',
    data: data
  })
}
