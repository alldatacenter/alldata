import request from '@/utils/request'

// datax插件api
export function getList(params) {
  return request({
    url: '/data/dts/log/pageList',
    method: 'get',
    params
  })
}

export function clearLog(jobGroup, jobId, type) {
  return request({
    url: '/data/dts/log/clearLog?jobGroup=' + jobGroup + '&jobId=' + jobId + '&type=' + type,
    method: 'post'
  })
}

export function killJob(data) {
  return request({
    url: '/data/dts/log/killJob',
    method: 'post',
    data
  })
}

export function viewJobLog(executorAddress, triggerTime, logId, fromLineNum) {
  return request({
    url: '/data/dts/log/logDetailCat?executorAddress=' + executorAddress + '&triggerTime=' + triggerTime + '&logId=' + logId + '&fromLineNum=' + fromLineNum,
    method: 'get'
  })
}
