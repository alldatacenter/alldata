import request from '@/utils/request'

export function refreshBusiness() {
  return request({
    url: '/workflow/business/refresh',
    method: 'get'
  })
}

export function pageBusiness(data) {
  return request({
    url: '/workflow/business/page',
    method: 'get',
    params: data
  })
}

export function getBusiness(id) {
  return request({
    url: '/workflow/business/' + id,
    method: 'get'
  })
}

export function delBusiness(id) {
  return request({
    url: '/workflow/business/' + id,
    method: 'delete'
  })
}

export function addBusiness(data) {
  return request({
    url: '/workflow/business',
    method: 'post',
    data: data
  })
}

export function updateBusiness(data) {
  return request({
    url: '/workflow/business/' + data.id,
    method: 'put',
    data: data
  })
}
