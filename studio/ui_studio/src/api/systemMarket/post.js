import request from '@/utils/request'

export function pagePost (data) {
  return request({
    url: '/data/system/posts/page',
    method: 'get',
    params: data
  })
}

export function listPost (data) {
  return request({
    url: '/data/system/posts/list',
    method: 'get',
    params: data
  })
}

export function getPost (id) {
  return request({
    url: '/data/system/posts/' + id,
    method: 'get'
  })
}

export function delPost (id) {
  return request({
    url: '/data/system/posts/' + id,
    method: 'delete'
  })
}

export function delPosts (ids) {
  return request({
    url: '/data/system/posts/batch/' + ids,
    method: 'delete'
  })
}

export function addPost (data) {
  return request({
    url: '/data/system/posts',
    method: 'post',
    data: data
  })
}

export function updatePost (data) {
  return request({
    url: '/data/system/posts/' + data.id,
    method: 'put',
    data: data
  })
}
