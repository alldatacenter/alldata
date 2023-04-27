import request from '@/utils/request'
export function pageDataModel(data) {
  return request({
    url: '/data/compare/dcDbConfig/list',
    method: 'post',
    params: data
  })
}

export function delDataModel(id) {
  const usersName = { ids: id }
  return request({
    url: '/data/compare/dcDbConfig/remove',
    method: 'post',
    params: usersName
  })
}

export function addDataModel(data) {
  return request({
    url: '/data/compare/dcDbConfig/add',
    method: 'post',
    params: data
  })
}

export function updateDataModel(data) {
  // const usersName = { id: data.id }
  return request({
    url: '/data/compare/dcDbConfig/edit',
    method: 'post',
    params: data

  })
}

