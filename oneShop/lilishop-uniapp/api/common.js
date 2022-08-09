/**
 * 公共API
 */
import { http, Method } from "@/utils/request.js";
import api from "@/config/api.js";

/**
 * 获取地区数据
 * @param id
 */
export function getRegionsById(id = 0) {
  return http.request({
    url: `${api.common}/common/region/item/${id}`,
    method: Method.GET,
    message: false,
  });
}

// 获取IM接口前缀
export function getIMDetail() {
  return http.request({
    url: `${api.common}/IM`,
    method: Method.GET,
    message: false,
  });
}

/**
 * 文件上传地址
 * @type {string}
 */
export const upload = api.common + "/common/upload/file";
