/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useThemeStore } from '@/store/theme'
import { find } from 'lodash'
import type { ITheme } from '@/store/theme/types'

export function useTheme() {
  const { t } = useI18n()
  const themeStore = useThemeStore()
  
  const getThemes = () => [
    // { label: t('theme.dark_blue'), key: 'dark-blue' },
    { label: t('theme.light'), key: 'light', value: 'light' },
    { label: t('theme.dark'), key: 'dark', value: 'dark' }
  ]

  let themeLabel = computed(() => themeStore.theme)
  const themes = ref(getThemes())
  // const currentThemeLabel = ref(getThemeLabel())

  const onSelectTheme = (theme: ITheme) => {
    console.log(111)
    themeStore.setTheme(theme)
    // currentThemeLabel.value = getThemeLabel()
  }

  watch(useI18n().locale, () => {
    themes.value = getThemes()
    // currentThemeLabel.value = getThemeLabel()
  })

  return { themes, themeLabel, onSelectTheme }
}
