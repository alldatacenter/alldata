
import {http, Method} from '@/utils/request.js';

/**
 * 签到
 * @param params
 */
export function sign() {
  return http.request({
    url: '/members/sign',
    method: Method.POST,
    needToken: true,
  });
}



/**
 * 签到时间获取
 * @param params
 */
export function signTime(time) {
  return http.request({
    url: '/members/sign?time='+time,
    method: Method.GET,
    needToken: true,
  });
}
