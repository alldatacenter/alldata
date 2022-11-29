import request from '@/utils/request'

export function listJob (data) {
  return request({
    url: '/quartz/jobs/list',
    method: 'get',
    params: data
  })
}

export function pageJob (data) {
  return request({
    url: '/quartz/jobs/page',
    method: 'get',
    params: data
  })
}

export function getJob (id) {
  return request({
    url: '/quartz/jobs/' + id,
    method: 'get'
  })
}

export function delJob (id) {
  return request({
    url: '/quartz/jobs/' + id,
    method: 'delete'
  })
}

export function addJob (data) {
  return request({
    url: '/quartz/jobs',
    method: 'post',
    data: data
  })
}

export function updateJob (data) {
  return request({
    url: '/quartz/jobs/' + data.id,
    method: 'put',
    data: data
  })
}

export function pauseJob (id) {
  return request({
    url: '/quartz/jobs/pause/' + id,
    method: 'post'
  })
}

export function resumeJob (id) {
  return request({
    url: '/quartz/jobs/resume/' + id,
    method: 'post'
  })
}

export function runJob (id) {
  return request({
    url: '/quartz/jobs/run/' + id,
    method: 'post'
  })
}
