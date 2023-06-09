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

import { createI18n } from 'vue-i18n'
import messages from './index'
import { datetimeFormats } from './datetimeFormats'

const i18n = createI18n({
  locale: 'en', // set locale
  fallbackLocale: 'zh', // set fallback locale
  globalInjection: true, // global injection
  legacy: false, // Using the Composition API pattern
  messages, // Set locale information
  // @ts-ignore
  datetimeFormats
})

export default i18n
