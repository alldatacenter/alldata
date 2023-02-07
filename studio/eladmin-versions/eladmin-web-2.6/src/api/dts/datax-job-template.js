import request from '@/utils/request'

// datax插件api

export function getList(params) {
  return request({
    url: '/system/api/jobTemplate/pageList',
    method: 'get',
    params
  })
}

export function getExecutorList() {
  return request({
    url: '/system/api/jobGroup/list',
    method: 'get'
  })
}

export function updateJob(data) {
  return request({
    url: '/system/api/jobTemplate/update',
    method: 'post',
    data
  })
}

export function createJob(data) {
  return request({
    url: '/system/api/jobTemplate/add/',
    method: 'post',
    data
  })
}

export function removeJob(id) {
  return request({
    url: '/system/api/jobTemplate/remove/' + id,
    method: 'post'
  })
}

export function nextTriggerTime(cron) {
  return request({
    url: '/system/api/jobTemplate/nextTriggerTime?cron=' + cron,
    method: 'get'
  })
}

export function getUsersList(params) {
  return request({
    url: '/system/api/user/list',
    method: 'get',
    params
  })
}
