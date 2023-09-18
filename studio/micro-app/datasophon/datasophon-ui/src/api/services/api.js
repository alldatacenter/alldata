/*
 * @Author: mjzhu
 * @Date: 2022-05-24 10:28:22
 * @LastEditTime: 2022-06-08 15:03:48
 * @FilePath: \ddh-ui\src\api\services\api.js
 */
//跨域代理前缀
// const API_PROXY_PREFIX='/api'
// const BASE_URL = process.env.NODE_ENV === 'production' ? process.env.VUE_APP_API_BASE_URL : API_PROXY_PREFIX
const BASE_URL = process.env.VUE_APP_API_BASE_URL
module.exports = {
  LOGIN: `${BASE_URL}/ddh/login`,
  ROUTES: `${BASE_URL}/routes`,
  GOODS: `${BASE_URL}/goods`,
  GOODS_COLUMNS: `${BASE_URL}/columns`,
}
