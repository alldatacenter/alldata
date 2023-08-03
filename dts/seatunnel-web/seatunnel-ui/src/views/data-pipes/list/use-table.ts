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

import { reactive, ref, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { NSpace, NButton, NTag } from 'naive-ui'
import { scriptList, scriptDelete, scriptPublish } from '@/service/script'
import { taskExecute } from '@/service/task'
import { useRouter } from 'vue-router'
import { getTableColumn } from '@/common/table'
import type { ResponseTable } from '@/service/types'
import type { ScriptDetail } from '@/service/script/types'
import type { Router } from 'vue-router'

export function useTable() {
  const { t } = useI18n()
  const router: Router = useRouter()
  const state = reactive({
    columns: [],
    tableData: [],
    pageNo: ref(1),
    pageSize: ref(10),
    totalPage: ref(1),
    row: {},
    loading: ref(false),
    showDeleteModal: ref(false),
    showPublishModal: ref(false)
  })

  const createColumns = (state: any) => {
    state.columns = [
      ...getTableColumn([{ key: 'id', title: t('data_pipes.id') }]),
      {
        title: t('data_pipes.name'),
        key: 'name',
        render: (row: ScriptDetail) => {
          return h(
            NButton,
            {
              text: true,
              type: 'primary',
              onClick: () => {
                router.push({
                  path: `/data-pipes/${row.id}`
                })
              }
            },
            {
              default: () => row.name
            }
          )
        }
      },
      {
        title: t('data_pipes.state'),
        key: 'status',
        render: (row: ScriptDetail) => {
          if (row.status === 'published') {
            return h(
              NTag,
              { type: 'info' },
              { default: () => t('tasks.published') }
            )
          } else {
            return h(
              NTag,
              { type: 'default' },
              { default: () => t('tasks.unpublished') }
            )
          }
        }
      },
      {
        title: t('data_pipes.create_time'),
        key: 'createTime'
      },
      {
        title: t('data_pipes.update_time'),
        key: 'updateTime'
      },
      {
        title: t('data_pipes.operation'),
        key: 'operation',
        render: (row: ScriptDetail) =>
          h(NSpace, null, {
            default: () => [
              h(
                NButton,
                {
                  text: true,
                  disabled: row.status !== 'published',
                  onClick: () => handleExecute(row)
                },
                {
                  default: () => t('data_pipes.execute')
                }
              ),
              h(
                NButton,
                {
                  text: true,
                  disabled: row.status === 'published',
                  onClick: () => {
                    router.push({
                      path: `/data-pipes/${row.id}/edit`
                    })
                  }
                },
                {
                  default: () => t('data_pipes.edit')
                }
              ),
              h(
                NButton,
                {
                  text: true,
                  disabled: row.status === 'published',
                  onClick: () => handlePublish(row)
                },
                { default: () => t('data_pipes.publish') }
              ),
              h(
                NButton,
                {
                  text: true,
                  disabled: row.status === 'published',
                  onClick: () => handleDelete(row)
                },
                { default: () => t('data_pipes.delete') }
              )
            ]
          })
      }
    ]
  }

  const handleDelete = (row: ScriptDetail) => {
    state.showDeleteModal = true
    state.row = row
  }

  const handleConfirmDeleteModal = () => {
    if (state.tableData.length === 1 && state.pageNo > 1) {
      --state.pageNo
    }

    scriptDelete((state.row as ScriptDetail).id as number).then(() => {
      state.showDeleteModal = false
      getTableData({
        pageSize: state.pageSize,
        pageNo: state.pageNo
      })
    })
  }

  const handlePublish = (row: ScriptDetail) => {
    state.showPublishModal = true
    state.row = row
  }

  const handleConfirmPublishModal = () => {
    scriptPublish((state.row as ScriptDetail).id as number).then(() => {
      state.showPublishModal = false
      getTableData({
        pageSize: state.pageSize,
        pageNo: state.pageNo
      })
    })
  }

  const handleExecute = (row: ScriptDetail) => {
    taskExecute(row.id, {
      objectType: 0,
      executeType: 0
    }).then(() => {
      getTableData({
        pageSize: state.pageSize,
        pageNo: state.pageNo
      })
    })
  }

  const getTableData = (params: any) => {
    if (state.loading) return
    state.loading = true
    scriptList(params).then((res: ResponseTable<Array<ScriptDetail> | []>) => {
      state.tableData = res.data as any
      state.totalPage = res.data.totalPage
      state.loading = false
    })
  }

  return {
    state,
    createColumns,
    handleConfirmDeleteModal,
    handleConfirmPublishModal,
    getTableData
  }
}
