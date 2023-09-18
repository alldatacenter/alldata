// 视图组件
const view = {
  tabs: () => import('@/layouts/tabs'),
  blank: () => import('@/layouts/BlankView'),
  page: () => import('@/layouts/PageView')
}

// 路由组件注册
const routerMap = {
  login: {
    authority: '*',
    path: '/login',
    component: () => import('@/pages/login')
  },
  root: {
    path: '/',
    name: '首页',
    redirect: '/login',
    component: view.tabs
  },
  colonyManage: {
    name: '集群管理',
    icon: 'table',
    component: view.page
  },
  clusterManage: {
    path: 'cluster-list',
    name: '集群管理',
    component: () => import('@/pages/colonyManage/clusterList/index')
  },
  colonyList: {
    path: 'colony-list',
    name: '集群管理',
    component: () => import('@/pages/colonyManage/list')
  },
  colonyFrame: {
    path: 'colony-frame',
    name: '集群框架',
    component: () => import('@/pages/colonyManage/frame')
  },
  success: {
    name: '成功',
    component: () => import('@/pages/result/Success')
  },
  error: {
    name: '失败',
    component: () => import('@/pages/result/Error')
  },
  exception: {
    name: '异常页',
    icon: 'warning',
    component: view.blank
  },
  exp403: {
    authority: '*',
    name: 'exp403',
    path: '403',
    component: () => import('@/pages/exception/403')
  },
  exp404: {
    name: 'exp404',
    path: '404',
    component: () => import('@/pages/exception/404')
  },
  exp500: {
    name: 'exp500',
    path: '500',
    component: () => import('@/pages/exception/500')
  }
}
export default routerMap

