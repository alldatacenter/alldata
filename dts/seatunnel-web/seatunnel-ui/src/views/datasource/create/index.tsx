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
import { defineComponent, ref } from 'vue'
import {
  NSpace,
  NBreadcrumb,
  NBreadcrumbItem,
  NForm,
  useDialog,
  NInput,
  NButton,
  NFormItemGi,
  NGrid,
  NDivider,
  NCard
} from 'naive-ui'
import { DynamicFormItem } from '@/components/dynamic-form/dynamic-form-item'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useDetail } from './use-detail'
import { useForm } from './use-form'
import styles from '../index.module.scss'
import SourceModal from '../components/source-modal'

const DatasourceCreate = defineComponent({
  setup() {
    const { t } = useI18n()
    const route = useRoute()
    const router = useRouter()
    const dialog = useDialog()
    const showSourceModal = ref(false)
    const detailFormRef = ref(null)

    const { state, changeType, getFieldsValue, setFieldsValue, getFormItems } =
      useForm(route.query.type as string)

    const { status, testConnect, createOrUpdate } = useDetail(
      getFieldsValue,
      setFieldsValue,
      getFormItems,
      detailFormRef,
      route.params.id as string
    )

    const onClose = () => {
      dialog.warning({
        title: t('datasource.warning'),
        content: t('datasource.close_confirm_tips'),
        onPositiveClick: () => {
          router.push({
            name: 'datasource-list'
          })
        },
        positiveText: t('datasource.confirm'),
        negativeText: t('datasource.cancel')
      })
    }

    return () => (
      <NSpace vertical>
        <NCard>
          {{
            header: () => <NBreadcrumb>
              <NBreadcrumbItem onClick={onClose}>
                {t('datasource.datasource')}
              </NBreadcrumbItem>
              <NBreadcrumbItem>
                {t(
                  route.params.id
                    ? 'datasource.edit_datasource'
                    : 'datasource.create_datasource'
                )}
              </NBreadcrumbItem>
            </NBreadcrumb>,
            'header-extra': () => <NSpace>
              <NButton secondary type='primary' onClick={testConnect} loading={status.testing}>
                {t('datasource.test_connect')}
              </NButton>
              <NButton secondary onClick={onClose}>
                {t('datasource.cancel')}
              </NButton>
              <NButton type='success' onClick={createOrUpdate} loading={status.saving}>
                {t('datasource.confirm')}
              </NButton>
            </NSpace>
          }}
        </NCard>
        <NCard>
          <NForm
            rules={state.rules}
            ref={detailFormRef}
            class={styles['detail-content']}
          >
            <NGrid xGap={10}>
              <NFormItemGi
                label={t('datasource.datasource_type')}
                path='type'
                show-require-mark
                span={24}
              >
                <NSpace
                  class={[
                    styles.typeBox,
                    !!route.params.id && styles.disabledBox
                  ]}
                >
                  <div>{state.detailForm.pluginName}</div>
                  {!route.params.id && (
                    <NButton
                      text
                      type='primary'
                      onClick={() => void (showSourceModal.value = true)}
                    >
                      {t('datasource.select')}
                    </NButton>
                  )}
                </NSpace>
              </NFormItemGi>
              <NFormItemGi
                label={t('datasource.datasource_name')}
                path='name'
                show-require-mark
                span={12}
              >
                <NInput
                  class='input-data-source-name'
                  v-model={[state.detailForm.datasourceName, 'value']}
                  maxlength={60}
                  placeholder={t('datasource.datasource_name_tips')}
                />
              </NFormItemGi>
              <NFormItemGi
                label={t('datasource.description')}
                path='note'
                span={12}
              >
                <NInput
                  class='input-data-source-description'
                  v-model={[state.detailForm.description, 'value']}
                  type='textarea'
                  placeholder={t('datasource.description_tips')}
                  rows={1}
                />
              </NFormItemGi>
            </NGrid>
            <NDivider style={{ marginTop: '0px' }} />
            {state.formStructure.length > 0 && (
              <DynamicFormItem
                model={state.detailForm}
                formStructure={state.formStructure}
                name={state.formName}
                locales={state.locales}
              />
            )}
          </NForm>
        </NCard>
        <SourceModal
          show={showSourceModal.value}
          onChange={(type) => {
            changeType(type)
            showSourceModal.value = false
          }}
          onCancel={() => void (showSourceModal.value = false)}
        />
      </NSpace>
    )
  }
})

export default DatasourceCreate
