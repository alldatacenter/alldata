/**
 * 安全相关API
 */

import {http,Method} from '@/utils/request.js';
import storage from "@/utils/storage.js"
import { md5 } from '@/utils/md5.js'

/**
 * 发送绑定手机验证码
 * @param mobile
 * @param captcha
 */
export function sendBindMobileSms(mobile, captcha) {
  return http.request({
    url: `members/security/bind/send/${mobile}`,
    method: Method.POST,
    needToken: true,
	header:{'content-type':"application/x-www-form-urlencoded"},
    data: {
      uuid: storage.getUuid(),
      captcha,
    },
  });
}

/**
 * 绑定手机号
 * @param mobile
 * @param sms_code
 */
export function bindMobile(mobile, sms_code) {
  return http.request({
    url: `members/security/bind/${mobile}`,
    method: Method.PUT,
    needToken: true,
    data: {sms_code},
  });
}

/**
 * 发送手机验证码
 * 在修改手机号和更改密码时通用
 * @param captcha
 */
export function sendMobileSms(captcha) {
  return http.request({
    url: 'members/security/send',
    method: Method.POST,
    needToken: true,
	header:{'content-type':"application/x-www-form-urlencoded"},
    data: {
      uuid: storage.getUuid(),
      captcha,
    },
  });
}

/**
 * 验证更换手机号短信
 * @param sms_code
 */
export function validChangeMobileSms(sms_code) {
  return http.request({
    url: 'members/security/exchange-bind',
    method: Method.GET,
    needToken: true,
    params: {sms_code},
  });
}

/**
 * 更换手机号
 * @param mobile
 * @param sms_code
 */
export function changeMobile(mobile, sms_code) {
  return http.request({
    url: `members/security/exchange-bind/${mobile}`,
    method: Method.PUT,
	header:{'content-type':"application/x-www-form-urlencoded"},
    needToken: true,
    data: {sms_code},
  });
}


/**
 * 更改密码
 * @param captcha
 * @param password
 */
export function changePassword(captcha, password) {
  return http.request({
    url: 'members/security/password',
    method: Method.PUT,
	header:{'content-type':"application/x-www-form-urlencoded"},
    needToken: true,
    data: {
      uuid: storage.getUuid(),
      captcha,
      password: md5(password),
    },
  });
}

/**
 * 获取当前实名认证进度
 * @param email
 * @param email_code
 */
export function contractStep() {
  return http.request({
    url: `members/contract/step`,
    method: Method.GET,
    needToken: true
  })
}

/**
 * 实名认证
 * @param email
 * @param email_code
 */
export function authentication(params) {
  return http.request({
    url: `members/contract/authentication`,
    method: Method.POST,
    needToken: true,
	header:{'content-type':"application/x-www-form-urlencoded"},
    data: params
  })
}



