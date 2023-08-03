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

import { onMounted, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import {
  useFormStructuresStore,
  StructureItem
} from '@/store/datasource'
import { dynamicFormItems } from '@/service/data-source'
import { useFormField } from '@/components/dynamic-form/use-form-field'
import { useFormRequest } from '@/components/dynamic-form/use-form-request'
import { useFormValidate } from '@/components/dynamic-form/use-form-validate'
import { useFormStructure } from '@/components/dynamic-form/use-form-structure'
import type { FormRules } from 'naive-ui'
import type { ResponseBasic } from '@/service/types'

export function useForm(type: string) {
  const { t } = useI18n()
  const router = useRouter()
  const formStructuresStore = useFormStructuresStore()

  const initialValues = {
    pluginName: type,
    datasourceName: '',
    description: ''
  }

  const state = reactive({
    detailForm: { ...initialValues },
    formName: '',
    formStructure: [] as StructureItem[],
    locales: {},
    rules: {
      name: {
        trigger: ['input'],
        validator() {
          if (!state.detailForm.datasourceName) {
            return new Error(t('datasource.datasource_name_tips'))
          }
        }
      }
    } as FormRules
  })

  const getFormItems = async (value: string) => {
    if (formStructuresStore.getItem(value)) {
      state.formStructure = formStructuresStore.getItem(value) as StructureItem[]
      return
    }

    const result: any = await dynamicFormItems(value)

    try {
      const res = JSON.parse(result)
      res.forms = res.forms.map((form: any) => ({ ...form, span: 12 }))
      Object.assign(state.detailForm, useFormField(res.forms))
      Object.assign(
        state.rules,
        useFormValidate(res.forms, state.detailForm, t)
      )
      state.locales = res.locales
      state.formStructure = useFormStructure(
        res.apis ? useFormRequest(res.apis, res.forms) : res.forms
      ) as any
    } finally {}
  }

  const changeType = (value: string) => {
    router.replace({ name: 'datasource-create', query: { type: value } })
    getFormItems(value)
  }

  const resetFieldsValue = () => {
    state.detailForm = { ...initialValues }
  }

  const setFieldsValue = (values: any) => {
    Object.assign(state.detailForm, values)
  }

  const getFieldsValue = () => state.detailForm

  onMounted(() => {
    if (type) {
      getFormItems(type)
    }
  })

  return {
    state,
    changeType,
    resetFieldsValue,
    getFieldsValue,
    setFieldsValue,
    getFormItems
  }
}
