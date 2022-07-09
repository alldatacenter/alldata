// 统一请求路径前缀在libs/axios.js中修改
import {getRequest} from '@/libs/axios';


// 传给后台citycode 获取城市街道等id
export const handleRegion = (params) => {
  return getRequest(`/address/region`,params)
}


