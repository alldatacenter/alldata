/*
 * @Author: mjzhu
 * @Date: 2022-05-24 10:22:10
 * @LastEditTime: 2022-06-10 15:15:37
 * @FilePath: \ddh-ui\src\api\baseUrl.js
 */
export default {
  path() {
    let path = process.env.VUE_APP_API_BASE_URL
    console.log(path, 'path地址', process.env.NOOE_ENV)
    if(process.env.NOOE_ENV === 'production') {
      console.log(1)
    }else {
      path = ''
    }

    return path
  }
}
