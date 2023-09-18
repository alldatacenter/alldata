/*
 * @Author: mjzhu
 * @Date: 2022-05-24 10:22:10
 * @LastEditTime: 2022-06-27 17:02:37
 * @FilePath: \ddh-ui\src\main.js
 */
import Vue from 'vue'
import App from './App.vue'
import {initRouter} from './router'
import './icons/index'
import './assets/less/index.less'
import Antd from 'ant-design-vue'
import Viser from 'viser-vue'
import '@/mock'
import '@/api'
import store from './store'
import {initI18n} from '@/utils/i18n'
import 'animate.css/source/animate.css'
import Plugins from '@/plugins'
import bootstrap from '@/bootstrap'
import 'moment/locale/zh-cn'
import '@/assets/fonts/font.css'

const router = initRouter(store.state.setting.asyncRoutes)
const i18n = initI18n('CN', 'US')

Vue.use(Antd)
Vue.config.productionTip = false
Vue.use(Viser)
Vue.use(Plugins)

bootstrap({router, store, i18n, message: Vue.prototype.$message})

new Vue({
  router,
  store,
  i18n,
  render: h => h(App),
}).$mount('#app')
