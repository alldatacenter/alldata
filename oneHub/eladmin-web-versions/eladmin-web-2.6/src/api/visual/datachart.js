import request from '@/utils/request'

export function listDataChart(data) {
  return request({
    url: '/data/visual/charts/list',
    method: 'get',
    params: data
  })
}

export function pageDataChart(data) {
  return request({
    url: '/data/visual/charts/page',
    method: 'get',
    params: data
  })
}

export function getDataChart(id) {
  return request({
    url: '/data/visual/charts/' + id,
    method: 'get'
  })
}

export function delDataChart(id) {
  return request({
    url: '/data/visual/charts/' + id,
    method: 'delete'
  })
}

export function addDataChart(data) {
  return request({
    url: '/data/visual/charts',
    method: 'post',
    data: data
  })
}

export function updateDataChart(data) {
  return request({
    url: '/data/visual/charts/' + data.id,
    method: 'put',
    data: data
  })
}

export function copyDataChart(id) {
  return request({
    url: '/data/visual/charts/copy/' + id,
    method: 'post'
  })
}

export function buildDataChart(data) {
  return request({
    url: '/data/visual/charts/build/' + data.id,
    method: 'put',
    data: data
  })
}

export function dataParser(data) {
  return request({
    url: '/data/visual/charts/data/parser',
    headers: {
      'showLoading': false
    },
    method: 'post',
    data: data
  })
}
