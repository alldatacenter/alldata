import request from '@/utils/request'

export function searchUser(name) {
  return request({
    url: '/dts/search/user',
    method: 'get',
    params: { name }
  })
}

export function transactionList(query) {
  return request({
    url: '/dts/transaction/list',
    method: 'get',
    params: query
  })
}
