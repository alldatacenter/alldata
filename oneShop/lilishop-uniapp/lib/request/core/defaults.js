/**
 * 默认的全局配置
 */


export default {
  baseURL: '',
  header: {},
  method: 'GET',
  dataType: 'json',
  // #ifndef MP-ALIPAY || APP-PLUS
  responseType: 'text',
  // #endif
  custom: {},
  // #ifdef MP-ALIPAY || MP-WEIXIN
  timeout: 30000,
  // #endif
  // #ifdef APP-PLUS
  sslVerify: true,
  // #endif
  // #ifdef H5
  withCredentials: false,
  // #endif
  validateStatus: function validateStatus(status) {
    return status >= 200 && status < 300
  }
}
