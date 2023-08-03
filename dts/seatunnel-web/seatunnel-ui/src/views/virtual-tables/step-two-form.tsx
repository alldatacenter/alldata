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
import { defineComponent, reactive, ref } from 'vue'
import { NForm } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { DynamicFormItem } from '@/components/dynamic-form/dynamic-form-item'
import { StructureItem } from '@/store/datasource/form-structures'
import { getDynamicConfig } from '@/service/virtual-table'
import { useFormField } from '@/components/dynamic-form/use-form-field'
import { useFormRequest } from '@/components/dynamic-form/use-form-request'
import { useFormValidate } from '@/components/dynamic-form/use-form-validate'
import { useFormStructure } from '@/components/dynamic-form/use-form-structure'

const StepTwoForm = defineComponent({
  name: 'StepTwoForm',
  setup(props, { expose }) {
    const { t } = useI18n()
    const stepTwoFormRef = ref()

    const state = reactive({
      rules: {},
      formStructure: [] as StructureItem[],
      locales: {} as any,
      formName: 'step-two-form',
      detailForm: {} as { [key: string]: string },
      config: [] as { key: string; value: string; label: string }[]
    })

    const getFormItems = async (pluginName: string, datasourceName: string) => {
      if (!pluginName || !datasourceName) return false
      const result = await getDynamicConfig({
        pluginName,
        datasourceName
      })
      try {
        const res = JSON.parse(result)

        state.locales = res.locales
        Object.assign(state.detailForm, useFormField(res.forms))
        Object.assign(
          state.rules,
          useFormValidate(res.forms, state.detailForm, t)
        )
        state.formStructure = useFormStructure(
          res.apis ? useFormRequest(res.apis, res.forms) : res.forms
        ) as any

        state.formStructure = res.forms.map((item: any) => ({
          ...item,
          span: 8
        }))
        return true
      } catch (err) {
        return false
      }
    }

    const getValues = async () => {
      await stepTwoFormRef.value.validate()
      return state.formStructure.map((item) => {
        return {
          label: item.label,
          key: item.field,
          value: state.detailForm[item.field]
        }
      })
    }

    const setValues = (values: { [key: string]: string }) => {
      Object.assign(state.detailForm, values)
    }

    expose({
      validate: async () => {
        await stepTwoFormRef.value.validate()
      },
      getFormItems,
      getValues,
      setValues
    })

    return () => (
      <NForm
        rules={state.rules}
        ref={stepTwoFormRef}
        require-mark-placement='left'
        model={state.detailForm}
        labelWidth={100}
      >
        {state.formStructure.length > 0 && (
          <DynamicFormItem
            model={state.detailForm}
            formStructure={state.formStructure}
            name={state.formName}
            locales={state.locales}
          />
        )}
      </NForm>
    )
  }
})

export default StepTwoForm
