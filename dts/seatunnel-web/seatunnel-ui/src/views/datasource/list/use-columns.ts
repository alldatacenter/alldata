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

import { h } from 'vue'
import { useI18n } from 'vue-i18n'
import { NPopover, NButton, NSpace } from 'naive-ui'
import JsonHighlight from '../components/json-highlight'
import { getTableColumn } from '@/common/table'

import { useTableOperation } from '@/hooks'
import { EditOutlined } from '@vicons/antd'
import ResourceAuth from '@/components/resource-auth'

export function useColumns(onCallback: Function) {
  const { t } = useI18n()
  const getColumns = () => {
    return [
      ...getTableColumn([{ key: 'id', title: t('datasource.id') }]),
      {
        title: t('datasource.datasource_name'),
        key: 'datasourceName'
      },
      {
        title: t('datasource.datasource_user_name'),
        key: 'createUserName'
      },
      {
        title: t('datasource.datasource_type'),
        key: 'pluginName',
        width: 180
      },
      {
        title: t('datasource.datasource_parameter'),
        key: 'parameter',
        width: 180,
        render: (row: any) => {
          return row.datasourceConfig
            ? h(
                NPopover,
                { trigger: 'click' },
                {
                  trigger: () =>
                    h(NButton, { text: true }, {
                      default: () => t('datasource.click_to_view')
                    }),
                  default: () =>
                    h(JsonHighlight, {
                      params: JSON.stringify(
                        row.datasourceConfig
                      ) as string
                    })
                }
              )
            : '--'
        }
      },
      {
        title: t('datasource.description'),
        key: 'description',
        render: (row: any) => row.description || '-'
      },
      {
        title: t('datasource.create_time'),
        key: 'createTime',
      },
      {
        title: t('datasource.update_time'),
        key: 'updateTime',
      },
      {
        title: t('data_pipes.operation'),
        key: 'operation',
        render: (row: any) =>
          h(NSpace, null, {
            default: () => [
              h(
                NButton,
          {
                  text: true,
                  onClick: () => void onCallback(row.id, 'edit')
          },
          {
                  default: () => t('datasource.edit')
                }
              ),
              h(
                NButton,
                {
                  text: true,
                  onClick: () => void onCallback(row.id, 'delete')
          },
                { default: () => t('datasource.delete') }
              )
        ]
      })
      }
    ]
  }

  return {
    getColumns
  }
}
