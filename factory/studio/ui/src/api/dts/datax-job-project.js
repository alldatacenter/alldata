import request from '@/utils/request'

// project

export function list(params) {
  return request({
    url: '/data/dts/jobProject',
    method: 'get',
    params
  })
}

export function updated(data) {
  return request({
    url: '/data/dts/jobProject',
    method: 'put',
    data
  })
}

export function created(data) {
  return request({
    url: '/data/dts/jobProject',
    method: 'post',
    data
  })
}

export function deleted(data) {
  return request({
    url: '/data/dts/jobProject',
    method: 'delete',
    params: data
  })
}

export function getJobProjectList(params) {
  return request({
    url: '/data/dts/jobProject/list',
    method: 'get',
    params
  })
}

