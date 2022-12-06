import request from '@/utils/request'

// datax用户api

export function getList(params) {
  return request({
    url: '/api/user/pageList',
    method: 'get',
    params
  })
}

export function updateUser(data) {
  return request({
    url: '/api/user/update',
    method: 'post',
    data
  })
}

export function createUser(data) {
  return request({
    url: '/api/user/add',
    method: 'post',
    data
  })
}

export function deleteUser(id) {
  return request({
    url: '/api/user/remove?id=' + id,
    method: 'post'
  })
}
