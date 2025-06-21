import request from '@/utils/request'

export function listDataModel(data) {
  return request({
    url: '/data/masterdata/models/list',
    method: 'get',
    params: data
  })
}

export function pageDataModel(data) {
  return request({
    url: '/data/masterdata/models/page',
    method: 'get',
    params: data
  })
}

export function getDataModel(id) {
  return request({
    url: '/data/masterdata/models/' + id,
    method: 'get'
  })
}

export function delDataModel(id) {
  return request({
    url: '/data/masterdata/models/' + id,
    method: 'delete'
  })
}

export function delDataModels(ids) {
  return request({
    url: '/data/masterdata/models/batch/' + ids,
    method: 'delete'
  })
}

export function addDataModel(data) {
  return request({
    url: '/data/masterdata/models',
    method: 'post',
    data: data
  })
}

export function updateDataModel(data) {
  return request({
    url: '/data/masterdata/models/' + data.id,
    method: 'put',
    data: data
  })
}

export function submitDataModel(id) {
  return request({
    url: '/data/masterdata/models/submit/' + id,
    method: 'post'
  })
}

export function createTable(id) {
  return request({
    url: '/data/masterdata/models/table/create/' + id,
    method: 'post'
  })
}

export function dropTable(id) {
  return request({
    url: '/data/masterdata/models/table/drop/' + id,
    method: 'delete'
  })
}

export function getTableParam(id) {
  return request({
    url: '/data/masterdata/models/table/param/' + id,
    method: 'get'
  })
}

export function getFormParam(id) {
  return request({
    url: '/data/masterdata/models/form/param/' + id,
    method: 'get'
  })
}
