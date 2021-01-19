import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import * as Element from 'element-ui'
import { MessageBox } from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css'
import axios from 'axios'
import VueAxios from 'vue-axios'

Vue.use(Element);
Vue.use(VueAxios, axios);

Vue.config.productionTip = false

// 路由拦截器
// to为向后走的路由对象，包括路由的完整信息
// from为从哪跳来的路由对象
// next()控制路由向下走，重新定义路由跳转的路由next(‘路由路径)

/** 验证用户是否登录 **/
router.beforeEach((to, from, next) => {
  // 若是要前往登录页或者用户已经登录则放行
  if (localStorage.getItem('user') || to.name == 'login') {
    next()
  } else {
    // 否则将用户转发到登录页
    MessageBox({
      title: '提示',
      message: '请先登录',
      type: 'warning',
      showConfirmButton: true
    })
    next({
      path: '/'
    })
  }
})

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
