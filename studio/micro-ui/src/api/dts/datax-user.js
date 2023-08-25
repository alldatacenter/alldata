import request from '@/utils/request'

// datax用户api

export function getList(params) {
  return request({
    url: '/data/dts/user/pageList',
    method: 'get',
    params
  })
}

export function updateUser(data) {
  return request({
    url: '/data/dts/user/update',
    method: 'post',
    data
  })
}

export function createUser(data) {
  return request({
    url: '/data/dts/user/add',
    method: 'post',
    data
  })
}

export function deleteUser(id) {
  return request({
    url: '/data/dts/user/remove?id=' + id,
    method: 'post'
  })
}
