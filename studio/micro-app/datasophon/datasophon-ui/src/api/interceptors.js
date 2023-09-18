/*
 * @Author: mjzhu
 * @Date: 2022-05-24 10:22:10
 * @LastEditTime: 2022-06-08 17:18:05
 * @FilePath: \ddh-ui\src\api\interceptors.js
 */
import axios from 'axios'

// axios请求拦截
axios.interceptors.request.use(
  config => {
    // 在请求之前操作
    if(config.method === 'get') {// 在ie中相同请求不会重复发送
      if(window.ActiveXObject || 'ActiveXObject' in window) {
        config.url = `${config.url}?${new Date().getTime()}`
      }
    }
    config.headers['Content-Type'] = config.ContentType?config.ContentType:'application/json;charset=UTF-8'
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

axios.interceptors.response.use(
  response => {
    // 对响应数据操作
    return response
  },
  error => {
    return Promise.reject(error)
  }
)
