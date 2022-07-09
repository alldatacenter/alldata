/**
 * 交♂易相关API
 */

import { http, Method } from "@/utils/request.js";

/**
 * 获取购物车列表
 * @param show_type 要显示的类型 all：全部 checked：已选中的
 */
export function getCarts() {
  return http.request({
    url: `/trade/carts/all`,
    method: Method.GET,
    needToken: true,
    loading: false,
  });
}

/**
 * 获取购物车总数
 * @param show_type 要显示的类型 all：全部 checked：已选中的
 */
export function getCartNum() {
  return http.request({
    url: `/trade/carts/count`,
    method: Method.GET,
    needToken: true,
    loading: false,
  });
}

/**
 * 获取购物车可用优惠券数量
 * @param way 购物车购买：CART/立即购买：BUY_NOW/拼团购买：PINTUAN / 积分购买：POINT
 */
export function getCartCouponNum(way) {
  return http.request({
    url: `/trade/carts/coupon/num?way=${way}`,
    method: Method.GET,
    needToken: true,
    loading: false,
  });
}

/**
 * 添加货品到购物车
 * @param skuId      产品ID
 * @param num         产品的购买数量
 * @param cartType   	购物车类型，默认加入购物车
 */
export function addToCart(data) {
  return http.request({
    url: "/trade/carts",
    method: Method.POST,
    needToken: true,
    header: { "content-type": "application/x-www-form-urlencoded" },
    data,
  });
}



/**
 * 更新购物车商品数量
 * @param skuId
 * @param num
 */
export function updateSkuNum(skuId, num = 1) {
  return http.request({
    url: `/trade/carts/sku/num/${skuId}`,
    method: Method.POST,
    header: { "content-type": "application/x-www-form-urlencoded" },
    needToken: true,
    data: { num },
  });
}

/**
 * 更新购物车货品选中状态
 * @param skuId
 * @param checked
 */
export function updateSkuChecked(skuId, checked) {
  return http.request({
    url: `/trade/carts/sku/checked/${skuId}`,
    method: Method.POST,
    needToken: true,
    header: { "content-type": "application/x-www-form-urlencoded" },
    data: { checked },
  });
}

/**
 * 删除多个货品项
 * @param skuIds
 */
export function deleteSkuItem(skuIds) {
  return http.request({
    url: `/trade/carts/sku/remove?skuIds=${skuIds}`,
    method: Method.DELETE,
    needToken: true,
  });
}


/**
 * 设置全部货品为选中或不选中
 * @param checked
 */
export function checkAll(checked) {
  return http.request({
    url: "/trade/carts/sku/checked",
    method: Method.POST,
    needToken: true,
    params: { checked },
  });
}

/**
 * 设置店铺内全部货品选中状态
 * @param storeId
 * @param checked
 */
export function checkStore(storeId, checked) {
  return http.request({
    url: `/trade/carts/store/${storeId}`,
    method: Method.POST,
    needToken: true,
    header: { "content-type": "application/x-www-form-urlencoded" },
    data: { checked },
  });
}

/**
 * 获取结算参数
 */
export function getCheckoutParams(way) {
  return http.request({
    url: "/trade/carts/checked?way=" + way,
    method: Method.GET,
    needToken: true,
  });
}

/**
 * 设置收货地址ID
 * @param addressId
 */
export function setAddressId(addressId,way) {
  return http.request({
    url: `/trade/carts/shippingAddress?shippingAddressId=${addressId}&way=${way}`,
    method: Method.GET,
    needToken: true,
  
  });
}


/**
 * 创建交易
 */
export function createTrade(params) {
  return http.request({
    url: "/trade/carts/create/trade",
    method: Method.POST,
    needToken: true,
    message: false,
    data:params,
  });
}


/**
 * 根据交易编号或订单编号查询收银台数据
 * @param params
 */
export function getCashierData(params) {
  return http.request({
    url: "payment/cashier/tradeDetail",
    method: Method.GET,
    needToken: true,
    params,
  });
}


/**
 * 发起支付
 * @param paymentMethod
 * @param paymentClient
 * @param params
 * @returns {*|*}
 */
export function initiatePay(paymentMethod, paymentClient, params) {
  return http.request({
    url: `payment/cashier/pay/${paymentMethod}/${paymentClient}`,
    method: Method.GET,
    needToken: true,
    params,
  });
}
  

/**
 * 查询物流
 * @param orderSn

 */
export function getExpress(orderSn) {
  return http.request({
    url: `/order/order/getTraces/${orderSn}`,
    method: Method.POST,
    needToken: true,
   
  });
}


/**
 * 获取当前会员的对于当前商品可使用的优惠券列表
 */
 export function getMemberCanUse(data) {
  return http.request({
    url: `/promotion/coupon/canUse`,
    method: Method.GET,
    params: data,
  });
}



/**
 * 获取当前会员的优惠券列表
 */
export function getMemberCouponList(data) {
  return http.request({
    url: `/promotion/coupon/getCoupons`,
    method: Method.GET,
    params: data,
  });
}

/**
 * 使用优惠券

 */
export function useCoupon(params) {
  return http.request({
    url: `/trade/carts/select/coupon`,
    method: Method.GET,
    needToken: true,
    params: params,
  });
}


/**
 * 更换参与活动
 * @param params
 */
export function changeActivity(params) {
  return http.request({
    url: "trade/promotion",
    method: Method.POST,
    needToken: true,
    data: params,

    header: { "content-type": "application/x-www-form-urlencoded" },
  });
}

/**
 * 根据交易单号查询订单列表
 * @param trade_sn
 */
export function reBuy(sn) {
  return http.request({
    url: `trade/carts/rebuy/${sn}`,
    method: Method.POST,
    needToken: true,
  });
}
