import request, {Method} from '@/plugins/request.js'

// 获取首页楼层装修数据
export function indexData (params) {
  return request({
    url: '/buyer/other/pageData/getIndex',
    method: Method.GET,
    needToken: false,
    params
  })
}

/**
 * 楼层装修数据
 * @param pageClientType 客户端类型,可用值:PC,H5,WECHAT_MP,APP
 * @param pageType 页面类型,可用值:INDEX,STORE,SPECIAL
 */
export function pageData (params) {
  return request({
    url: `/buyer/other/pageData`,
    method: Method.GET,
    needToken: false,
    params
  })
}
/**
 * 刷新token
 */
export function handleRefreshToken (token) {
  return request({
    url: `/buyer/passport/member/refresh/${token}`,
    method: Method.GET,
    needToken: false
  })
}
