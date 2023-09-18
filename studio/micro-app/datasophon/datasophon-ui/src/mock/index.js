/*
 * @Author: mjzhu
 * @Date: 2022-05-24 10:28:22
 * @LastEditTime: 2022-06-10 15:24:57
 * @FilePath: \ddh-ui\src\mock\index.js
 */
import Mock from 'mockjs'
import '@/mock/user/current'
import '@/mock/project'
import '@/mock/user/login'
import '@/mock/workplace'
import '@/mock/user/routes'
import '@/mock/goods'

// 设置全局延时
Mock.setup({
  timeout: '200-400'
})
