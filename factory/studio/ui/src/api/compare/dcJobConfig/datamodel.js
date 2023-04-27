import request from '@/utils/request'

// 查询
export function pageDataModel(data) {
  return request({
    url: '/data/compare/dcJobConfig/list',
    method: 'post',
    params: data
  })
}

// 删除
export function delDataModel(id) {
  const usersName = { ids: id }
  return request({
    url: '/data/compare/dcJobConfig/remove',
    method: 'post',
    params: usersName
  })
}

// 添加
export function addDataModel(data) {
  return request({
    url: '/data/compare/dcJobConfig/add',
    method: 'post',
    params: data
  })
}

// 修改
export function updateDataModel(data) {
  // const usersName = { id: data.id }
  return request({
    url: '/data/compare/dcJobConfig/edit',
    method: 'post',
    params: data

  })
}
// 获取数据配置
export function getDbConfig() {
  return request({
    url: '/data/compare/dcDbConfig/list',
    method: 'post'
  })
}
// 运行实例
export function dcJobConfigRun(data) {
  const id = { ids: data }
  return request({
    url: '/data/compare/dcJobConfig/run',
    method: 'post',
    params: id
  })
}
