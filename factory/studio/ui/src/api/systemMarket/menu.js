import request from '@/utils/request'

export function listMenu (data) {
  return request({
    url: '/data/system/menus/list',
    method: 'get',
    params: data
  })
}

export function listMenuForFlow (data) {
  return request({
    url: '/data/system/menus/list/flow',
    method: 'get',
    params: data
  })
}

export function getMenu (id) {
  return request({
    url: '/data/system/menus/' + id,
    method: 'get'
  })
}

export function delMenu (id) {
  return request({
    url: '/data/system/menus/' + id,
    method: 'delete'
  })
}

export function delMenus (ids) {
  return request({
    url: '/data/system/menus/batch/' + ids,
    method: 'delete'
  })
}

export function addMenu (data) {
  return request({
    url: '/data/system/menus',
    method: 'post',
    data: data
  })
}

export function updateMenu (data) {
  return request({
    url: '/data/system/menus/' + data.id,
    method: 'put',
    data: data
  })
}
