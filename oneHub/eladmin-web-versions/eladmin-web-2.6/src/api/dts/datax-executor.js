import request from '@/utils/request'

// 执行器管理

export function getList() {
  return request({
    url: '/api/jobGroup/list',
    method: 'get'
  })
}

export function updated(data) {
  return request({
    url: '/api/jobGroup/update',
    method: 'post',
    data
  })
}

export function created(data) {
  return request({
    url: '/api/jobGroup/save',
    method: 'post',
    data
  })
}

export function loadById(id) {
  return request({
    url: '/api/jobGroup/loadById?id=' + id,
    method: 'post'
  })
}

export function deleted(id) {
  return request({
    url: '/api/jobGroup/remove?id=' + id,
    method: 'post'
  })
}

