import request from '@/utils/request'

export function listDataSet(data) {
  return request({
    url: '/data/visual/dataSets/list',
    method: 'get',
    params: data
  })
}

export function pageDataSet(data) {
  return request({
    url: '/data/visual/dataSets/page',
    method: 'get',
    params: data
  })
}

export function getDataSet(id) {
  return request({
    url: '/data/visual/dataSets/' + id,
    method: 'get'
  })
}

export function delDataSet(id) {
  return request({
    url: '/data/visual/dataSets/' + id,
    method: 'delete'
  })
}

export function delDataSets(ids) {
  return request({
    url: '/data/visual/dataSets/batch/' + ids,
    method: 'delete'
  })
}

export function addDataSet(data) {
  return request({
    url: '/data/visual/dataSets',
    method: 'post',
    data: data
  })
}

export function updateDataSet(data) {
  return request({
    url: '/data/visual/dataSets/' + data.id,
    method: 'put',
    data: data
  })
}

export function sqlAnalyse(data) {
  return request({
    url: '/data/visual/dataSets/sql/analyse',
    method: 'post',
    data: data
  })
}
