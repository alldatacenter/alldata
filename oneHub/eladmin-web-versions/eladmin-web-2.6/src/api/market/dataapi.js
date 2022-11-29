import request from '@/utils/request'

export function listDataApi(data) {
  return request({
    url: '/data/market/dataApis/list',
    method: 'get',
    params: data
  })
}

export function pageDataApi(data) {
  return request({
    url: '/data/market/dataApis/page',
    method: 'get',
    params: data
  })
}

export function getDataApi(id) {
  return request({
    url: '/data/market/dataApis/' + id,
    method: 'get'
  })
}

export function delDataApi(id) {
  return request({
    url: '/data/market/dataApis/' + id,
    method: 'delete'
  })
}

export function delDataApis(ids) {
  return request({
    url: '/data/market/dataApis/batch/' + ids,
    method: 'delete'
  })
}

export function addDataApi(data) {
  return request({
    url: '/data/market/dataApis',
    method: 'post',
    data: data
  })
}

export function updateDataApi(data) {
  return request({
    url: '/data/market/dataApis/' + data.id,
    method: 'put',
    data: data
  })
}

export function sqlParse(data) {
  return request({
    url: '/data/market/dataApis/sql/parse',
    method: 'post',
    data: data
  })
}

export function copyDataApi(id) {
  return request({
    url: '/data/market/dataApis/' + id + '/copy',
    method: 'post'
  })
}

export function releaseDataApi(id) {
  return request({
    url: '/data/market/dataApis/' + id + '/release',
    method: 'post'
  })
}

export function cancelDataApi(id) {
  return request({
    url: '/data/market/dataApis/' + id + '/cancel',
    method: 'post'
  })
}

export function word(id) {
  return request({
    url: '/data/market/dataApis/word/' + id,
    method: 'post',
    responseType: 'blob'
  })
}

export function getDataApiDetail(id) {
  return request({
    url: '/data/market/dataApis/detail/' + id,
    method: 'get'
  })
}
