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

import { onMounted, reactive, Ref } from 'vue'
import {
  datasourceDetail,
  datasourceAdd,
  datasourceUpdate,
  checkConnect
} from '@/service/data-source'
import { useI18n } from 'vue-i18n'
import { omit } from 'lodash'
import { useRouter } from 'vue-router'

export function useDetail(
  getFieldsValue: Function,
  setFieldsValue: Function,
  getFormItems: Function,
  detailFormRef: Ref,
  id: string
) {
  const { t } = useI18n()
  const router = useRouter()
  const status = reactive({
    saving: false,
    testing: false,
    loading: false
  })

  const formatParams = () => {
    const values = getFieldsValue()
    return {
      datasourceName: values.datasourceName,
      pluginName: values.pluginName,
      description: values.description,
      datasourceConfig: JSON.stringify(
        omit(values, ['pluginName', 'datasourceName', 'description'])
      )
    }
  }

  const queryById = async () => {
    try {
      const result = await datasourceDetail(id)
      await getFormItems(result.pluginName)
      setFieldsValue({
        datasourceName: result.datasourceName,
        pluginName: result.pluginName,
        description: result.description,
        ...result.datasourceConfig
      })
    } finally {}
  }

  const testConnect = async () => {
    await detailFormRef.value.validate()
    if (status.testing) return
    status.testing = true
    const values = getFieldsValue()
    try {
      const result = await checkConnect({
        pluginName: values.pluginName,
        datasourceConfig: omit(values, ['pluginName', 'datasourceName', 'description'])
      })
      window.$message.success(
        result.msg ? result.msg : `${t('datasource.test_connect_success')}`
      )

      status.testing = false
    } catch (err) {
      status.testing = false
    }
  }

  const createOrUpdate = async () => {
    await detailFormRef.value.validate()

    if (status.saving) return false
    status.saving = true

    try {
      id
        ? await datasourceUpdate(formatParams(), id)
        : await datasourceAdd(formatParams())

      status.saving = false
      router.push({
        name: 'datasource-list',
        query: {}
      })
      return true
    } catch (err) {
      status.saving = false
      return false
    }
  }

  onMounted(() => {
    if (id) {
      queryById()
    }
  })

  return { status, testConnect, createOrUpdate }
}
