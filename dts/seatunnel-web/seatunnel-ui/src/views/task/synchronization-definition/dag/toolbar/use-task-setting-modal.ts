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

import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useFormField } from '@/components/dynamic-form/use-form-field'
import { useFormRequest } from '@/components/dynamic-form/use-form-request'
import { useFormValidate } from '@/components/dynamic-form/use-form-validate'
import { useFormStructure } from '@/components/dynamic-form/use-form-structure'
import { useSynchronizationDefinitionStore } from '@/store/synchronization-definition'
import { taskDefinitionForm } from '@/service/sync-task-definition'
import { updateSyncTaskDefinition } from '@/service/sync-task-definition'
import { useRoute } from 'vue-router'
import { omit } from 'lodash'
import type { FormItemRule } from 'naive-ui'
import type { SetupContext } from 'vue'

export function useTaskSettingModal(ctx: SetupContext<'cancelModal'[]>) {
  const route = useRoute()
  const { t } = useI18n()
  const settingFormRef = ref()
  const dagStore = useSynchronizationDefinitionStore()
  const state = reactive({
    model: {
      taskName: '',
      description: '',
      engine: ref('SeaTunnel')
    },
    rules: {
      taskName: {
        required: true,
        trigger: ['input', 'blur'],
        validator: (rule: FormItemRule, value: any) => {
          if (!value) {
            return Error(
              t('project.synchronization_definition.task_name_validate')
            )
          }
        }
      }
    },
    saving: false,
    formStructure: ref([]),
    formName: ref(''),
    loading: false
  })

  const handleValidate = async () => {
    await settingFormRef.value.validate()

    if (state.saving) return
    state.saving = true

    try {
      const params = {
        name: state.model.taskName,
        description: state.model.description,
        engine: state.model.engine,
        env: omit(state.model, ['taskName', 'description', 'engine'])
      }
      await updateSyncTaskDefinition(
        route.params.jobDefinitionCode as string,
        params
      )
      dagStore.setDagInfo(params)
      ctx.emit('cancelModal')
    } finally {
      state.saving = false
    }
  }

  const settingInit = async () => {
    if (state.loading) return
    state.loading = true
    try {
      const config = dagStore.getDagInfo
      state.model.taskName = config.name
      state.model.description = config.description
      const resJson = await taskDefinitionForm()
      const res = JSON.parse(resJson)
      const forms = config.env
        ? res.forms.map((f: any) => {
            f.defaultValue = config.env[f.field]
            return f
          })
        : res.forms.map((item: any) => {
          if(item.field === "job.mode") {
            item.defaultValue = ''
          }
          return item
        })
      state.formName = res.name
      Object.assign(state.model, useFormField(forms))
      Object.assign(state.rules, useFormValidate(forms, state.model, t))
      state.formStructure = useFormStructure(
        res.apis ? useFormRequest(res.apis, forms) : forms
      ) as any
    } finally {
      state.loading = false
    }
  }

  return {
    state,
    settingFormRef,
    handleValidate,
    settingInit
  }
}
