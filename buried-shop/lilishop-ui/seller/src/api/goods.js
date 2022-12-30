// 统一请求路径前缀在libs/axios.js中修改
import {
  getRequest,
  postRequest,
  putRequest,
  deleteRequest,
  importRequest,
  getRequestWithNoToken,
  commonUrl
} from "@/libs/axios";
// 获取数据字典
export const getDictData = "/dictData/getByType/";
// Websocket
export const ws = "/ws";

// 获取当前店铺分类
export const getShopGoodsLabelList = () => {
  return getRequest(`/goods/label`);
};

// 添加当前店铺分类
export const addShopGoodsLabel = params => {
  return postRequest(`/goods/label`, params);
};

// 修改当前店铺分类
export const editShopGoodsLabel = params => {
  return putRequest(`/goods/label`, params);
};

// 删除当前店铺分类
export const delCategdelShopGoodsLabel = id => {
  return deleteRequest(`/goods/label/${id}`);
};

//  根据goodsId分页获取商品列表
export const getQueryGoodsIdGoodsList = goodsId => {
  return getRequest(`/goods/goods/sku/${goodsId}/list`);
};

//  获取商品分页列表
export const getGoodsSkuListDataSeller = params => {
  return getRequest("/goods/goods/sku/list", params);
};

//  获取商品品牌分页列表
export const getUserListData = params => {
  return getRequest("/goods/brand/getByPage", params);
};
//  添加或修改品牌设置
export const insertOrUpdateBrand = params => {
  return postRequest("/goods/brand/insertOrUpdate", params);
};
// 启用品牌
export const enableBrand = (id, params) => {
  return postRequest(`/goods/brand/enable/${id}`, params);
};
// 禁用品牌
export const disableBrand = (id, params) => {
  return postRequest(`/goods/brand/disable/${id}`, params);
};
//根据分类id获取关联品牌
export const getCategoryBrandListData = (category_id, params) => {
  return getRequest(`/category/brand/${category_id}`, params);
};
//保存获取关联品牌
export const saveCategoryBrand = (category_id, params) => {
  return postRequest(`/category/brand/${category_id}`, params);
};
//保存获取关联规格
export const saveCategorySpec = (category_id, params) => {
  return postRequest(`/goods/categorySpec/${category_id}`, params);
};

//获取所有可用品牌
export const getBrandListData = params => {
  return getRequest("/goods/brand/all", params);
};
// 获取所有可用规格
export const getSpecificationList = params => {
  return getRequest("/goods/spec/all", params);
};

// 获取店铺分类列表数据
export const getAllList = id => {
  return getRequest(`/goods/label/get/${id}`);
};

//  添加商品分类
export const insertCategory = params => {
  return postRequest("/goods/category/insertCategory", params);
};
//  添加商品分类
export const updateCategory = params => {
  return postRequest("/goods/category/updateCategory", params);
};
//删除商品分类
export const delCategory = id => {
  return deleteRequest(`/goods/category/del/${id}`);
};
// 启用分类
export const enableCategory = (id, type) => {
  return postRequest(`/goods/category/enable/${id}`, type);
};
// 禁用分类
export const disableCategory = (id, type) => {
  return postRequest(`/goods/category/disable/${id}`, type);
};

//  获取商品规格分页列表
export const getSpecListData = params => {
  return getRequest("/goods/spec/list", params);
};
//  添加或修改规格设置
export const insertOrUpdateSpec = params => {
  return postRequest("/goods/spec/edit", params);
};
//根据分类id获取关联规格
export const getCategorySpecListData = (category_id, params) => {
  return getRequest(`/goods/categorySpec/${category_id}`, params);
};
//删除gUI个
export const delSpec = (id, params) => {
  return deleteRequest(`/goods/spec/del/${id}`, params);
};
//  获取商品规格值列表
// export const getSpecValuesListData = (id, params) => {
//   return getRequest(`/goods/spec-values/values/${id}`, params);
// };
//  添加商品规格值
// export const saveSpecValues = (id, params) => {
//   return postRequest(`/goods/spec-values/save/${id}`, params);
// };

// 查询某分类下的全部子分类列表
export const getGoodsCategory = parent_id => {
  return getRequest(`/goods/category/${parent_id}/all-children`);
};

//  获取商品sku分页列表
export const getGoodsSkuData = params => {
  return getRequest("/goods/goods/sku/list", params);
};

//  获取商品分页列表
export const getGoodsListData = params => {
  return getRequest("/goods/goods/list", params);
};
//  获取待审核商品分页列表
export const getAuthGoodsListData = params => {
  return getRequest("/goods/auth/list", params);
};

//  审核商品
export const authGoods = (id, params) => {
  return putRequest(`/goods/${id}/auth`, params);
};

/**
 * 发布商品 查询商品品类 分级查询（商城商品品类）
 * @param ids
 * @param params
 * @returns {Promise<any>}
 */
export const getGoodsCategoryLevelList = (ids, params) => {
  return getRequest(`/goods/category/${ids}/children`, params);
};

// 获取全部经营类目
export const getGoodsCategoryAll = () => {
  return getRequest(`/goods/category/all`);
};

