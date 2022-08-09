/**
 * 申请售后相关API
 */

import { http, Method } from "@/utils/request.js";

/**
 * 获取售后列表
 * @param params
 * @returns {AxiosPromise}
 */
export function getAfterSale(params) {
  return http.request({
    url: "after-sales/refunds",
    method: Method.GET,
    needToken: true,
    loading: false,
    params,
  });
}


/******************* 以下为新方法 ***********************/
/**
 * 申请取消订单
 * @param params
 */
export function applyCancelOrder(params) {
  return http.request({
    url: "after-sales/apply/cancel/order",
    method: Method.POST,
    needToken: true,
    params,
  });
}

/**
/**
 * 获取商家售后收件地址
 */
export function getStoreAfterSaleAddress(sn) {
  return http.request({
    url: `/order/afterSale/getStoreAfterSaleAddress/${sn}`,
    method: Method.GET,
    needToken: true,
  });
}
/**
 * 取消售后
 */
export function cancelAfterSale(afterSaleSn) {
  return http.request({
    url: `/order/afterSale/cancel/${afterSaleSn}`,
    method: Method.POST,
    needToken: true,
  });
}



/**
 * 获取售后服务记录相关数据
 * @param params 参数
 */
export function getAfterSaleList(params) {
  return http.request({
    url: `/order/afterSale/page`,
    method: Method.GET,
    needToken: true,
    params,
  });
}

/**
 * 查看售后服务详情
 * @param sn 售后服务单编号
 */
export function getServiceDetail(sn) {
  return http.request({
    url: `/order/afterSale/get/${sn}`,
    method: Method.GET,
    needToken: true,
  });
}


/**
 * 添加投诉
 */
export function addComplain(params) {
  return http.request({
    url: `/order/complain`,
    method: Method.POST,
    needToken: true,
    header: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    data: params,
  });
}

/**
 * 取消投诉
 */
export function clearComplain(id, params) {
  return http.request({
    url: `/order/complain/status/${id}`,
    method: Method.PUT,
    needToken: true,
    params,
  });
}

/**
 * 取消投诉
 */
export function getAfterSaleLog(sn) {
  return http.request({
    url: `/order/afterSale/get/getAfterSaleLog/${sn}`,
    method: Method.GET,
    needToken: true,
  });
}

/**
 * 投诉列表
 */
export function getComplain(params) {
  return http.request({
    url: `/order/complain`,
    method: Method.GET,
    needToken: true,
    params,
  });
}

/**
 * 获取申请原因
 */
export function getAfterSaleReason(serviceType) {
  return http.request({
    url: `/order/afterSale/get/afterSaleReason/${serviceType}`,
    method: Method.GET,
    needToken: true,
  });
}

/**
 * 获取取消原因
 */
export function getClearReason() {
  return http.request({
    url: `/order/afterSale/get/afterSaleReason/CANCEL`,
    method: Method.GET,
    needToken: true,
  });
}

/**
 * 获取投诉原因
 */
export function getComplainReason() {
  return http.request({
    url: `/order/afterSale/get/afterSaleReason/COMPLAIN`,
    method: Method.GET,
    needToken: true,
  });
}
/**
 * 获取投诉详情
 */
export function getComplainDetail(id) {
  return http.request({
    url: `/order/complain/${id}`,
    method: Method.GET,
    needToken: true,
  });
}

/**
 * 获取申请售后页面信息
 */
export function getAfterSaleInfo(sn) {
  return http.request({
    url: `/order/afterSale/applyAfterSaleInfo/${sn}`,
    method: Method.GET,
    needToken: true,
  });
}

/**
 * 申请退货服务
 * @param params
 */
export function applyReturn(orderItemSn, params) {
  return http.request({
    url: `/order/afterSale/save/${orderItemSn}`,
    method: Method.POST,
    header: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    data: params,
  });
}

/**
 * 填充物流信息
 * @param afterSaleSn 售后服务单号
 * @param params 参数信息
 */
export function fillShipInfo(afterSaleSn, params) {
  return http.request({
    url: `/order/afterSale/delivery/${afterSaleSn}`,
    method: Method.POST,
    header: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    data: params,
  });
}
