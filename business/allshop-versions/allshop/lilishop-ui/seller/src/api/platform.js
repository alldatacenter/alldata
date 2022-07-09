// 统一请求路径前缀在libs/axios.js中修改
import { getRequest, postRequest, putRequest, deleteRequest, importRequest, getRequestWithNoToken } from '@/libs/axios';

// 获取kuaidi配置
export const getParams = (params) => {
    return getRequest('/platformSetting/get/'+params)
}
// 编辑kuaidi配置
export const editParams = (params,method) => {
    return postRequest('/platformSetting/insertOrUpdate/'+method, params)
}