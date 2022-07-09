// 统一请求路径前缀在libs/axios.js中修改
import { getRequest, postRequest, putRequest, deleteRequest, importRequest, uploadFileRequest } from '@/libs/axios';


// 认证
export const authorize = (params) => {
    return getRequest('/oauth2/authorize', params)
}
// 认证过
export const authorized = (params) => {
    return getRequest('/oauth2/authorized', params)
}
// 获取token
export const token = (params) => {
    return getRequest('/oauth2/token', params)
}
// 获取user信息
export const user = (params) => {
    return getRequest('/oauth2/user', params)
}

// 获取随机secretKey
export const getSecretKey = (params) => {
    return getRequest('/client/getSecretKey', params)
}
// 获取客户端
export const getClientDataList = (params) => {
    return getRequest('/client/getByCondition', params)
}
// 添加客户端
export const addClient = (params) => {
    return postRequest('/client/save', params)
}
// 编辑客户端
export const updateClient = (params) => {
    return putRequest('/client/update', params)
}
// 删除客户端
export const deleteClient = (ids, params) => {
    return deleteRequest(`/client/delByIds/${ids}`, params)
}







