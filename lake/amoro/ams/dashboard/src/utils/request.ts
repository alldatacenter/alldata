/*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

import axios, { AxiosRequestConfig, AxiosResponse } from 'axios'
import { message } from 'ant-design-vue'
import useStore from '@/store'
import router from '@/router'

// http://10.196.98.23:19111/
export const baseURL = '/'

// Used to store request identification information whose current status is pending
export const pendingRequest: {
  name: string
  // eslint-disable-next-line @typescript-eslint/ban-types
  cancel: Function // request cancel function
  routeChangeCancel: boolean // Whether to cancel the request when the route is switched
}[] = []

interface CustomAxiosRequestConfig extends AxiosRequestConfig {
  useToken?: boolean // false
  handleError?: boolean // true，By default, the message pop-up window will prompt an error，and reject err，Therefore, each request needs to be actively caught by catch
  returnCode?: boolean // false
  requestMark?: string // cancel the request
  supportCancel?: boolean // false
  routeChangeCancel?: boolean // false
}

/**
 * Request option not to add public parameters for now
 */
function addToken(config: CustomAxiosRequestConfig) {
  if (!config.method) {
    config.method = 'get'
  }

  // const userName = store.userInfo.userName
  // const searchList = []
  // if (userName) {
  //   searchList.push('userName=' + userName)
  // }
  // if (searchList.length) {
  //   config.url += ((config.url || '').indexOf('?') === -1 ? '?' : '&') + searchList.join('&')
  // }
}

const DEFAULT_CONFIG = {
  baseURL: baseURL,
  timeout: 45000,
  headers: {
    'Content-Type': 'application/json'
  }
}

const DEFAULT_OPTIONS = {
  method: 'get',
  useToken: false,
  handleError: true,
  returnCode: false,
  supportCancel: false,
  routeChangeCancel: false
}

const instance = axios.create(DEFAULT_CONFIG)

/**
 * Cleared in pendingRequest queue when request is completed or cancelled
 * @param config config
 */
const clearRequest = (config: CustomAxiosRequestConfig) => {
  const markIndex = pendingRequest.findIndex((r) => r.name === config.requestMark)
  markIndex > -1 && pendingRequest.splice(markIndex, 1)
}

instance.interceptors.response.use(
  async (response: AxiosResponse): Promise<AxiosResponse<any>> => {
    clearRequest(response.config as CustomAxiosRequestConfig)
    if (response.status === 200 && response.data) {
      return response
    } else {
      return Promise.reject(new Error('网络错误'))
    }
  },
  (error) => {
    if (error?.response) {
      clearRequest(error?.response?.config as CustomAxiosRequestConfig)
    }
    // Actively cancel the request without throwing an error
    if (axios.isCancel(error)) {
      return new Promise(() => {}) // Returns a Promise object in the "pending" state, if it is not passed down, it may cause memory leaks
    }
    return Promise.reject(error)
  }
)

interface WrapperRequest {
  (options: CustomAxiosRequestConfig): Promise<any>
  get(url: string, options?: CustomAxiosRequestConfig): Promise<any>
  delete(url: string, options?: CustomAxiosRequestConfig): Promise<any>
  post(url: string, data?: Record<string, any>, options?: CustomAxiosRequestConfig): Promise<any>
  put(url: string, data?: Record<string, any>, options?: CustomAxiosRequestConfig): Promise<any>
}

const request = function (options: CustomAxiosRequestConfig) {
  const requestConfig = Object.assign({}, DEFAULT_OPTIONS, options)
  const requestMark = `${requestConfig.method}-${(requestConfig.url || '').split('?')[0]}`
  requestConfig.requestMark = requestMark
  // Whether to cancel the last repeat or similar request
  if (requestConfig.supportCancel) {
    const markIndex = pendingRequest.findIndex((r) => r.name === requestMark)
    if (markIndex > -1) {
      pendingRequest[markIndex].cancel() // Cancel the last repeat request
      pendingRequest.splice(markIndex, 1) // delete request id
    }
  }
  // Add the current request to the pendingRequest queue
  const source = axios.CancelToken.source()
  requestConfig.cancelToken = source.token
  pendingRequest.push({
    name: requestMark,
    cancel: source.cancel,
    routeChangeCancel: requestConfig.routeChangeCancel
  })

  // Add global parameters
  if (requestConfig.useToken) {
    addToken(requestConfig)
  }

  return instance(requestConfig)
    .then((res) => {
      const { code, message: msg } = res.data
      // return code
      if (requestConfig.returnCode) {
        return res.data
      }
      // By default only result is returned
      if (code === 0 || code === 200) {
        return res.data.result
      }
      // not login
      if (code === 403) {
        const store = useStore()
        store.updateUserInfo({
          userName: ''
        })
        if (requestConfig.handleError) {
          message.error(msg || 'need login')
        }
        return router.push({
          path: '/login'
        })
      }
      return Promise.reject(new Error(msg || 'error'))
    })
    .catch((err) => {
      // global error handling
      if (requestConfig.handleError) {
        message.error(err.message)
      }
      return Promise.reject(err) // Business custom handling errors
    })
}
;['get', 'delete'].forEach((method) => {
  // @ts-ignore
  request[method] = function (url: string, options: CustomAxiosRequestConfig = {}) {
    options = Object.assign({}, options, {
      url,
      method
    })
    return request(options)
  }
})
;['post', 'put'].forEach((method) => {
  // @ts-ignore
  request[method] = function (
    url: string,
    data: Record<string, any> = {},
    options: CustomAxiosRequestConfig = {}
  ) {
    options = Object.assign({}, options, {
      url,
      method,
      data
    })
    return request(options)
  }
})

/**
 * file download
 * @param url
 * @param _blank
 */
export function download(url: string, _blank = true) {
  if (_blank) {
    window.open(url)
  } else {
    const a = document.createElement('a')
    a.style.display = 'none'
    a.href = url
    a.download = ''
    document.body.appendChild(a)
    a.click()
    setTimeout(() => {
      document.body.removeChild(a)
    })
  }
}

export default request as WrapperRequest