// 获取当前店铺分类
export const getShopGoodsLabelListSeller = () => {
  return getRequest(`/goods/label`);
};

//查询分类绑定参数信息
export const getCategoryParamsListData = (id, params) => {
  return getRequest(`/goods/categoryParameters/${id}`, params);
};
//查询商品绑定参数信息
export const getCategoryParamsByGoodsId = (goodsId, categoryId) => {
  return getRequest(`/goods/parameters/${goodsId}/${categoryId}`);
};
//更新或者保存参数
export const insertOrUpdateParams = params => {
  return postRequest("/goods/parameters/save", params);
};
//删除参数
export const deleteParams = (id, params) => {
  return deleteRequest(`/goods/parameters/${id}`, params);
};
//更新或者保存参数组
export const insertOrUpdateParamsGroup = params => {
  return postRequest("/goods/categoryParameters/save", params);
};
//删除参数组
export const deleteParamsGroup = (id, params) => {
  return deleteRequest(`/goods/categoryParameters/${id}`, params);
};

//获取sku列表
export const getSkuPage = params => {
  return getRequest(`/goodsSku/getByPage`, params);
};

//  获取商品规格值列表
// export const getSpecValuesListSellerData = (id, params) => {
//   return getRequest(`/goods/spec-values/values/${id}`, params);
// };
//  添加商品规格值
// export const saveSpecValuesSeller = (id, params) => {
//   return postRequest(`/goods/spec-values/save/${id}`, params);
// };

//  获取商品规格分页列表
export const getSpecListSellerData = params => {
  return getRequest("/goods/spec/page", params);
};
//  添加或修改规格设置
export const insertSpecSeller = params => {
  return postRequest("/goods/spec", params);
};

//  更新商品库存
export const updateGoodsSkuStocks = params => {
  return putRequest("/goods/goods/update/stocks", params, {
    "Content-Type": "application/json"
  });
};
//  获取商品分页列表
export const getGoodsListDataSeller = params => {
  return getRequest("/goods/goods/list", params);
};
//  获取商品告警分页列表
export const getGoodsListDataByStockSeller = params => {
  return getRequest("/goods/goods/list/stock", params);
};
//  获取商品详情
export const getGoods = id => {
  return getRequest(`/goods/goods/get/${id}`);
};
// 上架商品
export const upGoods = params => {
  return putRequest(`/goods/goods/up`, params);
};
// 删除商品
export const deleteGoods = params => {
  return putRequest(`/goods/goods/delete`, params);
};
//  下架商品
export const lowGoods = params => {
  return putRequest(`/goods/goods/under`, params);
};

// 获取商品单位列表
export const getGoodsUnitList = params => {
  return getRequest(`/goods/goodsUnit`,params);
};
//根据分类id获取关联品牌
export const getCategoryBrandListDataSeller = (category_id, params) => {
  return getRequest(`/goods/category/${category_id}/brands`, params);
};

export function createGoods(params) {
  return postRequest("/goods/goods/create", params, {
    "Content-Type": "application/json"
  });
}

export function editGoods(goodsId, params) {
  return putRequest(`/goods/goods/update/${goodsId}`, params, {
    "Content-Type": "application/json"
  });
}

// 获取草稿商品分页列表
export const getDraftGoodsListData = params => {
  return getRequest("/goods/draftGoods/page", params);
};

// 获取草稿商品详情
export const getDraftGoodsDetail = id => {
  return getRequest(`/goods/draftGoods/${id}`);
};

// 保存草稿商品
export function saveDraftGoods(params) {
  return postRequest("/goods/draftGoods/save", params, {
    "Content-Type": "application/json"
  });
}

// 删除草稿商品
export const deleteDraftGoods = id => {
  return deleteRequest(`/goods/draftGoods/${id}`);
};

//查询分类绑定参数信息
export const getCategoryParamsListDataSeller = (id, params) => {
  return getRequest(`/goods/categoryParameters/${id}`, params);
};

//保存获取关联规格
export const getGoodsSpecInfoSeller = (category_id) => {
  return getRequest(`/goods/spec/${category_id}`);
};

//批量设置运费模板
export const batchShipTemplate = params => {
  return putRequest(`/goods/goods/freight`, params);
};


// 获取订单统计图表
export const getOrderChart = params => {
  return getRequest(`/statistics/order`, params);
};

// 订单统计概览
export const getOrderOverView = params => {
  return getRequest(`/statistics/order/overview`, params);
};

// 统计相关订单统计

export const statisticsOrderList = params => {
  return getRequest(`/statistics/order/order`, params);
};

// 统计相关退单统计

export const statisticsOrderRefundList = params => {
  return getRequest(`/statistics/order/refund`, params);
};

// 获取行业统计列表
export const goodsCategoryStatistics = params => {
  return getRequest(`/statistics/goods/getCategoryByPage`, params);
};

// 获取统计列表,排行前一百的数据
export const goodsStatistics = params => {
  return getRequest(`/statistics/goods`, params);
};

// 获取退款统计列表
export const refundStatistics = params => {
  return getRequest(`/statistics/refund/order/getByPage`, params);
};

// 获取退款统计金额
export const refundPriceStatistics = params => {
  return getRequest(`/statistics/refund/order/getPrice`, params);
};
