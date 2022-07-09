/**
 * 收货地址相关API
 */

import { http, Method } from "@/utils/request.js";

import api from "@/config/api.js";

/**
 * 获取收货地址列表
 * @returns {AxiosPromise}
 */
export function getAddressList(pageNumber, pageSize) {
  return http.request({
    url: "/member/address",
    method: Method.GET,
    needToken: true,
    params: { pageNumber, pageSize },
  });
}


/**
 * 获取物流公司
 */
export function getLogistics() {
  return http.request({
    url: "/logistics",
    method: Method.GET,
    needToken: true,
    params: { pageNumber: 1, pageSize: 200, disabled: "OPEN" },
  });
}

/**
 * 通过cityCode获取地区代码
 */
export function getAddressCode(cityCode, townName) {
  return http.request({
    url: api.common + "/common/region/region",
    method: Method.GET,
    needToken: true,
    params: { cityCode, townName },
  });
}

/**
 * 添加收货地址
 * @param params 地址参数
 * @returns {AxiosPromise}
 */
export function addAddress(data) {
  return http.request({
    url: "/member/address",
    method: Method.POST,
    needToken: true,
    header: { "content-type": "application/x-www-form-urlencoded" },
    data: data,
  });
}

/**
 * 编辑地址
 * @param id 地址ID
 * @param params 地址参数
 * @returns {AxiosPromise}
 */
export function editAddress(params) {
  return http.request({
    url: `/member/address`,
    method: Method.PUT,
    needToken: true,
    header: { "content-type": "application/x-www-form-urlencoded" },
    data: params,
  });
}

/**
 * 删除收货地址
 * @param id
 */
export function deleteAddress(id) {
  return http.request({
    url: `/member/address/delById/${id}`,
    method: Method.DELETE,
    needToken: true,
  });
}



/**
 * 根据ID获取会员收件地址
 * @param id
 */
export function getAddressDetail(id) {
  return http.request({
    url: `/member/address/get/${id}`,
    method: Method.GET,
    loading: false,
    needToken: true,
  });
}

/**
 *
 */
export function getAddressDefault() {
  return http.request({
    url: `/member/address/get/default`,
    method: Method.GET,
    loading: false,
    needToken: true,
  });
}
