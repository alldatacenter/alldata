// 统一请求路径前缀在libs/axios.js中修改
import { getRequest, postRequest } from '@/libs/axios';

// 验证码渲染图片接口
export const drawCodeImage = "/lili/common/captcha/draw/"


// 获取kuaidi配置
export const getParams = (params) => {
    return getRequest('/platformSetting/get/'+params)
}
// 编辑kuaidi配置
export const editParams = (params,method) => {
    return postRequest('/platformSetting/insertOrUpdate/'+method, params)
}
