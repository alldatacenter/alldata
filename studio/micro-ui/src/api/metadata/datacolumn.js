import request from '@/utils/request'

export function listDataColumn(data) {
  return request({
    url: '/data/metadata/columns/list',
    method: 'get',
    params: data
  })
}

export function pageDataColumn(data) {
  return request({
    url: '/data/metadata/columns/page',
    method: 'get',
    params: data
  })
}

export function getDataColumn(id) {
  return request({
    url: '/data/metadata/columns/' + id,
    method: 'get'
  })
}

export function delDataColumn(id) {
  return request({
    url: '/data/metadata/columns/' + id,
    method: 'delete'
  })
}

export function delDataColumns(ids) {
  return request({
    url: '/data/metadata/columns/batch/' + ids,
    method: 'delete'
  })
}

export function addDataColumn(data) {
  return request({
    url: '/data/metadata/columns',
    method: 'post',
    data: data
  })
}

export function updateDataColumn(data) {
  return request({
    url: '/data/metadata/columns/' + data.id,
    method: 'put',
    data: data
  })
}

export function getDataMetadataTree(level, data) {
  return request({
    url: '/data/metadata/columns/tree/' + level,
    method: 'get',
    params: data
  })
}
