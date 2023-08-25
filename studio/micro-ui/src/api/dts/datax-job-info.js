import request from '@/utils/request'

// datax插件api

export function getList(params) {
  return request({
    url: '/data/dts/job/pageList',
    method: 'get',
    params
  })
}

export function triggerJob(data) {
  return request({
    url: '/data/dts/job/trigger',
    method: 'post',
    data
  })
}

export function startJob(id) {
  return request({
    url: '/data/dts/job/start?id=' + id,
    method: 'post'
  })
}

export function stopJob(id) {
  return request({
    url: '/data/dts/job/stop?id=' + id,
    method: 'post'
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
    url: '/data/dts/job/update',
    method: 'post',
    data
  })
}

export function createJob(data) {
  return request({
    url: '/data/dts/job/add/',
    method: 'post',
    data
  })
}

export function removeJob(id) {
  return request({
    url: '/data/dts/job/remove/' + id,
    method: 'post'
  })
}

export function nextTriggerTime(cron) {
  return request({
    url: '/data/dts/job/nextTriggerTime?cron=' + cron,
    method: 'get'
  })
}
export function viewJobLog(id) {
  return request({
    url: '/data/dts/log/logDetailCat?id=' + id,
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

export function getJobIdList(params) {
  return request({
    url: '/data/dts/job/list',
    method: 'get',
    params
  })
}
// batchAdd
export function batchAddJob(data) {
  return request({
    url: '/data/dts/job/batchAdd',
    method: 'post',
    data
  })
}

