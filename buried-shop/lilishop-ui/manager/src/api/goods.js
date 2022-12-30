// 统一请求路径前缀在libs/axios.js中修改
import { getRequest, postRequest, putRequest, deleteRequest} from '@/libs/axios';

//  获取商品品牌分页列表
export const getManagerBrandPage = (params) => {
    return getRequest('/goods/brand/getByPage', params)
}

// 批量删除
export const delBrand = (ids) =>{
  return deleteRequest(`/goods/brand/delByIds/${ids}`)
}


//  添加
export const addBrand = (params) => {
    return postRequest('/goods/brand', params)
}
// 修改品牌设置
export const updateBrand = (params) => {
    return putRequest(`/goods/brand/${params.id}`, params)
}
// 禁用品牌
export const disableBrand = (id, params) => {
    return putRequest(`/goods/brand/disable/${id}`, params)
}
//根据分类id获取关联品牌
export const getCategoryBrandListData = (category_id, params) => {
    return getRequest(`/goods/categoryBrand/${category_id}`, params)
}
//保存获取关联品牌
export const saveCategoryBrand = (category_id, params) => {
    return postRequest(`/goods/categoryBrand/${category_id}`, params)
}
//保存获取关联规格
export const saveCategorySpec = (category_id, params) => {
    return postRequest(`/goods/categorySpec/${category_id}`, params)
}

//获取所有可用品牌
export const getBrandListData = (params) => {
    return getRequest('/goods/brand/all', params)
}
// 获取所有可用规格
export const getSpecificationList = (params) => {
    return getRequest('/goods/spec/all', params)
}

//获取分类列表数据
export const getAllCategoryList = (parent_id) => {
  return getRequest(`/goods/category/${parent_id}/all-children`)
}

//获取分类列表数据
export const getCategoryTree = () => {
  return getRequest(`/goods/category/allChildren`)
}

//  添加商品分类
export const insertCategory = (params) => {
    return postRequest('/goods/category', params)
}
//  添加商品分类
export const updateCategory = (params) => {
    return putRequest('/goods/category', params)
}
//删除商品分类
export const delCategory = (id) => {
    return deleteRequest(`/goods/category/${id}`)
}
// 禁用分类
export const disableCategory = (id, type) => {
    return putRequest(`/goods/category/disable/${id}`, type)
}


//  获取商品规格分页列表
export const getSpecListData = (params) => {
    return getRequest('/goods/spec', params)
}
//  添加或修改规格设置
export const insertSpec = (params) => {
    return postRequest('/goods/spec', params)
}
//  添加或修改规格设置
export const updateSpec = (id,params) => {
    return putRequest(`/goods/spec/${id}`, params)
}
//根据分类id获取关联规格
export const getCategorySpecListData = (category_id, params) => {
    return getRequest(`/goods/categorySpec/${category_id}`, params)
}
//删除gUI个
export const delSpec = (id, params) => {
    return deleteRequest(`/goods/spec/${id}`, params)
}

// 查询某分类下的全部子分类列表
export const getGoodsCategory = (parent_id) => {
    return getRequest(`/goods/category/${parent_id}/all-children`)
}

//  上架商品
export const upGoods = (id, params) => {
    return putRequest(`/goods/goods/${id}/up`, params)
  }
  //  下架商品
  export const lowGoods = (id, params) => {
    return putRequest(`/goods/goods/${id}/under`, params)
  }

//  获取商品sku分页列表
export const getGoodsSkuData = (params) => {
    return getRequest('/goods/goods/sku/list', params)
}


//  获取商品分页列表
export const getGoodsListData = (params) => {
    return getRequest('/goods/goods/list', params)
}
//  获取待审核商品分页列表
export const getAuthGoodsListData = (params) => {
    return getRequest('/goods/goods/auth/list', params)
}
//  审核商品
export const authGoods = (id, params) => {
    return putRequest(`/goods/goods/${id}/auth`, params)
}

//查询分类绑定参数信息
export const getCategoryParamsListData = (id, params) => {
    return getRequest(`/goods/categoryParameters/${id}`, params)
}

//查询商品绑定参数信息
export const getCategoryParamsByGoodsId = (goodsId, categoryId) => {
    return getRequest(`/goods/parameters/${goodsId}/${categoryId}`)
}
//保存参数
export const insertGoodsParams = (params) => {
    return postRequest('/goods/parameters', params)
}
//更新参数
export const updateGoodsParams = (params) => {
    return putRequest('/goods/parameters', params)
}
//删除参数
export const deleteParams = (id, params) => {
    return deleteRequest(`/goods/parameters/${id}`, params)
}
//保存参数组
export const insertParamsGroup = (params) => {
    return postRequest('/goods/categoryParameters', params)
}
//更新参数组
export const updateParamsGroup = (params) => {
    return putRequest('/goods/categoryParameters', params)
}
//删除参数组
export const deleteParamsGroup = (id, params) => {
    return deleteRequest(`/goods/categoryParameters/${id}`, params)
}

//保存获取关联规格
export const getGoodsSpecInfo = (category_id, params) => {
    return getRequest(`/goods/categorySpec/goods/${category_id}`, params)
}


//获取sku列表
export const getSkuPage = (params) => {
  return getRequest(`/goodsSku/getByPage`, params)
}


//查看商品详情
export const getGoodsDetail = (id) => {
  return getRequest(`/goods/goods/get/${id}`)
}



// 获取订单统计图表
export const getOrderChart = (params) => {
  return getRequest(`/statistics/order`, params)
}


// 订单统计概览
export const getOrderOverView = (params) => {
  return getRequest(`/statistics/order/overview`, params)
}

// 统计相关订单统计

export const statisticsOrderList = (params) => {
  return getRequest(`/statistics/order/order`, params)
}

// 统计相关退单统计

export const statisticsOrderRefundList = (params) => {
  return getRequest(`/statistics/order/refund`, params)
}


// 获取行业统计列表
export const goodsCategoryStatistics = (params) => {
    return getRequest(`/statistics/goods/getCategoryByPage`, params)
}

// 获取统计列表,排行前一百的数据
export const goodsStatistics = (params) => {
    return getRequest(`/statistics/goods`, params)
}


// 获取退款统计列表
export const refundStatistics = (params) => {
    return getRequest(`/statistics/refund/order/getByPage`, params)
}

// 获取退款统计金额
export const refundPriceStatistics = (params) => {
    return getRequest(`/statistics/refund/order/getPrice`, params)
}
