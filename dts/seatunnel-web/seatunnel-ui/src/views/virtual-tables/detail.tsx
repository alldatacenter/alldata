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
import { defineComponent } from 'vue'
import {
  NSpace,
  NBreadcrumb,
  NBreadcrumbItem,
  NSteps,
  NStep,
  NButton,
  NText,
  NIcon,
  NCard,
  useDialog
} from 'naive-ui'
import StepOneForm from './step-one-form'
import StepTwoForm from './step-two-form'
import StepTwoTable from './step-two-table'
import StepThreeParams from './step-three-params'
import { PlusOutlined } from '@vicons/antd'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useDetail } from './use-detail'
import styles from './index.module.scss'

const VirtualTablesDetail = defineComponent({
  name: 'VirtualTablesDetail',
  setup() {
    const { t } = useI18n()
    const route = useRoute()
    const router = useRouter()
    const dialog = useDialog()
    const {
      state,
      stepOneFormRef,
      stepTwoFormRef,
      onAddRecord,
      onChangeStep,
      createOrUpdate
    } = useDetail(route.params.id as string)
    console.log('create')
    const onClose = () => {
      dialog.warning({
        title: t('virtual_tables.warning'),
        content: t('virtual_tables.close_confirm_tips'),
        onPositiveClick: () => {
          router.push({
            name: 'virtual-tables-list',
            query: { tab: 'virtual-tables' }
          })
        },
        positiveText: t('virtual_tables.confirm'),
        negativeText: t('virtual_tables.cancel')
      })
    }

    return () => (
      <NSpace vertical>
        <NBreadcrumb>
          <NBreadcrumbItem
            // @ts-ignore
            onClick={onClose}
          >
            {t('virtual_tables.virtual_tables')}
          </NBreadcrumbItem>
          <NBreadcrumbItem>
            {t(
              route.params.id
                ? t('virtualTables.edit_virtual_tables')
                : t('virtual_tables.create_virtual_tables')
            )}
          </NBreadcrumbItem>
        </NBreadcrumb>
        <NCard
          title={t(
            route.params.id
              ? t('virtualTables.edit_virtual_tables')
              : t('virtual_tables.create_virtual_tables')
          )}
        >
          <div class={styles['detail-content']}>
            <NSteps current={state.current} class={styles['detail-step']}>
              <NStep title={t('virtual_tables.configure')} />
              <NStep title={t('virtual_tables.model')} />
              <NStep title={t('virtual_tables.complete')} />
            </NSteps>
            <div class={styles['width-100']} v-show={state.current === 1}>
              <NSpace justify='center'>
                <StepOneForm params={state.stepOne} ref={stepOneFormRef} />
              </NSpace>
            </div>
            <div class={styles['detail-step-two']} v-show={state.current === 2}>
              <StepTwoForm ref={stepTwoFormRef} />
              <div class={styles['detail-table-header']}>
                <NText class={styles['detail-table-title']}>
                  {t('virtual_tables.table_structure')}
                </NText>
                <NButton text type='primary' onClick={onAddRecord}>
                  {{
                    icon: () => (
                      <NIcon>
                        <PlusOutlined />
                      </NIcon>
                    ),
                    default: () => t('virtual_tables.add')
                  }}
                </NButton>
              </div>
              <StepTwoTable
                list={state.stepTwo.list}
                fieldTypes={state.fieldTypes}
              />
            </div>
            <div
              class={styles['detail-step-three']}
              v-show={state.current === 3}
            >
              <div class={styles['detail-step-three-params']}>
                <StepThreeParams
                  class={styles['detail-step-three-left']}
                  params={[
                    {
                      label: t('virtual_tables.source_type'),
                      value: state.stepOne.pluginName || ''
                    },
                    {
                      label: t('virtual_tables.source_name'),
                      value: state.stepOne.datasourceName || ''
                    },
                    {
                      label: t('virtual_tables.virtual_tables_name'),
                      value: state.stepOne.tableName || ''
                    }
                  ]}
                />
                <StepThreeParams
                  class={styles['detail-step-three-right']}
                  params={state.stepTwo.config}
                  cols={3}
                />
              </div>
              <StepTwoTable
                class={styles['width-100']}
                list={state.stepTwo.list}
                plain
                fieldTypes={state.fieldTypes}
              />
            </div>
          </div>
          <NSpace justify='end'>
            <NButton
              v-show={state.current !== 1}
              type='primary'
              onClick={() => void onChangeStep(-1)}
            >
              {t('virtual_tables.previous_step')}
            </NButton>
            <NButton onClick={onClose}>{t('virtual_tables.cancel')}</NButton>

            {state.current !== 3 && (
              <NButton
                type='primary'
                onClick={() => void onChangeStep(1)}
                loading={state.goNexting}
              >
                {t('virtual_tables.next_step')}
              </NButton>
            )}
            <NButton
              v-show={state.current === 3}
              onClick={createOrUpdate}
              loading={state.saving}
              type='primary'
            >
              {t('virtual_tables.confirm')}
            </NButton>
          </NSpace>
        </NCard>
      </NSpace>
    )
  }
})

export default VirtualTablesDetail
