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

import { h, ref, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { EditOutlined } from '@vicons/antd'
import { NButton, NSpace } from 'naive-ui'
//import type { TableColumns, VirtualTableRecord } from '../types'

export function useColumns(onCallback: Function) {
  const { t } = useI18n()
  const columns = ref()
  const getColumns = () => {
    const columns = [
      {
        title: 'ID',
        key: 'id',
        render: (ignore: any, index: number) => index + 1
      },
      {
        title: t('virtual_tables.table_name'),
        key: 'tableName',
        align: 'left'
      },
      {
        title: t('virtual_tables.database_name'),
        key: 'databaseName',
        align: 'left'
      },
      {
        title: t('virtual_tables.source_name'),
        key: 'datasourceName',
        align: 'left'
      },
      {
        title: t('virtual_tables.source_type'),
        key: 'pluginName'
      },
      {
        title: t('virtual_tables.creator'),
        key: 'createUserName'
      },
      {
        title: t('virtual_tables.creation_time'),
        key: 'createTime'
        //render: (rowData: VirtualTableRecord) =>
        //  renderTableTime(rowData.createTime)
      },
      {
        title: t('virtual_tables.updater'),
        key: 'updateUserName'
      },
      {
        title: t('virtual_tables.update_time'),
        key: 'updateTime'
        //render: (rowData: VirtualTableRecord) =>
        //  renderTableTime(rowData.createTime)
      },
      {
        title: t('virtual_tables.operation'),
        key: 'operation',
        render: (row: any) =>
          h(NSpace, null, {
            default: () => [
              h(
                NButton,
                {
                  text: true,
                  onClick: () => void onCallback(row.tableId, 'edit')
                },
                {
                  default: () => t('virtual_tables.edit')
                }
              ),
              h(
                NButton,
                {
                  text: true,
                  onClick: () => void onCallback(row.tableId, 'delete')
                },
                { default: () => t('virtual_tables.delete') }
              )
            ]
          })
      }
    ]
    return columns
  }

  watch(useI18n().locale, () => {
    columns.value = getColumns()
  })

  onMounted(() => {
    columns.value = getColumns()
  })

  return {
    columns
  }
}
