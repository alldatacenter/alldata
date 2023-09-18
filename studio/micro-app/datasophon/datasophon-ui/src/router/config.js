import TabsView from '@/layouts/tabs/TabsView'
import BlankView from '@/layouts/BlankView'
import PageView from '@/layouts/PageView'


// 路由配置
const options = {
  routes: [{
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
    children: [{
      path: 'overview',
      name: '总览',
      meta: {
        notAlive: true,
        icon: 'over-view',
        isCluster: 'isCluster',
      },
      component: () => import('@/pages/overview/index'),
      children: []
    },
    {
      path: 'service-manage',
      name: '服务管理',
      meta: {
        icon: 'service-manage',
        isCluster: 'isCluster',
      },
      component: PageView,
      children: [{
        meta: {
          notAlive: true,
          params: {
            serviceId: ''
          }
        },
        path: 'service-list/:serviceId',
        name: '服务管理',
        component: () => import('@/pages/serviceManage/index'),
      }]
    },
    {
      path: 'colony-manage',
      name: '集群管理',
      meta: {
        icon: 'cluster',
        isCluster: '',
      },
      component: PageView,
      children: [{
        path: 'colony-list',
        meta: {
          notAlive: true,
        },
        name: '集群管理',
        component: () => import('@/pages/colonyManage/list'),
      },
      {
        path: 'colony-frame',
        name: '集群框架',
        component: () => import('@/pages/colonyManage/frame'),
      }
      ]
    },
    {
      path: 'security-center',
      name: '用户管理',
      meta: {
        icon: 'safety-certificate',
        isCluster: '',
      },
      component: PageView,
      children: [{
        path: 'user',
        name: '用户管理',
        component: () => import('@/pages/securityCenter/user'),
      },]
    },
    {
      path: 'host-manage',
      name: '主机管理',
      meta: {
        icon: 'host',
        isCluster: 'isCluster',
      },
      component: () => import('@/pages/hostManage/index'),
      children: []
    },
    {
      path: 'alarm-manage',
      name: '告警管理',
      meta: {
        icon: 'gaojing',
        isCluster: 'isCluster',
      },
      component: PageView,
      children: [{
        path: 'group',
        meta: {
          notAlive: false,
        },
        name: '告警组管理',
        label: '告警组管理',
        component: () => import('@/pages/alarmManage/group'),
      },
      {
        path: 'metric',
        meta: {
          notAlive: true,
        },
        name: '告警指标管理',
        label: '告警指标管理',
        component: () => import('@/pages/alarmManage/metric'),
      },
      // {
      //   path: 'user',
      //   name: '租户管理',
      //   label: '租户管理',
      //   component: () => import('@/pages/systemCenter/user'),
      // },
      ]
    },
    {
      path: 'system-center',
      name: '系统管理',
      label: '系统管理',
      meta: {
        icon: 'system-icon',
        isCluster: 'isCluster',
      },
      component: PageView,
      children: [{
        path: 'user',
        name: '租户管理',
        label: '租户管理',
        component: () => import('@/pages/systemCenter/user'),
      },]
    },
    ]
  },
  ]
}

export default options