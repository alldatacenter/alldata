import request from '@/utils/request'

export function pageChangeRecord (data) {
  return request({
    url: '/data/metadata/changeRecords/page',
    method: 'get',
    params: data
  })
}

export function getChangeRecord (id) {
  return request({
    url: '/data/metadata/changeRecords/' + id,
    method: 'get'
  })
}

export function delChangeRecord (id) {
  return request({
    url: '/data/metadata/changeRecords/' + id,
    method: 'delete'
  })
}

export function delChangeRecords (ids) {
  return request({
    url: '/data/metadata/changeRecords/batch/' + ids,
    method: 'delete'
  })
}

export function addChangeRecord (data) {
  return request({
    url: '/data/metadata/changeRecords',
    method: 'post',
    data: data
  })
}

export function updateChangeRecord (data) {
  return request({
    url: '/data/metadata/changeRecords/' + data.id,
    method: 'put',
    data: data
  })
}
