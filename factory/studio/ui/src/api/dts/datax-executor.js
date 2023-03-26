import request from '@/utils/request'

// 执行器管理

export function getList() {
  return request({
    url: '/data/dts/jobGroup/list',
    method: 'get'
  })
}

export function updated(data) {
  return request({
    url: '/data/dts/jobGroup/update',
    method: 'post',
    data
  })
}

export function created(data) {
  return request({
    url: '/data/dts/jobGroup/save',
    method: 'post',
    data
  })
}

export function loadById(id) {
  return request({
    url: '/data/dts/jobGroup/loadById?id=' + id,
    method: 'post'
  })
}

export function deleted(id) {
  return request({
    url: '/data/dts/jobGroup/remove?id=' + id,
    method: 'post'
  })
}

