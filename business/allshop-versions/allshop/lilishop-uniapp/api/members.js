import storage from "@/utils/storage";
import { http, Method } from "@/utils/request.js";

/**
 * 意见反馈
 */
export function feedBack(params) {
  return http.request({
    url: "/other/feedback",
    method: Method.POST,
    needToken: true,
    params,
  });
}

// 提现
export function withdrawalApply(params) {
  return http.request({
    url: "/wallet/wallet/withdrawal",
    method: Method.POST,
    needToken: true,
    params,
  });
}

/**
 * 支付结果查询
 * @param orderType 交易类型,可用值:TRADE,ORDER,RECHARGE
 * @param sn   订单编号
 */
export function payCallback(params) {
  return http.request({
    url: `/payment/cashier/result`,
    method: Method.GET,
    params,
  });
}

// 在线充值
export function recharge(params) {
  return http.request({
    url: "/trade/recharge",
    method: Method.POST,
    params,
  });
}

/**
 * 分页获取预存款充值记录
 * @param params
 */
export function getUserRecharge(params) {
  return http.request({
    url: "/member/recharge",
    method: Method.GET,
    needToken: true,
    params,
  });
}

/**
 * 分页获取预存款充值记录
 * @param params
 */
export function getWalletLog(params) {
  return http.request({
    url: "/wallet/log",
    method: Method.GET,
    needToken: true,
    params,
  });
}

/**
 * 获取优惠券列表
 * @param params
 */
export function getCoupons(params) {
  return http.request({
    url: "/promotion/coupon/getCoupons",
    method: Method.GET,
    needToken: true,
    params,
  });
}

/**
 * 领取优惠券
 * @param couponId
 */
export function receiveCoupons(couponId) {
  return http.request({
    url: `/promotion/coupon/receive/${couponId}`,
    method: Method.GET,
    needToken: true,
  });
}

/**
 * 获取积分明细数据
 * @param params
 * @returns {AxiosPromise}
 */
export function getPointsData(params) {
  return http.request({
    url: "member/memberPointsHistory/getByPage",
    method: Method.GET,
    needToken: true,
    params,
  });
}

/**
 * 获取我的评论列表
 * @param params
 * @returns {AxiosPromise}
 */
export function getComments(params) {
  return http.request({
    url: "/member/evaluation",
    method: Method.GET,
    needToken: true,
    params,
  });
}

/**
 * 获取当前会员的浏览数量是多少
 * @param params
 * @returns {AxiosPromise}
 */
export function getFootprintNum(params) {
  return http.request({
    url: "/member/footprint/getFootprintNum",
    method: Method.GET,
    needToken: true,
    params,
  });
}

/**
 * 订单评论
 * @param params
 */
export function commentsMemberOrder(params) {
  return http.request({
    url: "/member/evaluation",
    method: Method.POST,
    needToken: true,
    header: { "content-type": "application/x-www-form-urlencoded" },
    data: params,
  });
}

/**
 * 追加评论
 * @param params
 */
export function AppendCommentsOrder(params) {
  return http.request({
    url: "members/comments/additional",
    method: Method.POST,
    needToken: true,
    header: { "content-type": "application/x-www-form-urlencoded" },
    data: params,
  });
}

// TODO 第一版本暂未实现此功能
/**
 * 商品咨询
 * @param goods_id
 * @param ask_content
 */
export function consultating(goods_id, ask_content, anonymous) {
  return http.request({
    url: "members/asks",
    method: Method.POST,
    header: { "content-type": "application/x-www-form-urlencoded" },
    needToken: true,
    data: {
      goods_id,
      ask_content,
      anonymous,
    },
  });
}

/**
 * 获取商品收藏
 * @param params
 * @returns {AxiosPromise}
 */
export function getGoodsCollection(params, type) {
  return http.request({
    url: `/member/collection/${type}`,
    method: Method.GET,
    needToken: true,
    loading: false,
    message: false,
    params,
  });
}

/**
 * 收藏商品
 * @returns {AxiosPromise}
 */
export function collectionGoods(type, id) {
  return http.request({
    url: `/member/collection/add/${type}/${id}`,
    method: Method.POST,
    needToken: true,
  });
}

