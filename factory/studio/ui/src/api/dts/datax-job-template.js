import request from '@/utils/request'

// datax插件api

export function getList(params) {
  return request({
    url: '/data/dts/jobTemplate/pageList',
    method: 'get',
    params
  })
}

export function getExecutorList() {
  return request({
    url: '/data/dts/jobGroup/list',
    method: 'get'
  })
}

export function updateJob(data) {
  return request({
    url: '/data/dts/jobTemplate/update',
    method: 'post',
    data
  })
}

export function createJob(data) {
  return request({
    url: '/data/dts/jobTemplate/add/',
    method: 'post',
    data
  })
}

export function removeJob(id) {
  return request({
    url: '/data/dts/jobTemplate/remove/' + id,
    method: 'post'
  })
}

export function nextTriggerTime(cron) {
  return request({
    url: '/data/dts/jobTemplate/nextTriggerTime?cron=' + cron,
    method: 'get'
  })
}

export function getUsersList(params) {
  return request({
    url: '/data/dts/user/list',
    method: 'get',
    params
  })
}
