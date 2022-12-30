// 统一请求路径前缀在libs/axios.js中修改
import {
  getRequest,
  putRequest,
  postRequest,
  deleteRequest,
} from "@/libs/axios";

//   获取分页
export const getMember = (params) => {
  return getRequest("/member/getByPage", params);
};

// 分页获取会员评价
export const getMemberReview = (params) => {
  return getRequest("/member/evaluation/getByPage", params);
};

// 获取id
export const getMemberInfoReview = (id) => {
  return getRequest(`/member/evaluation/get/${id}`);
};

// 删除评论
export const delMemberReview = (id) => {
  return putRequest(`/member/evaluation/delete/${id}`);
};
// 修改评价状态
export const updateMemberReview = (id, params) => {
  return getRequest(`/member/evaluation/updateStatus/${id}`, params);
};

//  添加或修改
export const insertOrUpdateSpec = (params) => {
  return postRequest("/memberNoticeSenter/insertOrUpdate", params);
};

//  获取会员列表
export const getMemberListData = (params) => {
  return getRequest("/passport/member", params);
};

//  获取会员详情
export const getMemberInfoData = (id) => {
  return getRequest(`/passport/member/${id}`);
};

//  修改会员基本信息
export const updateMember = (params) => {
  return putRequest(`/passport/member`, params);
};

//  添加会员基本信息
export const addMember = (params) => {
  return postRequest(`/passport/member`, params);
};

//  获取会员列表
export const getMemberAll = () => {
  return getRequest("/passport/member/getAll");
};

//  增加或修改会员列表
export const operationMemberListData = (params) => {
  return postRequest("/passport/member/insertOrUpdate", params);
};

//  增加或修改会员列表
export const deleteMemberListData = (ids) => {
  return deleteRequest(`/passport/member/delByIds/${ids}`);
};
// 获取充值记录列表数据
export const getUserRecharge = (params) => {
  return getRequest("/wallet/recharge", params);
};

// 获取预存款明细列表数据
export const getUserWallet = (params) => {
  return getRequest("/wallet/log", params);
};

// 获取提现申请列表数据
export const getUserWithdrawApply = (params) => {
  return getRequest("/wallet/withdrawApply", params);
};

// 审核提现申请
export const withdrawApply = (params) => {
  return postRequest("/wallet/withdrawApply", params);
};

//会员状态修改
export const updateMemberStatus = (params) => {
  return putRequest("/passport/member/updateMemberStatus", params);
};

// 获取会员注册统计列表
export const getMemberStatistics = (params) => {
  return getRequest("/statistics/member", params);
};

// 获取流量统计
export const getStatisticsList = (params) => {
  return getRequest("/statistics/view/list", params);
};

// 获取会员历史流量
export const historyMemberChartList = () => {
  return getRequest("/statistics/view/online/history");
};

//查询会员数量
export const getMemberNum = (params) => {
  return getRequest("/passport/member/num", params);
};
//查询会员历史积分
export const getHistoryPointData = (params) => {
  return getRequest(`/member/memberPointsHistory/getByPage`, params);
};
//查询会员的收货地址
export const getMemberAddressData = (id, params) => {
  return getRequest(`/member/address/${id}`, params);
};
//删除会员地址
export const removeMemberAddress = (id) => {
  return deleteRequest(`/member/address/delById/${id}`);
};
//添加会员收货地址
export const addMemberAddress = (params) => {
  return postRequest(`/member/address`, params);
};
//修改会员收货地址
export const editMemberAddress = (params) => {
  return putRequest(`/member/address`, params);
};
//查询会员预存款
export const getMemberWallet = (params) => {
  return getRequest(`/wallet/wallet`, params);
};
