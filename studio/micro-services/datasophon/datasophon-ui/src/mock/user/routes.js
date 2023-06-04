/*
 * @Author: mjzhu
 * @Date: 2022-05-24 10:28:22
 * @LastEditTime: 2022-06-10 15:24:35
 * @FilePath: \ddh-ui\src\mock\user\routes.js
 */
import Mock from 'mockjs'

Mock.mock(`${process.env.VUE_APP_API_BASE_URL}/routes`, 'get', () => {
  let result = {}
  result.code = 200
  result.data = [{
    router: 'root',
    children: [
      {
        router: 'dashboard',
        children: ['workplace', 'analysis'],
      },
      {
        router: 'form',
        children: ['basicForm', 'stepForm', 'advanceForm']
      },
      {
        router: 'basicForm',
        name: '验权表单',
        icon: 'file-excel',
        authority: 'queryForm'
      },
      {
        router: 'antdv',
        path: 'antdv',
        name: 'Ant Design Vue',
        icon: 'ant-design',
        link: 'https://www.antdv.com/docs/vue/introduce-cn/'
      },
      {
        router: 'document',
        path: 'document',
        name: '使用文档',
        icon: 'file-word',
        link: 'https://iczer.gitee.io/vue-antd-admin-docs/'
      }
    ]
  }]
  return result
})
