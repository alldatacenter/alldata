/**
 * 信任登录相关API
 */

import {
	http,
	Method
} from '@/utils/request.js';
const request = http.request;


/**
 * web 第三方登录
 * @param {Object} code
 */
export function webConnect(code) {
	return http.request({
		url: `passport/connect/connect/login/web/${code}`,
		method: Method.GET,
		needToken: true,
		header: {
			"clientType": "H5"
		}
	});
}
export function openIdLogin(params, clientType) {
	return http.request({
		url: `passport/connect/connect/app/login`,
		method: Method.GET,
		needToken: true,
		data: params,
		header: {
			"clientType": clientType
		}
	});
}

/**
 * 第三方登录成功 回调接口
 */
export function loginCallback(state) {
	return http.request({
		url: `passport/connect/connect/result?state=${state}`,
		method: Method.GET,
		needToken: false
	});
}



/**
 * 小程序自动登录
 * @param params
 */
export function mpAutoLogin(params) {
	return http.request({
		url: 'passport/connect/miniProgram/auto-login',
		method: Method.GET,
		params
	});
}
