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
import { onMounted, reactive, watch } from 'vue'
import { getDatasourceType } from '@/service/data-source'
import { useI18n } from 'vue-i18n'
import type { SelectOption } from './types'

type Key = '1' | '2' | '3' | '4' | '5'
type IType = {
  type: string
  label: string
  key: string
  children: SelectOption[]
}

export const useSource = (showVirtualDataSource = false) => {
  const i18n = useI18n()
  const TYPE_MAP = {
    1: 'database',
    2: 'file',
    3: 'no_structured',
    4: 'storage',
    5: 'remote_connection'
  }
  const state = reactive({
    loading: false,
    types: [] as IType[]
  })

  const querySource = async () => {
    if (state.loading) return
    state.loading = true
    try {
      const res = (await getDatasourceType({
        showVirtualDataSource,
        source: 'WT'
      })) as {
        Key: { code: number; name: string; chineseName: string }[]
      }

      console.log(res, 'ers')
      const locales = {
        zh_CN: {} as { [key: string]: string },
        en_US: {} as { [key: string]: string }
      }
      state.types = Object.entries(res).map(([key, value]) => {
        return {
          type: 'group',
          label: i18n.t(`datasource.${TYPE_MAP[key as Key]}`),
          key: TYPE_MAP[key as Key],
          children: value.map((item) => {
            locales.zh_CN[item.name] = item.chineseName
            locales.en_US[item.name] = item.name
            return {
              label: item.name,
              value: item.name
            }
          })
        }
      })
      i18n.mergeLocaleMessage('zh_CN', locales.zh_CN)
      i18n.mergeLocaleMessage('en_US', locales.en_US)
    } finally {
      state.loading = false
    }
  }

  onMounted(() => {
    querySource()
  })

  watch(useI18n().locale, () => {
    querySource()
  })

  return { state }
}
