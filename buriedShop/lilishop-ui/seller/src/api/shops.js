// 统一请求路径前缀在libs/axios.js中修改
import {getRequest, postRequest, putRequest, deleteRequest, importRequest, getRequestWithNoToken} from '@/libs/axios';


// 获取数据字典
export const getDictData = "/dictData/getByType/"
// Websocket
export const ws = "/ws"


//查询店铺列表
export const getShopListData = (params) => {
  return getRequest('/shop', params)
}
// 获取结算单分页
export const getBillPage = (params) => {
  return getRequest(`/order/bill/getByPage`, params)
}

// 获取商家结算单流水分页
export const getSellerFlow = (id, params) => {
  return getRequest(`/order/bill/${id}/getStoreFlow`, params)
}

// 商家核对结算单
export const reconciliation = (id, params) => {
  return putRequest(`/order/bill/check/${id}/`, params)
}

// 获取商家分销订单流水分页
export const getDistributionFlow = (id, params) => {
  return getRequest(`/order/bill/${id}/getDistributionFlow`, params)
}

// 获取商家结算单详细
export const getBillDetail = (id, params) => {
  return getRequest(`/order/bill/get/${id}`, params)
}

// 获取所有物流公司
export const getLogistics = (id, params) => {
  return getRequest(`/other/logistics`, params)
}

// 开启物流公司
export const logisticsChecked = (id, params) => {
  return postRequest(`/other/logistics/${id}`, params)
}

// 关闭开启物流公司
export const logisticsUnChecked = (id, params) => {
  return deleteRequest(`/other/logistics/${id}`, params)
}
// 获取商家自提点
export const getShopAddress = (id, params) => {
  return getRequest(`/member/storeAddress/`, params)
}

// 修改商家自提点
export const editShopAddress = (id, params) => {
  return putRequest(`/member/storeAddress/${id}`, params)
}

// 添加商品自提点
export const addShopAddress = (params) => {
  return postRequest(`/member/storeAddress/`, params)
}

// 添加商品自提点
export const deleteShopAddress = (id) => {
  return deleteRequest(`/member/storeAddress/${id}`)
}

// 获取商家详细信息
export const getShopInfo = () => {
  return getRequest(`/settings/storeSettings`)
}

// 保存商家详细信息
export const saveShopInfo = (params) => {
  return putRequest(`/settings/storeSettings`, params)
}

//获取商家退货地址
export const getRefundGoodsAddress = () => {
  return getRequest(`/settings/storeSettings/storeAfterSaleAddress`)
}
//修改商家退货地址
export const saveRefundGoodsAddress = (params) => {
  return putRequest(`/settings/storeSettings/storeAfterSaleAddress`, params)
}
//修改im商户id
export const updatEmerchantId = (params) => {
  return putRequest(`/settings/storeSettings/merchantEuid`, params)
}


//修改保存库存预警数
export const updateStockWarning = (params) => {
  return putRequest(`/settings/storeSettings/updateStockWarning`, params)
}
//查询运费模板
export const getShipTemplate = () => {
  return getRequest(`/setting/freightTemplate`)
}
//删除运费模板
export const deleteShipTemplate = (id) => {
  return deleteRequest(`/setting/freightTemplate/${id}`)
}
//新增运费模板
export const addShipTemplate = (params, headers) => {
  return postRequest(`/setting/freightTemplate`, params, headers)
}

//新增运费模板
export const editShipTemplate = (id, params, headers) => {
  return putRequest(`/setting/freightTemplate/${id}`, params, headers)
}


