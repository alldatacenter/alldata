/**
 * Created by Andste on 2018/5/2.
 * 用户认证相关API
 */
import storage from '@/utils/storage.js';
import {http, Method} from '@/utils/request.js';
import { md5 } from '@/utils/md5.js';

/**
 * 普通登录
 * @param username
 * @param password
 * @param captcha
 */
export function login(username, password, captcha) {
  return http.request({
    url: 'passport/login',
    method: Method.POST,
    params: {
      username,
      password: md5(password),
      captcha,
      uuid: storage.getUuid(),
    },
  });
}





/**
 * 验证账户信息
 * @param captcha
 * @param account
 */
export function validAccount(captcha, account) {
  return http.request({
    url: 'passport/find-pwd',
    method: Method.GET,
    params: {
      uuid: storage.getUuid(),
      captcha,
      account,
    },
  });
}

/**
 * 发送找回密码短信
 * @param uuid
 * @param captcha
 */
export function sendFindPasswordSms(uuid,captcha) {
  return http.request({
    url: 'passport/find-pwd/send',
    method: Method.POST,
	header:{'content-type':"application/x-www-form-urlencoded"},
    data: {
      uuid:uuid,
      captcha,
    },
  });
}

/**
 * 校验找回密码验证码
 * @param uuid
 * @param sms_code
 */
export function validFindPasswordSms(uuid, sms_code) {
  return http.request({
    url: 'passport/find-pwd/valid',
    method: Method.GET,
    params: {
      uuid,
      sms_code,
    },
  });
}

/**
 * 修改密码【找回密码用】
 * @param password
 * @param uuid
 */
export function changePassword(password, uuid) {
  if (!uuid) {
    uuid = storage.getUuid();
  }
  return http.request({
    url: 'passport/find-pwd/update-password',
    method: Method.PUT,
	header:{'content-type':"application/x-www-form-urlencoded"},
    data: {
      uuid,
      password: md5(password),
    },
  });
}



// 保存生物认证登录
export function setBiolofy(params) {
	return http.request({
		url: `passport/login/save/biology`,
		method: 'POST',
		params
	})
}
