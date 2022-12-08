// 统一请求路径前缀在libs/axios.js中修改
import {
  getRequest,
  postRequest,
  putRequest,
  deleteRequest
} from '@/libs/axios';


//  交由商家申诉
export const storeComplain = (params) => {
  return putRequest(`/order/complain/status`, params)
}


//  获取售后原因分页列表
export const getAfterSaleReasonPage = (params) => {
  return getRequest('/order/afterSaleReason/getByPage', params)
}

//  删除售后原因
export const delAfterSaleReason = (id) => {
  return deleteRequest(`/order/afterSaleReason/delByIds/${id}`)
}

//  添加售后原因
export const addAfterSaleReason = (params) => {
  return postRequest(`/order/afterSaleReason`, params)
}

//  修改售后原因
export const editAfterSaleReason = (id, params) => {
  return putRequest(`/order/afterSaleReason/update/${id}`, params)
}

// 售后服务单详情
export const afterSaleOrderDetail = (sn) => {
  return getRequest(`/order/afterSale/${sn}`)
}

// 商家审核
export const afterSaleSellerReview = (sn, params) => {
  return putRequest(`/order/afterSale/review/${sn}`, params)
}

// 商家确认收货
export const afterSaleSellerConfirm = (sn, params) => {
  return putRequest(`/order/afterSale/confirm/${sn}`, params)
}

// 商家换货业务发货
export const afterSaleSellerDelivery = (sn, params) => {
  return putRequest(`/order/afterSale/${sn}/delivery`, params)
}

//  获取交易投诉信息
export const getOrderComplain = (params) => {
  return getRequest('/order/complain', params)
}

//  获取交易投诉详细信息
export const getOrderComplainDetail = (id) => {
  return getRequest(`/order/complain/${id}`)
}

//  交易投诉对话
export const addOrderCommunication = (params) => {
  return postRequest(`/order/complain/communication`, params)
}

//  平台仲裁
export const orderComplete = (id, params) => {
  return putRequest(`/order/complain/complete/${id}`, params)
}

//  查询订单列表
export const getOrderList = (params) => {
  return getRequest(`/order/order`, params)
}

//  订单付款
export const orderPay = (sn) => {
  return postRequest(`/order/order/${sn}/pay`)
}

//  订单付款
export const orderDetail = (sn) => {
  return getRequest(`/order/order/${sn}`)
}

//  修改订单价格
export const updateOrderPrice = (sn, params) => {
  return putRequest(`/order/order/update/${sn}/price`, params)
}

// 获取订单日志
export const getOrderLog = (params) => {
  return getRequest(`/orderLog/getByPage`, params)
}

// 订单取消
export const orderCancel = (sn, params) => {
  return postRequest(`/order/order/${sn}/cancel`, params)
}

// 修改收货地址
export const editOrderConsignee = (sn, params) => {
  return postRequest(`/order/order/update/${sn}/consignee`, params)
}

//  获取售后订单列表
export const getAfterSaleOrderPage = (params) => {
  return getRequest('/order/afterSale/page', params)
}

//  获取售后订单详情
export const getAfterSaleOrderDetail = (sn) => {
  return getRequest(`/order/afterSale/get/${sn}`)
}
//售后单查询物流
export const getAfterSaleTraces = (sn) => {
  return getRequest(`/order/afterSale/getDeliveryTraces/${sn}`)
}
// 获取商家选中的物流公司
export const getLogisticsChecked = () => {
  return getRequest(`/other/logistics/getChecked`)
}

//查询物流
export const getTraces = (sn, params) => {
  return postRequest(`/order/order/getTraces/${sn}`, params)
}
//获取发票列表
export const getReceiptPage = (params) => {
  return getRequest(`/trade/receipt`, params)
}
//平台退款
export const refundPrice = (afterSaleSn, params) => {
  return putRequest(`/order/afterSale/refund/${afterSaleSn}`, params)
}


//平台收款记录
export const paymentLog = (params) => {
  return getRequest(`/order/paymentLog`, params)
}


//平台退款记录
export const refundLog = (params) => {
  return getRequest(`/order/refundLog`, params)
}

//售后单商家收货信息
export const storeAddress = (sn) => {
  return getRequest(`/order/afterSale/getStoreAfterSaleAddress/${sn}`)
}
