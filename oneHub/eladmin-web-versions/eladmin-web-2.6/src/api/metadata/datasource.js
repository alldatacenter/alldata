import request from '@/utils/request'

// 刷新参数缓存
export function refreshMetadata () {
  return request({
    url: '/data/metadata/sources/refresh',
    method: 'get'
  })
}

export function listDataSource (data) {
  return request({
    url: '/data/metadata/sources/list',
    method: 'get',
    params: data
  })
}

export function pageDataSource (data) {
  return request({
    url: '/data/metadata/sources/page',
    method: 'get',
    params: data
  })
}

export function getDataSource (id) {
  return request({
    url: '/data/metadata/sources/' + id,
    method: 'get'
  })
}

export function delDataSource (id) {
  return request({
    url: '/data/metadata/sources/' + id,
    method: 'delete'
  })
}

export function delDataSources (ids) {
  return request({
    url: '/data/metadata/sources/batch/' + ids,
    method: 'delete'
  })
}

export function addDataSource (data) {
  return request({
    url: '/data/metadata/sources',
    method: 'post',
    data: data
  })
}

export function updateDataSource (data) {
  return request({
    url: '/data/metadata/sources/' + data.id,
    method: 'put',
    data: data
  })
}

export function checkConnection (data) {
  return request({
    url: '/data/metadata/sources/checkConnection',
    method: 'post',
    data: data
  })
}

export function queryByPage (data) {
  return request({
    url: '/data/metadata/sources/queryByPage',
    method: 'post',
    data: data
  })
}

export function getDbTables (id) {
  return request({
    url: '/data/metadata/sources/' + id + '/tables',
    method: 'get'
  })
}

export function getDbTableColumns (id, tableName) {
  return request({
    url: '/data/metadata/sources/' + id + '/' + tableName + '/columns',
    method: 'get'
  })
}

export function sync (id) {
  return request({
    url: '/data/metadata/sources/sync/' + id,
    method: 'post'
  })
}

export function word (id) {
  return request({
    url: '/data/metadata/sources/word/' + id,
    method: 'post',
    responseType: 'blob'
  })
}
