import request from '@/utils/request'

export function pageCheckJob(data) {
  return request({
    url: '/data/quality/scheduleJobs/page',
    method: 'get',
    params: data
  })
}

export function pauseCheckJob(id) {
  return request({
    url: '/data/quality/scheduleJobs/pause/' + id,
    method: 'post'
  })
}

export function resumeCheckJob(id) {
  return request({
    url: '/data/quality/scheduleJobs/resume/' + id,
    method: 'post'
  })
}

/* zrx add */
export function runCheckJob(id) {
  return request({
    url: '/data/quality/scheduleJobs/run/' + id,
    method: 'post'
  })
}
