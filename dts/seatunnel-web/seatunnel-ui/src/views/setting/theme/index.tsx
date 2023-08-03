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

import { defineComponent, computed, ref, watch } from 'vue'
import { NDropdown, NIcon, NButton, NSelect } from 'naive-ui'
import { DownOutlined } from '@vicons/antd'
import { useTheme } from './use-theme'
import styles from './index.module.scss'
import { useI18n } from 'vue-i18n'
import { useThemeStore } from '@/store/theme'
import { find } from 'lodash'
import type { ITheme } from '@/store/theme/types'

const Theme = defineComponent({
  name: 'Theme',
  setup() {
    const themeStore = useThemeStore()
    const { t } = useI18n()
    const onSelectTheme = (theme: ITheme) => {
      themeStore.setTheme(theme)
    }

    console.log(t('theme.light'), 'theme.light')
    const themeOpts = computed(() =>[
      // { label: t('theme.dark_blue'), key: 'dark-blue' },
      { label: t('theme.light'), key: 'light', value: 'light' },
      { label: t('theme.dark'), key: 'dark', value: 'dark' }
    ])

    let themeLabel = computed(() => themeStore.theme)
    return {
      themeOpts,
      themeLabel,
      onSelectTheme
    }
  },
  render() {
    return (
      <NSelect
        options={this.themeOpts}
        v-model={[this.themeLabel, 'value']}
        onUpdateValue={(v) => {
          this.onSelectTheme(v)
        }}
      />
    )
  }
})

export default Theme
