// import {
//   getRequest,
//   postRequest,
//   putRequest,
//   deleteRequest,
//   importRequest,
//   getRequestWithNoToken
// } from '@/axios/index.js';
import request, {Method} from '@/plugins/request.js'

// 获取密码状态
export function getPwdStatus (params) {
  return request({
    url: '/buyer/passport/member/wallet/check',
    method: Method.GET,
    needToken: true,
    params
  })
}

// 设置密码
export function setPwd (params) {
  return request({
    url: '/buyer/passport/member/wallet/set-password',
    method: Method.POST,
    needToken: true,
    data: params
  })
}

// 设置支付密码
export function setUpdatePwdOrdinary (params) {
  return request({
    url: '/buyer/passport/member/wallet/update-password/ordinary',
    method: Method.GET,
    needToken: true,
    data: params
  })
}

// 修改会员资料
export function editMemberInfo (params) {
  return request({
    url: '/buyer/passport/member/editOwn',
    method: Method.PUT,
    needToken: true,
    data: params
  })
}

// 修改密码
export function editPwd (params) {
  return request({
    url: `/buyer/passport/member/modifyPass`,
    method: Method.PUT,
    needToken: true,
    data: params
  })
}

// 获取密码状态
export function logout () {
  return request({
    url: '/buyer/passport/member/logout',
    method: Method.POST,
    needToken: true
  })
}
