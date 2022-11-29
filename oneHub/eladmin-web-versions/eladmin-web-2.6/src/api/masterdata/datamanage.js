import request from '@/utils/request'

export function pageData(data) {
  return request({
    url: '/data/masterdata/datas/page',
    method: 'post',
    data: data
  })
}

export function getData(data) {
  return request({
    url: '/data/masterdata/datas/' + data.id,
    method: 'get',
    params: data
  })
}

export function addData(data) {
  return request({
    url: '/data/masterdata/datas/addData',
    method: 'post',
    data: data
  })
}

export function updateData(data) {
  return request({
    url: '/data/masterdata/datas/updateData/' + data.id,
    method: 'put',
    data: data
  })
}

export function delData(data) {
  return request({
    url: '/data/masterdata/datas/delData/' + data.id,
    method: 'post',
    data: data
  })
}
