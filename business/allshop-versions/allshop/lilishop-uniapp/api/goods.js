/**
 * 商品相关API
 */

import { http, Method } from "@/utils/request.js";

/**
 * 从ES中获取相关商品品牌名称，分类名称及属性
 */
 export function getGoodsMessage(goodsId) {
  return http.request({
    url: `/goods/goods/get/${goodsId}`,
    method: Method.GET,
  });
}

/**
 * 从ES中获取相关商品品牌名称，分类名称及属性
 */
export function getGoodsRelated(params) {
  return http.request({
    url: `/goods/goods/es/related`,
    method: Method.GET,
    params,
  });
}

/**
 * 获取商品详情
 * @param skuId 商品ID
 * @returns {AxiosPromise}
 */
 export function getGoods(skuId, goodsId) {
  return http.request({
    url: `/goods/goods/sku/${goodsId}/${skuId}`,
    method: Method.GET,
  });
}

/**
 * 获取商品分销
 * @param distributionId 商品分销ID
 */
 export function getGoodsDistribution(distributionId) {
  return http.request({
    url: `/distribution/distribution/bindingDistribution/${distributionId}`,
    method: Method.GET,
  });
}


/**
 * 获取商品列表
 * @param params
 * @returns {AxiosPromise}
 */
export function getGoodsList(params) {
  return http.request({
    url: "/goods/goods/es",
    method: Method.GET,
    params,
  });
}

/**
 * 获取上新商品列表
 * @param params
 * @returns {AxiosPromise}
 */
export function getGoodsListUplog(params) {
  return http.request({
    url: "goods/search/uplog",
    method: Method.GET,
    params,
  });
}



/**
 * 获取标签商品
 * @param storeId 卖家id
 * @param mark      标签 hot：热卖 new：新品 recommend：推荐
 * @param num       获取个数
 */
export function getTagGoods(storeId, mark = "hot", num = 5) {
  return http.request({
    url: `goods/tags/${mark}/goods`,
    method: Method.GET,
    loading: false,
    params: {
      storeId,
      mark,
      num,
    },
  });
}
/**
 * 获取标签商品
 */
export function getPlateformTagGoods(tag_id) {
  return http.request({
    url: `goods/tags/byid/${tag_id}`,
    method: Method.GET,
    loading: false,
  });
}

/**
 * 获取首页商品分类 左侧列表
 * @param parent_id
 */
export function getCategoryList(id) {
  return http.request({
    url: `/goods/category/get/${id}`,
    method: Method.GET,
    loading: false,
  });
}




/**
 * 获取当前会员的分销商信息 可根据分销商信息查询待提现金额以及冻结金额等信息
 */
export function distribution() {
  return http.request({
    url: `/distribution/distribution`,
    method: Method.GET,
  });
}

/**
 * 申请分销商
 */
export function applyDistribution(params) {
  return http.request({
    url: `/distribution/distribution`,
    method: Method.POST,
    params,
  });
}

/**
 * 分销商提现
 */
export function cash(params) {
  return http.request({
    url: `/distribution/cash`,
    method: Method.POST,
    params,
  });
}

/**
 * 分销商提现历史
 */
export function cashLog(params) {
  return http.request({
    url: `/distribution/cash`,
    method: Method.GET,
    params

  });
}

/**
 * 获取分销商分页订单列表
 */
export function distributionOrderList(params) {
  return http.request({
    url: `/distribution/distribution/distributionOrder`,
    method: Method.GET,
    params
  });
}

/**
 * 获取分销商商品列表
 */
export function distributionGoods(params) {
  return http.request({
    url: `/distribution/goods`,
    method: Method.GET,
    params,
  });
}
/**
 * 选择分销商品 分销商品id
 */
export function checkedDistributionGoods(params) {
  return http.request({
    url: `/distribution/goods/checked/${params.id}`,
    method: Method.GET,
    params
  });
}

/**
 * 获取 小程序码
 */
 export function getMpCode(params){
  return http.request({
    url:`/passport/connect/miniProgram/mp/unlimited`,
    method:Method.GET,
    params
  })
}

/**
 * 根据shortlink 获取页面参数
 */
 export function getMpScene(id){
  return http.request({
    url:`/passport/connect/miniProgram/mp/unlimited/scene?id=${id}`,
    method:Method.GET,

  })
}
