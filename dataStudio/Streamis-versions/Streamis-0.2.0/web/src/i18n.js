/*
 * Copyright 2019 WeBank
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import Vue from 'vue';
import VueI18n from 'vue-i18n'
import { merge } from 'lodash'
import en from 'view-design/dist/locale/en-US'
import zh from 'view-design/dist/locale/zh-CN'

import dssI18n from './common/i18n'
import scriptisI18n from './apps/scriptis/i18n'
import workflowsI18n from './apps/workflows/i18n'
import streamisI18n from './apps/streamis/i18n'

Vue.use(VueI18n);
// 先判断是否有设置语言，没有就用本地语言
if (localStorage.getItem('locale')) {
  Vue.config.lang = localStorage.getItem('locale');
} else {
  const lang = navigator.language;
  if (lang === 'zh-CN') {
    Vue.config.lang = 'zh-CN';
    localStorage.setItem('locale', 'zh-CN');
  } else {
    Vue.config.lang = 'en';
    localStorage.setItem('locale', 'en');
  }
}
Vue.locale = () => {};

const messages = {en, 'zh-CN': zh};

[dssI18n, scriptisI18n, workflowsI18n, streamisI18n].forEach(item=>{
  merge(messages.en, item.en);
  merge(messages['zh-CN'], item['zh-CN']);
})

export default new VueI18n({
  locale: Vue.config.lang,
  messages
});
