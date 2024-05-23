import request from '@/utils/request'
import qs from 'qs'

export function login(username, password) {
  const data = {
    username: username,
    password: password,
    grant_type: 'password',
    scope: 'all'
  }
  return request({
    url: '/auth/oauth/token',
    method: 'post',
    headers: {
      'Authorization': 'Basic ZGF0YXg6MTIzNDU2'
    },
    data: qs.stringify(data)
  })
}

export function getRoutes() {
  return request({
    url: '/data/system/users/route',
    method: 'get'
  })
}

export function logout(token) {
  return request({
    url: `/system/logout/${token}`,
    method: 'delete'
  })
}

export function log() {
  return request({
    url: '/data/system/login/logs',
    method: 'post'
  })
}

export function pageUser(data) {
  return request({
    url: '/data/system/users/page',
    method: 'get',
    params: data
  })
}

export function getUser(id) {
  return request({
    url: '/data/system/users/' + id,
    method: 'get'
  })
}

export function delUser(id) {
  return request({
    url: '/data/system/users/' + id,
    method: 'delete'
  })
}

export function delUsers(ids) {
  return request({
    url: '/data/system/users/batch/' + ids,
    method: 'delete'
  })
}

export function addUser(data) {
  return request({
    url: '/data/system/users',
    method: 'post',
    data: data
  })
}

export function updateUser(data) {
  return request({
    url: '/data/system/users/' + data.id,
    method: 'put',
    data: data
  })
}

export function updatePassword(data) {
  return request({
    url: '/data/system/users/password',
    method: 'put',
    data: data
  })
}

export function resetPassword(data) {
  return request({
    url: '/data/system/users/reset/password',
    method: 'put',
    data: data
  })
}

export function getAuditUsers() {
  return request({
    url: '/data/system/users/audit',
    method: 'get'
  })
}
