// 统一请求路径前缀在libs/axios.js中修改
import { getRequest, putRequest, postRequest } from "@/libs/axios";

// 分页获取会员评价
export const getMemberReview = (params) => {
  return getRequest("/member/evaluation", params);
};

// 根据id获取评价详情
export const getMemberInfoReview = (id) => {
  return getRequest(`/member/evaluation/get/${id}`);
};

//回复评价信息
export const replyMemberReview = (id, params) => {
  return putRequest(`/member/evaluation/reply/${id}`, params);
};

// 获取会员注册统计列表
export const getStatisticsList = (params) => {
  return getRequest("/statistics/view/list", params);
};

//   获取分页
export const getMember = (params) => {
  return getRequest("/member/getByPage", params);
};

//  添加或修改
export const insertOrUpdateSpec = (params) => {
  return postRequest("/memberNoticeSenter/insertOrUpdate", params);
};
//删除gUI个
export const delSpec = (id, params) => {
  return deleteRequest(`/goods/spec/del/${id}`, params);
};

//  获取会员列表
export const getMemberListData = (params) => {
  return getRequest("/member", params);
};

//  获取会员详情
export const getMemberInfoData = (id) => {
  return getRequest(`/member/${id}`);
};

//  添加会员基本信息
export const addMember = (params) => {
  return postRequest(`/member`, params);
};

//  获取会员列表
export const getMemberAll = () => {
  return getRequest("/member/getAll");
};

//  增加或修改会员列表
export const operationMemberListData = (params) => {
  return postRequest("/member/insertOrUpdate", params);
};

//  增加或修改会员列表
export const deleteMemberListData = (ids) => {
  return deleteRequest(`/member/delByIds/${ids}`);
};
// 获取充值记录列表数据
export const getUserRecharge = (params) => {
  return getRequest("/wallet/recharge", params);
};

// 获取预存款明细列表数据
export const getUserDeposit = (params) => {
  return getRequest("/deposit", params);
};

// 获取提现申请列表数据
export const getUserWithdrawApply = (params) => {
  return getRequest("/members/withdraw-apply", params);
};

// 审核提现申请
export const withdrawApply = (params) => {
  return postRequest("/members/withdraw-apply", params);
};
