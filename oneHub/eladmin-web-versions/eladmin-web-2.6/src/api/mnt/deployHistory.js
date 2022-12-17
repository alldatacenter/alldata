import request from '@/utils/request'

export function del(ids) {
  return request({
    url: '/system/api/deployHistory',
    method: 'delete',
    data: ids
  })
}

/**
 * 版本回退
 * @param data 选中行
 */
export function reducte(data) {
  return request({
    url: '/system/api/deploy/serverReduction',
    method: 'post',
    data
  })
}