/**
 * 删除商品收藏
 * @param ids 收藏ID【集合或单个商品ID】
 * @returns {AxiosPromise}
 */
export function deleteGoodsCollection(ids) {
  if (Array.isArray(ids)) {
    ids = ids.join(",");
  }
  return http.request({
    url: `/member/collection/delete/GOODS/${ids}`,
    method: Method.DELETE,
    needToken: true,
  });
}

/**
 * 删除店铺收藏
 * @param store_id
 */
export function deleteStoreCollection(store_id) {
  return http.request({
    url: `/member/collection/delete/STORE/${store_id}`,
    method: Method.DELETE,
    needToken: true,
  });
}

/**
 * 获取商品是否被收藏
 * @param good_id
 */
export function getGoodsIsCollect(type, good_id) {
  return http.request({
    url: `/member/collection/isCollection/${type}/${good_id}`,
    method: Method.GET,
    needToken: true,
    loading: false,
  });
}

/**
 * 收藏店铺
 * @param store_id 店铺ID
 * @returns {AxiosPromise}
 */
export function collectionStore(store_id) {
  return http.request({
    url: "members/collection/store",
    header: { "content-type": "application/x-www-form-urlencoded" },
    method: Method.POST,
    data: { store_id },
  });
}

/**
 * 获取当前登录的用户信息
 * @returns {AxiosPromise}
 */
export function getUserInfo() {
  return http.request({
    url: "/passport/member",
    method: Method.GET,
    needToken: true,
  });
}

/**
 * 获取当前用户的预存款
 * @returns {AxiosPromise}
 */
export function getUserWallet() {
  return http.request({
    url: "/wallet/wallet",
    method: Method.GET,
    needToken: true,
  });
}

/**
 * 保存用户信息
 * @param params
 * @returns {AxiosPromise}
 */
export function saveUserInfo(params) {
  return http.request({
    url: "/passport/member/editOwn",
    method: Method.PUT,
    header: { "content-type": "application/x-www-form-urlencoded" },
    needToken: true,
    data: params,
  });
}

/**
 * 添加发票
 * @param params
 */
export function addReceipt(params) {
  return http.request({
    url: "/trade/receipt",
    method: Method.POST,
    needToken: true,
    params,
  });
}

/**
 * 获取商品评论列表
 * @param goodsId
 * @param params
 */
export function getGoodsComments(goodsId, params) {
  return http.request({
    url: `/member/evaluation/${goodsId}/goodsEvaluation`,
    method: Method.GET,
    loading: false,
    params,
  });
}

/**
 * 获取商品评论数量统计
 * @param goodsId
 */
export function getGoodsCommentsCount(goodsId) {
  return http.request({
    url: `/member/evaluation/${goodsId}/evaluationNumber`,
    method: Method.GET,
    loading: false,
  });
}

/**
 * 获取未读消息数量信息
 */
export function getNoReadMessageNum() {
  return http.request({
    url: `members/member-nocice-logs/number`,
    method: Method.GET,
    needToken: true,
  });
}

/**
 * 我的足迹列表
 * @param pageNumber  pageSize
 *
 */
export function myTrackList(params) {
  return http.request({
    url: `/member/footprint`,
    method: Method.GET,
    needToken: true,
    params,
  });
}

/**
 * 根据id删除会员足迹
 * @param id
 */
export function deleteHistoryListId(ids) {
  return http.request({
    url: `/member/footprint/delByIds/${ids}`,
    method: Method.DELETE,
    needToken: true,
  });
}

/**
 * 获取当前会员优惠券列表
 * @param
 */
export function getMemberCoupons(data) {
  return http.request({
    url: `/promotion/coupon/getCoupons`,
    method: Method.GET,
    needToken: true,
    params: data,
  });
}

/**
 * 获取当前会员可使用的优惠券数量
 *
 */
export function getCouponsNum() {
  return http.request({
    url: `/promotion/coupon/getCouponsNum`,
    method: Method.GET,
    needToken: true,
  });
}

/**
 * 获取会员积分VO
 * @param
 */
export function getMemberPointSum() {
  return http.request({
    url: `member/memberPointsHistory/getMemberPointsHistoryVO`,
    method: Method.GET,
  });
}
