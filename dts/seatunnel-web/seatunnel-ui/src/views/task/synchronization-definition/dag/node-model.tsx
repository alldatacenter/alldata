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
  NDataTable,
  NEmpty,
  NGrid,
  NGridItem,
  NEllipsis
} from 'naive-ui'
import { useNodeModel } from './use-model'
import { useI18n } from 'vue-i18n'
import styles from './node-mode-model.module.scss'

const NodeModeModal = defineComponent({
  name: 'NodeModeModal',
  props: {
    type: {
      type: String,
      default: 'source'
    },
    transformType: {
      type: String,
      default: ''
    },
    predecessorsNodeId: {
      type: String,
      default: ''
    },
    currentNodeId: {
      type: String,
      default: ''
    },
    schemaError: {
      type: Object,
      default: {}
    },
    refForm: {
      type: Object,
      default: null
    }
  },
  setup(props, { expose }) {
    const { t } = useI18n()
    const { state, onInit, onSwitchTable, onUpdatedCheckedRowKeys } =
      useNodeModel(props.type, props.transformType, props.predecessorsNodeId, props.schemaError, props.currentNodeId, props.refForm)

    expose({
      getOutputSchema: () => ({
        allTableData: state.allTableData,
        outputTableData: state.outputTableData,
        inputTableData: state.inputTableData
      }),
      getSelectFields: () => ({
        tableFields: state.selectedKeys,
        all: state.selectedKeys.length === state.inputTableData.length
      }),
      setSelectFields: (selectedKeys: string[]) =>
        (state.selectedKeys = selectedKeys),
      initData: (info: any) => {
        onInit(info)
      }
    })

    return () => (
      <NGrid xGap={6}>
        <NGridItem
          span={6}
          class={styles['list-container']}
        >
          <NSpace vertical>
            <h3>{t('project.synchronization_definition.table_name')}</h3>
            <dl v-show={state.tables.length}>
              {state.tables.map((table) => (
                <dd
                  class={
                    table === state.currentTable ? styles['dd-active'] : ''
                  }
                  onClick={() => void onSwitchTable(table)}
                >
                  <NEllipsis>{table}</NEllipsis>
                </dd>
              ))}
            </dl>
            {state.tables.length === 0 && <NEmpty />}
          </NSpace>
        </NGridItem>
        <NGridItem span={props.type === 'sink' ? 18 : 9}>
          <NSpace vertical>
            <h3>
              {t('project.synchronization_definition.input_table_structure')}
            </h3>
            <NDataTable
              size='small'
              row-class-name={styles['adjust-th-height']}
              columns={state.inputColumns}
              data={state.inputTableData}
              onUpdateCheckedRowKeys={onUpdatedCheckedRowKeys}
              rowKey={(row) => row.name}
              checkedRowKeys={state.selectedKeys}
              scrollX={state.inputTableWidth}
            />
          </NSpace>
        </NGridItem>
        {
          props.type !== 'sink' && <NGridItem span={9}>
            <NSpace vertical>
              <h3>
                {t('project.synchronization_definition.output_table_structure')}
              </h3>
              <NDataTable
                size='small'
                row-class-name={styles['adjust-th-height']}
                columns={state.outputColumns}
                data={state.outputTableData}
                scrollX={state.outputTableWidth}
              />
            </NSpace>
          </NGridItem>
        }
      </NGrid>
    )
  }
})

export default NodeModeModal
