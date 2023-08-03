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

import {
  NCard,
  NSpace,
  NTree,
  NButton,
  NInput,
  NSwitch,
  NDivider,
  NIcon
} from 'naive-ui'
import { computed, defineComponent, PropType, ref } from 'vue'
import { TreeOption } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import styles from './index.module.scss'
import { useUserStore } from '@/store/user'
import { SearchOutlined } from '@vicons/antd'

const props = {
  data: {
    type: Object as PropType<TreeOption[]>,
    default: []
  },
  defaultChecked: {
    type: Object as PropType<number[]>,
    default: []
  },
  globalResource: {
    type: Boolean as PropType<boolean>,
    default: false
  },
  showGlobalResource: {
    type: Boolean as PropType<boolean>,
    default: true
  },
  isResource: {
    type: Boolean as PropType<boolean>,
    default: false
  },
  row: {
    type: Object as any,
    default: {}
  }
}

const CheckboxTree = defineComponent({
  name: 'checkbox-tree',
  props,
  emits: ['cancel', 'confirm'],
  setup(props, ctx) {
    const { t } = useI18n()
    const userStore = useUserStore()

    const pattern = ref('')
    const checkedValue = ref(props.defaultChecked)
    const globalResource = ref(props.globalResource)
    const rowData = ref(props.row)

    const isAllSelected = computed(
      () =>
        checkedValue.value.length === props.data.length &&
        checkedValue.value.length
    )

    const onCancel = () => {
      ctx.emit('cancel')
    }

    const onConfirm = () => {
      ctx.emit(
        'confirm',
        props.showGlobalResource ? globalResource.value : false,
        globalResource.value ? [] : checkedValue.value
      )
    }

    const handleAllSelect = () => {
      checkedValue.value = props.data.map((option) => option.key as number)
    }

    const handleCancelSelect = () => (checkedValue.value = [])

    return () => {
      return (
        <NCard
          bordered={false}
          contentStyle={{ padding: 0 }}
          footerStyle={{ padding: 0 }}
        >
          {{
            default: () => (
              <NSpace vertical>
                {props.isResource ? (
                  <div>
                    {globalResource.value ? (
                      <NSpace
                        justify='space-between'
                        style={{ marginTop: '10px', marginBottom: '10px' }}
                      >
                        {t('resource.auth.public_resource')}
                      </NSpace>
                    ) : (
                      <NSpace
                        justify='space-between'
                        style={{ marginTop: '10px', marginBottom: '10px' }}
                      >
                        {rowData.value.projectName}
                      </NSpace>
                    )}
                  </div>
                ) : (
                  <div>
                    {props.showGlobalResource && (
                      <NSpace
                        justify='space-between'
                        style={{ marginTop: '10px', marginBottom: '10px' }}
                      >
                        {t('resource.auth.public_resource')}
                        <NSwitch
                          v-model={[globalResource.value, 'value']}
                        />
                      </NSpace>
                    )}
                  </div>
                )}
                {!globalResource.value && (
                  <NSpace vertical>
                    {props.showGlobalResource && (
                      <NDivider
                        style={{ marginTop: 0, marginBottom: '10px' }}
                      />
                    )}
                    <NButton
                      tertiary
                      size='tiny'
                      onClick={
                        isAllSelected.value
                          ? handleCancelSelect
                          : handleAllSelect
                      }
                    >
                      {isAllSelected.value
                        ? t('resource.auth.cancel_select')
                        : t('resource.auth.select_all')}
                    </NButton>
                    <NInput
                      size='small'
                      v-model={[pattern.value, 'value']}
                      placeholder={t('resource.auth.search')}
                    >
                      {{
                        suffix: () => (
                          <NIcon>
                            <SearchOutlined />
                          </NIcon>
                        )
                      }}
                    </NInput>
                    <NTree
                      data={props.data}
                      pattern={pattern.value}
                      checkable={true}
                      selectable={false}
                      showIrrelevantNodes={false}
                      class={styles['tree']}
                      v-model={[checkedValue.value, 'checked-keys']}
                      virtualScroll
                      style={{ maxHeight: '220px' }}
                    />
                  </NSpace>
                )}
              </NSpace>
            ),
            footer: () => (
              <NSpace justify='end'>
                <NButton size='tiny' onClick={onCancel}>
                  {t('resource.auth.cancel')}
                </NButton>
                <NButton size='tiny' type='primary' onClick={onConfirm}>
                  {t('resource.auth.confirm')}
                </NButton>
              </NSpace>
            )
          }}
        </NCard>
      )
    }
  }
})

export default CheckboxTree
