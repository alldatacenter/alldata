import request, {
  Method
} from '@/plugins/request.js';

/**
 * 清空购物车
 */
export function clearCart () {
  return request({
    url: '/buyer/trade/carts',
    method: Method.DELETE,
    needToken: true
  });
}

/**
 * 获取购物车页面购物车详情
 */
export function cartGoodsAll () {
  return request({
    url: '/buyer/trade/carts/all',
    method: Method.GET,
    needToken: true
  });
}

/**
 * 获取购物车商品数量
 */
export function cartCount () {
  return request({
    url: '/buyer/trade/carts/count',
    method: Method.GET,
    needToken: true
  });
}

/**
 * 获取结算页面购物车详情
 */
export function cartGoodsPay (params) {
  return request({
    url: '/buyer/trade/carts/checked',
    method: Method.GET,
    needToken: true,
    params
  });
}

/**
 * 向购物车添加一个商品
 * @param skuId skuId
 * @param  num  购买数量
 */
export function addCartGoods (params) {
  return request({
    url: '/buyer/trade/carts',
    method: Method.POST,
    needToken: true,
    params
  });
}

/**
 * 创建交易
 * @param client 客户端：H5/移动端 PC/PC端,WECHAT_MP/小程序端,APP/移动应用端
 * @param way 购物车购买：CART/立即购买：BUY_NOW / 积分购买：POINT
 * @param remark 备注  非必填
 */
export function createTrade (data) {
  return request({
    url: '/buyer/trade/carts/create/trade',
    method: Method.POST,
    needToken: true,
    headers: {'Content-Type': 'application/json'},
    data
  });
}

/**
 * 选择优惠券
 * @param memberCouponId 优惠券id
 * @param way 购物车购买：CART/立即购买：BUY_NOW/ 积分购买：POINT
 * @param used 使用true 弃用 false
 */
export function selectCoupon (params) {
  return request({
    url: '/buyer/trade/carts/select/coupon',
    method: Method.GET,
    needToken: true,
    params
  });
}

/**
 * 可用优惠券数量
 */
export function couponNum (params) {
  return request({
    url: '/buyer/trade/carts/coupon/num',
    method: Method.GET,
    needToken: true,
    params
  });
}
/**
 * 选择收货地址
 * @param shippingAddressId 地址id
 * @param way 购物车类型
 */
export function selectAddr (params) {
  return request({
    url: `/buyer/trade/carts/shippingAddress`,
    method: Method.GET,
    needToken: true,
    params
  });
}

/**
 * 选中购物车所有商品
 * @param checked 设置选中 0，1
 */
export function setCheckedAll (params) {
  return request({
    url: `/buyer/trade/carts/sku/checked`,
    method: Method.POST,
    needToken: true,
    params
  });
}

/**
 * 批量设置某商家的商品为选中或不选中
 * @param checked 是否选中
 * @param storeId   商家id
 */
export function setCheckedSeller (params) {
  return request({
    url: `/buyer/trade/carts/store/${params.storeId}`,
    method: Method.POST,
    needToken: true,
    params
  });
}

/**
 * 选中购物车中单个产品
 * @param skuId 产品id
 * @param checked 设置选中0，1
 */
export function setCheckedGoods (params) {
  return request({
    url: `/buyer/trade/carts/sku/checked/${params.skuId}`,
    method: Method.POST,
    needToken: true,
    params
  });
}

/**
 * 更新购物车中单个产品数量
 * @param skuId 产品id
 * @param num   产品数量
 */
export function setCartGoodsNum (params) {
  return request({
    url: `/buyer/trade/carts/sku/num/${params.skuId}`,
    method: Method.POST,
    needToken: true,
    params
  });
}

/**
 * 删除购物车中一个或多个产品
 * @param skuIds 产品id数组
 */
export function delCartGoods (params) {
  return request({
    url: `/buyer/trade/carts/sku/remove`,
    method: Method.DELETE,
    needToken: true,
    params
  });
}

/**
 * 选择配送方式
 * @param shippingMethod SELF_PICK_UP(自提),LOCAL_TOWN_DELIVERY(同城配送),LOGISTICS(物流)
 * @param way 购物方式
 */
export function shippingMethod (params) {
  return request({
    url: `/buyer/trade/carts/shippingMethod`,
    method: Method.GET,
    needToken: true,
    params
  });
}

/**
 * 选择发票
 * @param receiptId 发票Id
 * @param way 购物方式
 */
export function receiptSelect (params) {
  return request({
    url: `/buyer/trade/carts/select/receipt`,
    method: Method.GET,
    needToken: true,
    params
  });
}
