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

import { useI18n } from 'vue-i18n'
import { reactive, ref, SetupContext } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createSyncTaskDefinition } from '@/service/sync-task-definition'
import type { FormItemRule } from 'naive-ui'
import type { Router } from 'vue-router'

export function useTaskModal(
  props: any,
  ctx: SetupContext<('cancelModal' | 'confirmModal')[]>
) {
  const { t } = useI18n()
  const router: Router = useRouter()
  const route = useRoute()

  const variables = reactive({
    taskModalFormRef: ref(),
    saving: false,
    model: {
      name: ref(''),
      description: ref(''),
      jobType: ref('DATA_REPLICA')
    },
    rules: {
      name: {
        required: true,
        trigger: ['input', 'blur'],
        validator: (rule: FormItemRule, value: string) => {
          if (!value) {
            return Error(
              t('project.synchronization_definition.task_name_validate')
            )
          }
        }
      },
      jobType: {
        required: true,
        trigger: ['change']
      }
    }
  })

  const handleValidate = async () => {
    await variables.taskModalFormRef.validate()

    if (variables.saving) return
    variables.saving = true

    try {
      await submitSyncTaskDefinition()
      variables.saving = false
    } catch (err) {
      variables.saving = false
    }
  }

  const submitSyncTaskDefinition = () => {
    createSyncTaskDefinition({
      description: variables.model.description,
      name: variables.model.name,
      jobType: variables.model.jobType
    }).then((res: any) => {
      variables.model.description = ''
      variables.model.name = ''

      ctx.emit('confirmModal', props.showModalRef)

      router.push({
        path: `/task/synchronization-definition/${res}`,
      })
    })
  }

  return {
    variables,
    handleValidate
  }
}
