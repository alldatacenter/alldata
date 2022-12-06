import request from '@/utils/request'

// datax插件api

export function list(params) {
  return request({
    url: '/api/jobJdbcDatasource',
    method: 'get',
    params
  })
}

export function fetched(params) {
  return request({
    url: '/api/jobJdbcDatasource/' + params,
    method: 'get'
  })
}

export function updated(data) {
  return request({
    url: '/api/jobJdbcDatasource',
    method: 'put',
    data
  })
}

export function created(data) {
  return request({
    url: '/api/jobJdbcDatasource',
    method: 'post',
    data
  })
}

export function deleted(data) {
  return request({
    url: '/api/jobJdbcDatasource',
    method: 'delete',
    params: data
  })
}

export function test(data) {
  return request({
    url: '/api/jobJdbcDatasource/test',
    method: 'post',
    data
  })
}

export function getDataSourceList(params) {
  return request({
    url: '/api/jobJdbcDatasource/all',
    method: 'get',
    params
  })
}
