import request from '@/utils/request'

// datax 执行器注册信息

export function getList(params) {
  return request({
    url: '/system/api/jobRegistry',
    method: 'get',
    params
  })
}
