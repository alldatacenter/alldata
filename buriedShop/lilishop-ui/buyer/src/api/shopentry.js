import request, {
  Method
} from '@/plugins/request.js'

// 店铺分页列表
export function shopList (params) {
  return request({
    url: '/buyer/store/store',
    needToken: true,
    method: Method.GET,
    params
  })
}

// 申请店铺第一步-填写企业信息
export function applyFirst (params) {
  return request({
    url: '/buyer/store/store/apply/first',
    needToken: true,
    method: Method.PUT,
    params
  })
}

// 申请店铺第一步-填写企业信息
export function applySecond (params) {
  return request({
    url: '/buyer/store/store/apply/second',
    needToken: true,
    method: Method.PUT,
    params
  })
}

// 申请店铺第一步-填写企业信息
export function applyThird (params) {
  return request({
    url: '/buyer/store/store/apply/third',
    needToken: true,
    method: Method.PUT,
    params
  })
}

// 店铺详情
export function getDetailById (id) {
  return request({
    url: `/buyer/store/store/get/detail/${id}`,
    needToken: true,
    method: Method.GET
  })
}
// 店铺分类
export function getCateById (id) {
  return request({
    url: `/buyer/store/store/label/get/${id}`,
    needToken: true,
    method: Method.GET
  })
}
//  店铺入驻协议
export function agreement () {
  return request({
    url: `/buyer/other/article/type/STORE_REGISTER`,
    needToken: true,
    method: Method.GET
  })
}

//  获取当前登录会员的店铺信息
export function applyStatus () {
  return request({
    url: `/buyer/store/store/apply`,
    needToken: true,
    method: Method.GET
  })
}
