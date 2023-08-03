/*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import useStore from './store'
import VueI18n from './language/i18n'

import RegisterComponents from './components/register'
import './styles/index.less'
import './utils/editor'
import './assets/icons'
import SvgIcon from '@/components/svg-icon.vue'
import loginService from './services/login.service'
import { NavigationGuardNext, RouteLocationNormalized } from 'vue-router'
import { getQueryString } from './utils'

const app = createApp(App).use(createPinia())
app.component('svg-icon', SvgIcon)
app.use(VueI18n)
RegisterComponents(app);

// login
(async () => {
  try {
    const store = useStore()
    const token = getQueryString('token') || ''
    const res = await loginService.getCurUserInfo(token)
    if (res) {
      store.updateUserInfo({
        userName: res.userName
      })
    }
  } finally {
    const store = useStore()
    router.beforeEach((to: RouteLocationNormalized, from: RouteLocationNormalized, next: NavigationGuardNext) => {
      if (to.fullPath === '/login') {
        if (store.userInfo.userName) {
          return next('/')
        }
        store.setHistoryPath({
          path: from.path,
          query: { ...from.query }
        })
      }
      next()
    })
    app.use(router)

    app.mount('#app')
  }
})()
