import request from '@/utils/request'

export function listCategory(data) {
  return request({
    url: '/workflow/categorys/list',
    method: 'get',
    params: data
  })
}

export function addCategory(data) {
  return request({
    url: '/workflow/categorys',
    method: 'post',
    data: data
  })
}

export function updateCategory(data) {
  return request({
    url: '/workflow/categorys/' + data.id,
    method: 'put',
    data: data
  })
}

export function delCategory(id) {
  return request({
    url: '/workflow/categorys/' + id,
    method: 'delete'
  })
}

export function pageDefinition(data) {
  return request({
    url: '/workflow/definitions/page',
    method: 'get',
    params: data
  })
}

export function delDefinition(deploymentId) {
  return request({
    url: '/workflow/definitions/delete/' + deploymentId,
    method: 'delete'
  })
}

export function activateDefinition(processDefinitionId) {
  return request({
    url: '/workflow/definitions/activate/' + processDefinitionId,
    method: 'put'
  })
}

export function suspendDefinition(processDefinitionId) {
  return request({
    url: '/workflow/definitions/suspend/' + processDefinitionId,
    method: 'put'
  })
}

export function deployDefinition(data) {
  return request({
    url: '/workflow/definitions/import/file',
    method: 'post',
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    data: data
  })
}

export function flowResource(processDefinitionId) {
  return request({
    url: '/workflow/definitions/resource',
    method: 'get',
    responseType: 'blob',
    params: { processDefinitionId: processDefinitionId, resType: 'image' }
  })
}
