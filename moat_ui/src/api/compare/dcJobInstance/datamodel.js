import request from '@/utils/request'
export function pageDataModel(data) {
  return request({
    url: '/data/compare/dcJobInstance/list',
    method: 'post',
    params: data
  })
}

export function getDiffDetail(data) {
  return request({
    url: '/data/compare/dcJobInstance/getDiffDetail/' + data,
    method: 'get'
  })
}

