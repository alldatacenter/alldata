import request from '@/utils/request'

export function pageDataService(data) {
  return request({
    url: '/data/service/services/page',
    method: 'get',
    params: data
  })
}

export function getDataService(id) {
  return request({
    url: `/data/service/services/${id}`,
    method: 'get'
  })
}

export function delDataService(id) {
  return request({
    url: `/data/service/services/${id}`,
    method: 'delete'
  })
}

export function delDataServices(ids) {
  return request({
    url: `/data/service/services/batch/${ids}`,
    method: 'delete'
  })
}

export function addDataService(data) {
  return request({
    url: '/data/service/services',
    method: 'post',
    data: data
  })
}

export function updateDataService(data) {
  return request({
    url: `/data/service/services/${data.id}`,
    method: 'put',
    data: data
  })
}

export function getDataServiceDetail(id) {
  return request({
    url: `/data/service/services/detail/${id}`,
    method: 'get'
  })
}
