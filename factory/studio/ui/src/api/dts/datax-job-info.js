import request from '@/utils/request'

// datax插件api

export function getList(params) {
  return request({
    url: '/dts/api/job/pageList',
    method: 'get',
    params
  })
}

export function triggerJob(data) {
  return request({
    url: '/dts/api/job/trigger',
    method: 'post',
    data
  })
}

export function startJob(id) {
  return request({
    url: '/dts/api/job/start?id=' + id,
    method: 'post'
  })
}

export function stopJob(id) {
  return request({
    url: '/dts/api/job/stop?id=' + id,
    method: 'post'
  })
}

export function getExecutorList() {
  return request({
    url: '/dts/api/jobGroup/list',
    method: 'get'
  })
}

export function updateJob(data) {
  return request({
    url: '/dts/api/job/update',
    method: 'post',
    data
  })
}

export function createJob(data) {
  return request({
    url: '/dts/api/job/add/',
    method: 'post',
    data
  })
}

export function removeJob(id) {
  return request({
    url: '/dts/api/job/remove/' + id,
    method: 'post'
  })
}

export function nextTriggerTime(cron) {
  return request({
    url: '/dts/api/job/nextTriggerTime?cron=' + cron,
    method: 'get'
  })
}
export function viewJobLog(id) {
  return request({
    url: '/dts/api/log/logDetailCat?id=' + id,
    method: 'get'
  })
}

export function getUsersList(params) {
  return request({
    url: '/dts/api/user/list',
    method: 'get',
    params
  })
}

export function getJobIdList(params) {
  return request({
    url: '/dts/api/job/list',
    method: 'get',
    params
  })
}
// batchAdd
export function batchAddJob(data) {
  return request({
    url: '/dts/api/job/batchAdd',
    method: 'post',
    data
  })
}

