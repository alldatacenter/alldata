import Vue from 'vue'
import VueRouter from 'vue-router'
// 导入组件的时候只需要指定父级目录就行了，因为在父级的index.ts里面已经将全部的组件export出去了
import { Login, Home, Detail, Search } from "@/views"

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'login',
    component: Login
    // () => import(/* webpackChunkName: "about" */ '@/components/Login/Login')
  },
  {
    path: '/home',
    name: 'home',
    component: Home
  },
  {
    path: '/detail',
    name: 'detail',
    component: Detail
  },
  {
    path: '/search',
    name: 'search',
    component: Search
  }
]

const router = new VueRouter({
 routes
})

export default router
