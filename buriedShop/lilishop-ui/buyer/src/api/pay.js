import request, {
  Method
} from '@/plugins/request.js';

/**
 * 获取支付详情
 * @param orderType 交易类型,可用值:TRADE,ORDER,RECHARGE
 * @param sn   订单编号
 * @param clientType  调起方式，PC
 */
export function tradeDetail (params) {
  return request({
    url: '/buyer/payment/cashier/tradeDetail',
    needToken: true,
    method: Method.GET,
    params
  });
}

/**
 * 支付
 * @param orderType 交易类型,可用值:TRADE,ORDER,RECHARGE
 * @param paymentMethod 支付方式 可用值:ALIPAY,WECHAT
 * @param payClient  调起方式 可用值：APP,NATIVE,JSAPI,H5
 * @param sn   订单编号
 */
export function pay (params) {
  return request({
    url: `/buyer/payment/cashier/pay/${params.paymentMethod}/${params.paymentClient}`,
    needToken: true,
    method: Method.GET,
    params
  });
}

/**
 * 支付结果查询
 * @param orderType 交易类型,可用值:TRADE,ORDER,RECHARGE
 * @param sn   订单编号
 */
export function payCallback (params) {
  return request({
    url: `/buyer/payment/cashier/result`,
    needToken: true,
    method: Method.GET,
    params
  });
}
