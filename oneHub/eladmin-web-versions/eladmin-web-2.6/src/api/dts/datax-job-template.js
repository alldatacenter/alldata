import request from '@/utils/request'

// datax插件api

export function getList(params) {
  return request({
    url: 'api/jobTemplate/pageList',
    method: 'get',
    params
  })
}

export function getExecutorList() {
  return request({
    url: 'api/jobGroup/list',
    method: 'get'
  })
}

export function updateJob(data) {
  return request({
    url: '/api/jobTemplate/update',
    method: 'post',
    data
  })
}

export function createJob(data) {
  return request({
    url: '/api/jobTemplate/add/',
    method: 'post',
    data
  })
}

export function removeJob(id) {
  return request({
    url: '/api/jobTemplate/remove/' + id,
    method: 'post'
  })
}

export function nextTriggerTime(cron) {
  return request({
    url: '/api/jobTemplate/nextTriggerTime?cron=' + cron,
    method: 'get'
  })
}

export function getUsersList(params) {
  return request({
    url: 'api/user/list',
    method: 'get',
    params
  })
}
