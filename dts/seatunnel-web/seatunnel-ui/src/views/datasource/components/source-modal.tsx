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

import { defineComponent, PropType } from 'vue'
import {
  NSpace,
  NModal,
  NCard,
  NButton,
  NTabs,
  NTabPane,
  NEmpty
} from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useSource } from './use-source'
import styles from './source-model.module.scss'
import type { SelectOption } from 'naive-ui'

const props = {
  show: {
    type: Boolean as PropType<boolean>,
    default: false
  },
  id: {
    type: Number as PropType<number>
  }
}

const SourceModal = defineComponent({
  name: 'SourceModal',
  props,
  emits: ['change', 'cancel'],
  setup(props, ctx) {
    const { t } = useI18n()
    const { state } = useSource(false)
    const handleTypeSelect = (type: string) => {
      ctx.emit('change', type)
    }
    const onCancel = () => {
      ctx.emit('cancel')
    }

    return () => (
      <NModal show={props.show} onMaskClick={onCancel} onEsc={onCancel}>
        <NCard
          class={styles.content}
          title={t('datasource.choose_datasource_type')}
        >
          <NTabs>
            {state.types.map((item) => (
              <NTabPane name={item.key} tab={item.label} key={item.key}>
                <div class={styles['types']}>
                  {item?.children.map((slip: SelectOption) => (
                    <div
                      class={styles.itemBox}
                      onClick={() => handleTypeSelect(slip.label as string)}
                    >
                      <div class='font-bold'>{slip.label}</div>
                      <div class='text-xs mt-1.5'>{item.label}</div>
                    </div>
                  ))}
                </div>
                {item.children.length === 0 && <NEmpty />}
              </NTabPane>
            ))}
          </NTabs>
          <NSpace justify='end'>
            <NButton onClick={onCancel}>{t('datasource.cancel')}</NButton>
          </NSpace>
        </NCard>
      </NModal>
    )
  }
})

export default SourceModal
