/**
 * 订单相关API
 */

import { http, Method } from "@/utils/request.js";




/**
 * 选择发票
 * @param params
 */
 export function getReceipt(params) {
  return http.request({
    url: "/trade/carts/select/receipt",
    method: Method.GET,
    needToken: true,
    params,
  });
}

/**
 * 选择发票
 * @param id
 */
export function getReceiptDetail(id) {
  return http.request({
    url: `/trade/receipt/${id}`,
    method: Method.GET,
    needToken: true,
  });
}

/**
 * 选择配送方式
 * @param params
 */
export function selectedShipMethod(params) {
  return http.request({
    url: "/trade/carts/shippingMethod",
    method: Method.GET,
    needToken: true,
    params,
  });
}

/**
 * 获取订单列表
 * @param params
 */
export function getOrderList(params) {
  return http.request({
    url: "/order/order",
    method: Method.GET,
    needToken: true,
    params,
  });
}

/**
 * 获取订单详情
 * @param orderSn 订单编号
 */
export function getOrderDetail(orderSn) {
  return http.request({
    url: `/order/order/${orderSn}`,
    method: Method.GET,
    needToken: true,
  });
}

/**
 * 取消订单
 * @param orderSn 订单编号
 * @param reason   取消原因
 */
export function cancelOrder(orderSn, reason) {
  return http.request({
    url: `/order/order/${orderSn}/cancel`,
    method: Method.POST,
    needToken: true,
    header: { "content-type": "application/x-www-form-urlencoded" },
    data: reason,
  });
}

/**
 * 确认收货
 * @param orderSn 订单编号
 */
export function confirmReceipt(orderSn) {
  return http.request({
    url: `/order/order/${orderSn}/receiving`,
    method: Method.POST,
    needToken: true,
  });
}



/**
 * 获取当前拼团订单的拼团分享信息
 * @param {*} parentOrderSn
 * @param {*} skuId
 */
export function getPinTuanShare(parentOrderSn,skuId) {
  return http.request({
    url: `promotion/pintuan/share`,
    method: Method.GET,
    needToken: true,
    params:{parentOrderSn,skuId}
  });
}

