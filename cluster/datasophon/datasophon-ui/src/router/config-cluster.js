/*
 * @Author: mjzhu
 * @describe: 
 * @Date: 2022-06-20 11:34:29
 * @LastEditTime: 2022-07-12 10:47:51
 * @FilePath: \ddh-ui\src\router\config-cluster.js
 */
import TabsView from '@/layouts/tabs/TabsView'
import BlankView from '@/layouts/BlankView'
import PageView from '@/layouts/PageView'

// 路由配置
const options = {
  routes: [
    {
      path: '/login',
      name: '登录页',
      component: () => import('@/pages/login')
    },
    {
      path: '*',
      name: '404',
      component: () => import('@/pages/exception/404'),
    },
    {
      path: '/403',
      name: '403',
      component: () => import('@/pages/exception/403'),
    },
    {
      path: '/',
      name: '首页',
      component: TabsView,
      redirect: '/login',
      children: [
        {
          path: 'overview',
          name: '总览',
          meta: {
            icon: 'cluster'
          },
          component: PageView,
          children: [
            {
              path: 'overView',
              name: '总览',
              component: () => import('@/pages/overview/index'),
            },
          ]
        },
        {
          path: 'host-manage',
          name: '主机管理',
          meta: {
            icon: 'cluster'
          },
          component: PageView,
          children: [
            {
              path: 'host-list',
              name: '主机管理',
              component: () => import('@/pages/hostManage/index'),
            }
          ]
        },
      ]
    },
  ]
}

export default options
