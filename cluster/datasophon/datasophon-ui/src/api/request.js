/*
 * @Author: mjzhu
 * @Date: 2022-05-24 10:22:10
 * @LastEditTime: 2022-06-10 14:24:26
 * @FilePath: \ddh-ui\src\api\request.js
 */
import Vue from 'vue'
import axios from 'axios'
// import '@/api/interceptors'

// post数据处理
const handleParams = function(data) {
  const params = new FormData()
  for(var key in data) {
    params.append(key, data[key])
  }
  return params
}

const axiosGet = function(url, params = {}) {
  return new Promise((resolve, reject) => {
    axios({
      method: 'get',
      url: url,
      params: params,
    }).then(res => {
      resolve(res.data)
    }).catch(error => {
      reject(error)
    })
  })
}

const axiosPost = function(url, params = {}) {
  return new Promise((resolve, reject) => {
    axios({
      method: 'post',
      url: url,
      data: handleParams(params),
      ContentType:"application/json;charset=UTF-8"
    }).then(res => {
      resolve(res.data)
    }).catch(error => {
      reject(error)
    })
  })
}
const axiosJsonPost = function(url, params = {}) {
  return new Promise((resolve, reject) => {
    axios({
      method: 'post',
      url: url,
      data: params,
    }).then(res => {
      resolve(res.data)
    }).catch(error => {
      reject(error)
    })
  })
}

// 文件上传，params为form-data
const axiosPostUpload = function(url, params = {}) {
  return new Promise((resolve, reject) => {
    axios({
      method: 'post',
      url: url,
      data: params
    }).then(res => {
      resolve(res.data)
    }).catch(error => {
      reject(error)
    })
  })
}

Vue.prototype.$axiosGet = axiosGet// get请求
Vue.prototype.$axiosPost = axiosPost// post请求
Vue.prototype.$axiosPostUpload = axiosPostUpload// 文件上传-post请求
Vue.prototype.$axiosJsonPost = axiosJsonPost// jsonpost请求
