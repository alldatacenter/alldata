// 统一请求路径前缀在libs/axios.js中修改
import {
  getRequest,
  postRequest,
  putRequest,
  deleteRequest
} from "@/libs/axios";

// 获取秒杀活动申请列表

export const getPromotionSeckill = params => {
  return getRequest(`/promotion/seckill/apply`, params);
};

// 是否推荐直播间
export const whetherStar = params => {
  return putRequest(`/broadcast/studio/recommend/${params.id}`, params);
};

// 添加优惠券活动
export const addCouponActivity = params => {
  return postRequest(`/promotion/couponActivity/addCouponActivity`, params);
};


// 获取店铺直播间列表
export const getLiveList = params => {
  return getRequest("/broadcast/studio", params);
};

// 获取直播间详情
export const getLiveInfo = studioId => {
  return getRequest(`/broadcast/studio/${studioId}`);
};

// 获取当前进行中的促销活动商品

export const getPromotionGoods = (promotionId, params) => {
  return getRequest(`/promotion/${promotionId}/goods`, params);
};

// 获取当前进行中的促销活动
export const getAllPromotion = params => {
  return getRequest("/promotion/promotion/current", params);
};

// 获取拼团数据
export const getPintuanList = params => {
  return getRequest("/promotion/pintuan", params);
};

// 获取拼团详情
export const getPintuanDetail = id => {
  return getRequest(`/promotion/pintuan/${id}`);
};

// 获取拼团商品数据
export const getPintuanGoodsList = params => {
  return getRequest(`/promotion/pintuan/goods/${params.pintuanId}`, params);
};

// 关闭拼团活动
export const updatePintuanStatus = (pintuanId, params) => {
  return putRequest(`/promotion/pintuan/status/${pintuanId}`, params);
};

// 修改砍价活动商品
export const saveKanJiaActivityGoods = params => {
  return postRequest("/promotion/kanJiaGoods", params, {
    "Content-type": "application/json"
  });
};
// 修改砍价活动商品
export const editKanJiaActivityGoods = params => {
  return putRequest("/promotion/kanJiaGoods", params, {
    "Content-type": "application/json"
  });
};
// 获取砍价活动商品
export const getKanJiaGoodsList = params => {
  return getRequest(`/promotion/kanJiaGoods`, params);
};

//删除砍价活动商品
export const delKanJiaGoods = ids => {
  return deleteRequest(`/promotion/kanJiaGoods/${ids}`);
};

// 砍价活动商品详情
export const getKanJiaActivityGoodsById = id => {
  return getRequest(`/promotion/kanJiaGoods/${id}`);
};

// 保存平台优惠券
export const savePlatformCoupon = params => {
  return postRequest("/promotion/coupon", params, {
    "Content-type": "application/json"
  });
};
// 修改平台优惠券
export const editPlatformCoupon = params => {
  return putRequest("/promotion/coupon", params, {
    "Content-type": "application/json"
  });
};

// 获取平台优惠券
export const getPlatformCouponList = params => {
  return getRequest("/promotion/coupon", params);
};
//  作废优惠券
export const deletePlatformCoupon = ids => {
  return deleteRequest(`/promotion/coupon/${ids}`);
};
//  更新优惠券状态
export const updatePlatformCouponStatus = params => {
  return putRequest(`/promotion/coupon/status`, params);
};
//  获取单个优惠券
export const getPlatformCoupon = id => {
  return getRequest(`/promotion/coupon/${id}`);
};

// 获取优惠券领取详情
export const getMemberReceiveCouponList = id => {
  return getRequest(`/promotion/coupon/member/${id}`);
};
//  作废会员优惠券
export const deleteMemberReceiveCoupon = id => {
  return putRequest(`/promotion/coupon/member/cancellation/${id}`);
};


