/*
 * @Author: mjzhu
 * @describe: 
 * @Date: 2022-05-24 10:22:10
 * @LastEditTime: 2022-07-01 15:16:23
 * @FilePath: \ddh-ui\src\api\index.js
 */
import api from '@/api/httpApi/index.js'
import '@/api/request'


global.API = api// 挂在全局接口
global.intervalTime = 5000
