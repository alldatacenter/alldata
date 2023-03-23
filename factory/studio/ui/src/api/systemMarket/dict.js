import request from '@/utils/request'

// 查询字典数据详细
export function getDicts (dictCode) {
  return request({
    url: '/data/system/dicts/code/' + dictCode,
    method: 'get'
  })
}

// 刷新字典缓存
export function refreshDict () {
  return request({
    url: '/data/system/dicts/refresh',
    method: 'get'
  })
}

export function pageDict (data) {
  return request({
    url: '/data/system/dicts/page',
    method: 'get',
    params: data
  })
}

export function getDict (id) {
  return request({
    url: '/data/system/dicts/' + id,
    method: 'get'
  })
}

export function delDict (id) {
  return request({
    url: '/data/system/dicts/' + id,
    method: 'delete'
  })
}

export function delDicts (ids) {
  return request({
    url: '/data/system/dicts/batch/' + ids,
    method: 'delete'
  })
}

export function addDict (data) {
  return request({
    url: '/data/system/dicts',
    method: 'post',
    data: data
  })
}

export function updateDict (data) {
  return request({
    url: '/data/system/dicts/' + data.id,
    method: 'put',
    data: data
  })
}

export function pageDictItem (data) {
  return request({
    url: '/data/system/dict/items/page',
    method: 'get',
    params: data
  })
}

export function getDictItem (id) {
  return request({
    url: '/data/system/dict/items/' + id,
    method: 'get'
  })
}

export function delDictItem (id) {
  return request({
    url: '/data/system/dict/items/' + id,
    method: 'delete'
  })
}

export function delDictItems (ids) {
  return request({
    url: '/data/system/dict/items/batch/' + ids,
    method: 'delete'
  })
}

export function addDictItem (data) {
  return request({
    url: '/data/system/dict/items',
    method: 'post',
    data: data
  })
}

export function updateDictItem (data) {
  return request({
    url: '/data/system/dict/items/' + data.id,
    method: 'put',
    data: data
  })
}