// 保存平台优惠券
export const saveActivityCoupon = params => {
  return postRequest("/promotion/couponActivity", params, {
    "Content-type": "application/json"
  });
};
// 获取活动优惠券列表
export const getActivityCouponList = params => {
  return getRequest("/promotion/couponActivity/activityCoupons", params);
};
// 获取平台优惠券活动
export const getCouponActivityList = params => {
  return getRequest("/promotion/couponActivity", params);
};
// 关闭优惠券活动
export const closeActivity = id => {
  return deleteRequest(`/promotion/couponActivity/${id}`);
};


//  更新优惠券活动
export const updateCouponActivity = params => {
  return putRequest(`/promotion/couponActivity/status`, params);
};
//  获取单个优惠券活动
export const getCouponActivity = id => {
  return getRequest(`/promotion/couponActivity/${id}`);
};

// 获取秒杀活动数据
export const getSeckillList = params => {
  return getRequest("/promotion/seckill", params);
};

// 获取秒杀活动审核列表
export const seckillGoodsList = params => {
  return getRequest("/promotion/seckill/apply", params);
};

// 获取秒杀活动详情数据
export const seckillDetail = (id, params) => {
  return getRequest(`/promotion/seckill/${id}`, params);
};

// 删除秒杀活动
export const delSeckill = id => {
  return deleteRequest(`/promotion/seckill/${id}`);
};

// 保存秒杀活动
export const saveSeckill = params => {
  return postRequest("/promotion/seckill", params);
};

// 修改秒杀活动
export const updateSeckill = params => {
  return putRequest("/promotion/seckill", params, {
    "Content-type": "application/json"
  });
};

// 关闭秒杀活动
export const updateSeckillStatus = (id, params) => {
  return putRequest(`/promotion/seckill/status/${id}`, params);
};
// 删除秒杀商品
export const delSeckillGoods = params => {
  return deleteRequest(`/promotion/seckill/apply/${params.seckillId}/${params.id}`);
};
// 审核秒杀活动
export const auditApplySeckill = params => {
  return putRequest(`/promotion/seckill/apply/audit/${params.ids}`, params);
};

// 满优惠列表
export const getFullDiscountList = params => {
  return getRequest(`/promotion/fullDiscount`, params);
};
// 满优惠列表
export const getFullDiscountById = id => {
  return getRequest(`/promotion/fullDiscount/${id}`);
};
// 开启、关闭满额活动
export const updateFullDiscount = (id, promotionStatus) => {
  return putRequest(`/promotion/fullDiscount/status/${id}`)
}
// 积分商品列表
export const getPointsGoodsList = params => {
  return getRequest(`/promotion/pointsGoods`, params);
};
// 积分商品详情
export const getPointsGoodsById = id => {
  return getRequest(`/promotion/pointsGoods/${id}`);
};
// 添加积分商品
export const addPointsGoods = params => {
  return postRequest(`/promotion/pointsGoods`, params, {
    "Content-type": "application/json"
  });
};
// 修改积分商品
export const updatePointsGoods = params => {
  return putRequest(`/promotion/pointsGoods`, params, {
    "Content-type": "application/json"
  });
};
// 修改积分商品状态
export const editPointsGoodsStatus = (id, params) => {
  return putRequest(`/promotion/pointsGoods/status/${id}`, params);
};
// 删除积分商品
export const deletePointsGoodsStatus = id => {
  return deleteRequest(`/promotion/pointsGoods/${id}`);
};

// 积分商品分类列表
export const getPointsGoodsCategoryList = params => {
  return getRequest(`/promotion/pointsGoodsCategory`, params);
};
// 积分商品分类详情
export const getPointsGoodsCategoryById = id => {
  return getRequest(`/promotion/pointsGoodsCategory/${id}`);
};
// 添加积分商品分类
export const addPointsGoodsCategory = params => {
  return postRequest(`/promotion/pointsGoodsCategory`, params);
};
// 更新积分商品分类
export const updatePointsGoodsCategory = params => {
  return putRequest(`/promotion/pointsGoodsCategory`, params);
};
// 删除积分商品分类
export const deletePointsGoodsCategoryById = id => {
  return deleteRequest(`/promotion/pointsGoodsCategory/${id}`);
};
