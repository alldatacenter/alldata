import request, {
  Method, commonUrl
} from '@/plugins/request.js';

// 会员收货地址列表
export function memberAddress () {
  return request({
    url: '/buyer/member/address',
    needToken: true,
    method: Method.GET
  });
}

// 添加收货地址
export function newMemberAddress (params) {
  return request({
    url: '/buyer/member/address',
    needToken: true,
    method: Method.POST,
    data: params
  });
}

// 编辑收货地址
export function editMemberAddress (params) {
  return request({
    url: '/buyer/member/address',
    needToken: true,
    method: Method.PUT,
    params
  });
}

// 删除收货地址
export function delMemberAddress (id) {
  return request({
    url: `/buyer/member/address/delById/${id}`,
    needToken: true,
    method: Method.DELETE
  });
}

// 根据id获取会员地址详情
export function getAddrDetail (id) {
  return request({
    url: `/buyer/member/address/get/${id}`,
    needToken: true,
    method: Method.GET
  });
}

// 传给后台citycode 获取城市街道等id
export function handleRegion (params) {
  return request({
    url: `${commonUrl}/common/common/region/region`,
    needToken: true,
    method: Method.GET,
    params
  });
}
