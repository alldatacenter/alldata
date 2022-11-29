import request from '@/utils/request'

export function pageRunningInstance(data) {
  return request({
    url: '/workflow/instances/pageRunning',
    method: 'get',
    params: data
  })
}

export function pageMyStartedInstance(data) {
  return request({
    url: '/workflow/instances/pageMyStarted',
    method: 'get',
    params: data
  })
}

export function pageMyInvolvedInstance(data) {
  return request({
    url: '/workflow/instances/pageMyInvolved',
    method: 'get',
    params: data
  })
}

export function delInstance(processInstanceId) {
  return request({
    url: '/workflow/instances/delete/' + processInstanceId,
    method: 'delete'
  })
}

export function activateInstance(processInstanceId) {
  return request({
    url: '/workflow/instances/activate/' + processInstanceId,
    method: 'put'
  })
}

export function suspendInstance(processInstanceId) {
  return request({
    url: '/workflow/instances/suspend/' + processInstanceId,
    method: 'put'
  })
}

export function flowTrack(processInstanceId) {
  return request({
    url: '/workflow/instances/track',
    method: 'get',
    responseType: 'blob',
    params: { processInstanceId: processInstanceId }
  })
}
