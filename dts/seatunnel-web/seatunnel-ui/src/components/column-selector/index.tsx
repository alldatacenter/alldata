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

import { NButton, NCheckboxGroup, NDropdown } from 'naive-ui'
import { defineComponent, PropType, ref, watch } from 'vue'
import {
  filterColumns,
  getColumnsBytable,
  getCheckboxList
} from './dealColumns'
import { getColumns, setColumns } from './dealColumns'
import './index.scss'
import { useI18n } from 'vue-i18n'

const props = {
  tableKey: {
    default: '',
    type: String
  },
  tableColumns: {
    default: [],
    type: Array as PropType<Array<any>>
  }
}
export default defineComponent({
  name: 'columnSelector',
  props,
  emits: ['changeOptions'],
  setup(props, ctx) {
    const selector = ref([]) as any
    const ff = ref(null)
    const { t } = useI18n()
    const showDropdown = ref(false)
    const columns = ref<(string | number)[] | null>(null)
    const localColumn = getColumns(props.tableKey)
    const storeColumn = ref<(string | number)[] | null>(null)
    watch(props, () => {
      selector.value = getColumnsBytable(props.tableColumns, t)
      if (localColumn && localColumn.length > 0) {
        columns.value = getCheckboxList(selector.value, localColumn, false)
      } else {
        columns.value = getCheckboxList(selector.value, [], true)
      }
      storeColumn.value = [...columns.value]
      const resColumn = filterColumns(columns.value, props.tableColumns)
      ctx.emit('changeOptions', resColumn)
    })

    const handleClick = (show: boolean) => {
      if (!show) {
        const resColumn = filterColumns(columns.value, props.tableColumns)
        ctx.emit('changeOptions', resColumn)
        setColumns(props.tableKey, resColumn)
      }
    }

    const handleUpdateValue = (value: Array<string | number>) => {
      const length = getCheckboxList(selector.value, [], true).length - 1
      if (value.indexOf('ALL') == -1 && value.length == length) {
        if (storeColumn.value && storeColumn.value.indexOf('ALL') > -1) {
          columns.value = []
          storeColumn.value = []
        } else {
          columns.value = getCheckboxList(selector.value, [], true)
          storeColumn.value = getCheckboxList(selector.value, [], true)
        }
      } else if (value.indexOf('ALL') == -1 && value.length < length) {
        columns.value = value.filter((item) => {
          return item !== 'ALL'
        })
      } else if (value.indexOf('ALL') > -1 && value.length == length + 1) {
        columns.value = []
        storeColumn.value = []
      } else if (value.indexOf('ALL') > -1 && value.length == 1) {
        columns.value = getCheckboxList(selector.value, [], true)
        storeColumn.value = getCheckboxList(selector.value, [], true)
      } else if (value.indexOf('ALL') > -1 && value.length == length) {
        if (storeColumn.value && storeColumn.value.indexOf('ALL') > -1) {
          columns.value = value.filter((item) => {
            return item !== 'ALL'
          })
          storeColumn.value = [
            ...value.filter((item) => {
              return item !== 'ALL'
            })
          ]
        }
      } else if (value.indexOf('ALL') > -1 && value.length < length) {
        columns.value = value.filter((item) => {
          return item !== 'ALL'
        })
        storeColumn.value = [
          ...value.filter((item) => {
            return item !== 'ALL'
          })
        ]
        if (storeColumn.value && storeColumn.value.indexOf('ALL') == -1) {
          columns.value = getCheckboxList(selector.value, [], true)
          storeColumn.value = getCheckboxList(selector.value, [], true)
        }
      }
    }

    return {
      selector,
      ff,
      showDropdown,
      handleClick,
      columns,
      handleUpdateValue,
      t
    }
  },
  render() {
    const { t } = this
    return (
      <div>
        <NCheckboxGroup
          v-model:value={this.columns}
          onUpdate:value={this.handleUpdateValue}
        >
          <NDropdown options={this.selector} onUpdateShow={this.handleClick}>
            <NButton class={'column-style'}>{t('project.column')}</NButton>
          </NDropdown>
        </NCheckboxGroup>
      </div>
    )
  }
})
