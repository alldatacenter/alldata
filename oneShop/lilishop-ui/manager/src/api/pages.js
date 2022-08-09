// 统一请求路径前缀在libs/axios.js中修改
import {getRequest, postRequest, putRequest, deleteRequest} from '@/libs/axios';

//保存导航栏
export const save = (params) => {
    return postRequest('/siteNavigation/save', params)
}
//保存导航栏
export const update = (params) => {
    return postRequest('/siteNavigation/update', params)
}
//查询导航栏
export const getNavigationData = (params) => {
    return getRequest('/siteNavigation/getByPage', params)
}
//禁用导航栏
export const disableNavigation = (ids) => {
    return deleteRequest(`/siteNavigation/disable/${ids}`)
}
//启用导航栏
export const enableNavigation = (ids) => {
    return deleteRequest(`/siteNavigation/enable/${ids}`)
}
//删除导航栏
export const delNavigation = (ids) => {
    return deleteRequest(`/siteNavigation/remove/${ids}`)
}

//
export const saveFocus = (params) => {
    return deleteRequest('/siteNavigation/remove/${ids}', params)
}

//查询焦点图
export const getFocusData = (params) => {
    return getRequest('/focus/getByPage', params)
}
//查询焦点图
export const saveFocusData = (params) => {
    return postRequest('/focus/insertOrUpdate', params)
}
//禁用导航栏
export const disableFocus = (ids) => {
    return putRequest(`/focus/disable/${ids}`)
}
//启用导航栏
export const enableFocus = (ids) => {
    return putRequest(`/focus/enable/${ids}`)
}
//删除导航栏
export const delFocus = (ids) => {
    return deleteRequest(`/focus/delByIds/${ids}`)
}

//根据店铺编号查询页面数据（店铺编号为0则为平台）
export const getPageData = (id) => {
    return getRequest(`/page/get/${id}`)
}

//保存页面数据
export const savePageData = (params) => {
    return postRequest('/page/insertOrUpdate', params)
}
//获取分类列表数据
export const getArticleCategory = () => {
  return getRequest(`/other/articleCategory/all-children`)
}
//文章分类添加
export const saveArticleCategory = (params) => {
    return postRequest('/other/articleCategory', params)
}

//删除文章分类
export const delArticleCategory = (id) => {
    return deleteRequest(`/other/articleCategory/${id}`)
}
//修改文章分类
export const updateArticleCategory = (params, id) => {
    return putRequest(`/other/articleCategory/update/${id}`, params)
}
//文章添加
export const saveArticle = (params) => {
    return postRequest('/other/article', params, {"Content-Type": "application/json"})
}
//文章修改
export const updateArticle = (params) => {
    return putRequest(`/other/article/update/${params.id}`, params, {"Content-Type": "application/json"})
}
//文章是否展示修改
export const updateArticleStatus = (id,params) => {
  return putRequest(`/other/article/update/status/${id}`, params)
}
//查看文章
export const seeArticle = (id) => {
    return getRequest(`/other/article/${id}`)
}
//获取文章列表数据
export const getArticle = (params) => {
    return getRequest('/other/article/getByPage', params)
}



//删除文章数据
export const delArticle = (ids) => {
    return deleteRequest(`/other/article/delByIds/${ids}`)
}



//获取隐私协议数据
export const getPrivacy = (type) => {
    return getRequest(`/other/article/type/${type}`)
}
//修改隐私协议数据
export const updatePrivacy = (id,type,params) => {
    return putRequest(`/other/article/updateArticle/${type}?id=${id}`, params, {"Content-Type": "application/json"})
}