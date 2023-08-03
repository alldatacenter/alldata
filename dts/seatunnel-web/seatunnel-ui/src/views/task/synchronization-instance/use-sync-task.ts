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

import { h, reactive, ref } from 'vue'
import { endOfToday, format, startOfToday } from 'date-fns'
import { useTableLink, useTableOperation } from '@/hooks'
import {
  AlignLeftOutlined,
  CheckCircleOutlined,
  ClearOutlined,
  DownloadOutlined,
  SyncOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  DeleteOutlined
} from '@vicons/antd'
import { useI18n } from 'vue-i18n'
import { cleanState, downloadLog, forceSuccess } from '@/service/task-instances'
import {
  COLUMN_WIDTH_CONFIG,
  DefaultTableWidth,
  calculateTableWidth
} from '@/common/column-width-config'
import { useRoute, useRouter } from 'vue-router'
import { ITaskState } from '@/common/types'
import { tasksState } from '@/common/common'
import { NIcon, NSpin, NTooltip } from 'naive-ui'
import { useMessage } from 'naive-ui'
import {
  querySyncTaskInstancePaging,
  hanldlePauseJob,
  hanldleRecoverJob,
  hanldleDelJob
} from '@/service/sync-task-instance'
import type { RowKey } from 'naive-ui/lib/data-table/src/interface'
import type { Router } from 'vue-router'
import {
  cleanStateByIds,
  forcedSuccessByIds
} from '@/service/sync-task-instance'
import { getRemainTime } from '@/utils/time'

export function useSyncTask(syncTaskType = 'BATCH') {
  const { t } = useI18n()
  const router: Router = useRouter()
  // const projectStore = useProjectStore()
  const route = useRoute()
  const message = useMessage()

  const variables = reactive({
    tableWidth: DefaultTableWidth,
    columns: [],
    tableData: [],
    page: ref(1),
    pageSize: ref(10),
    totalPage: ref(1),
    loadingRef: ref(false),
    logRef: '',
    logLoadingRef: ref(true),
    showModalRef: ref(false),
    row: {},
    skipLineNum: ref(0),
    limit: ref(1000),
    taskName: ref(''),
    executeUser: ref(''),
    host: ref(''),
    stateType: null as null | string,
    syncTaskType,
    checkedRowKeys: [] as Array<RowKey>,
    buttonList: [],
    datePickerRange: [
      format(startOfToday(), 'yyyy-MM-dd HH:mm:ss'),
      format(endOfToday(), 'yyyy-MM-dd HH:mm:ss')
    ]
  })

  const creatInstanceButtons = (variables: any) => {
    variables.buttonList = [
      {
        label: t('project.task.clean_state'),
        key: 'clean_state'
      },
      {
        label: t('project.task.forced_success'),
        key: 'forced_success'
      }
    ]
  }
  //
  const createColumns = (variables: any) => {
    variables.columns = [
      useTableLink(
        {
          title: t('project.synchronization_definition.task_name'),
          key: 'jobDefineName',
          ...COLUMN_WIDTH_CONFIG['link_name'],
          button: {
            // disabled: (row: any) =>
            //   !row.jobInstanceEngineId ||
            //   !row.jobInstanceEngineId.includes('::'),
            onClick: (row: any) => {
              router.push({
                path: `/task/synchronization-instance/${row.jobDefineId}`,
                query: {
                  jobInstanceId: row.id,
                  taskName: row.jobDefineName,
                }
              })
            }
          }
        }
      ),
      {
        title: t('project.synchronization_instance.amount_of_data_read'),
        key: 'readRowCount',
        ...COLUMN_WIDTH_CONFIG['tag']
      },
      {
        title: t('project.synchronization_instance.amount_of_data_written'),
        key: 'writeRowCount',
        ...COLUMN_WIDTH_CONFIG['tag']
      },
      {
        title: t('project.synchronization_instance.execute_user'),
        key: 'createUserId',
        ...COLUMN_WIDTH_CONFIG['state']
      },
      {
        title: t('project.synchronization_instance.state'),
        key: 'jobStatus',
        ...COLUMN_WIDTH_CONFIG['state']
      },
      {
        title: t('project.synchronization_instance.start_time'),
        key: 'createTime',
        ...COLUMN_WIDTH_CONFIG['time']
      },
      {
        title: t('project.synchronization_instance.end_time'),
        key: 'endTime',
        ...COLUMN_WIDTH_CONFIG['time']
      },
      {
        title: t('project.synchronization_instance.run_time'),
        key: 'runningTime',
        render: (row: any) => getRemainTime(row.runningTime),
        ...COLUMN_WIDTH_CONFIG['duration']
      },
      useTableOperation(
        {
          title: t('project.synchronization_instance.operation'),
          key: 'operation',
          itemNum: 3,
          buttons: [
            {
              text: t('project.workflow.recovery_suspend'),
              icon: h(PlayCircleOutlined),
              onClick: (row) => void handleRecover(row.id)
            },
            {
              text: t('project.workflow.pause'),
              icon: h(PauseCircleOutlined),
              onClick: (row) => void handlePause(row.id)
            },
            {
              isDelete: true,
              text: t('project.synchronization_instance.delete'),
              icon: h(DeleteOutlined),
              onClick: (row) => void handleDel(row.id),
              onPositiveClick: () => {
                console.log('123')
              },
              positiveText: t('project.synchronization_instance.confirm'),
              popTips: t('project.synchronization_instance.delete_confirm')
            }
          ]
        }
      )
    ]

    if (variables.tableWidth) {
      variables.tableWidth = calculateTableWidth(variables.columns)
    }
  }

  const getTableData = (params: any) => {
    if (variables.loadingRef) return
    variables.loadingRef = true

    variables.loadingRef = false
    querySyncTaskInstancePaging(params)
      .then((res: any) => {
        variables.tableData = res.totalList as any
        variables.totalPage = res.totalPage
        variables.loadingRef = false
      })
      .catch(() => {
        variables.loadingRef = false
        variables.tableData = [] as any
      })
  }
  const handleRecover = (id: number) => {
    hanldleRecoverJob(id).then(() => {
      message.success(t('common.success_tips'))
    })
  }
  const handlePause = (id: number) => {
    hanldlePauseJob(id).then(() => {
      message.success(t('common.success_tips'))
    })
  }
  const handleDel = (id: number) => {
    hanldleDelJob(id).then(() => {
      message.success(t('common.success_tips'))
    })
  }

  const handleLog = (row: any) => {
    variables.showModalRef = true
    variables.row = row
  }

  const handleCleanState = (row: any) => {
    cleanState(Number(row.projectCode), [row.id]).then(() => {
      getList()
    })
  }

  const handleForcedSuccess = (row: any) => {
    forceSuccess({ id: row.id }, { projectCode: Number(row.projectCode) }).then(
      () => {
        getList()
      }
    )
  }

  const getList = () => {
    getTableData({
      pageSize: variables.pageSize,
      pageNo:
        variables.tableData.length === 1 && variables.page > 1
          ? variables.page - 1
          : variables.page,
      taskName: variables.taskName,
      host: variables.host,
      stateType: variables.stateType,
      startDate: variables.datePickerRange ? variables.datePickerRange[0] : '',
      endDate: variables.datePickerRange ? variables.datePickerRange[1] : '',
      executorName: variables.executeUser,
      syncTaskType: variables.syncTaskType
    })
  }

  const onReset = () => {
    variables.taskName = ''
    variables.executeUser = ''
    variables.host = ''
    variables.stateType = null
    variables.datePickerRange = [
      format(startOfToday(), 'yyyy-MM-dd HH:mm:ss'),
      format(endOfToday(), 'yyyy-MM-dd HH:mm:ss')
    ]
  }
  const onBatchCleanState = (ids: any) => {
    cleanStateByIds(ids).then(() => {
      window.$message.success(t('project.workflow.success'))
      variables.checkedRowKeys = []
      getList()
    })
  }

  const onBatchForcedSuccess = (ids: any) => {
    forcedSuccessByIds(ids).then(() => {
      window.$message.success(t('project.workflow.success'))
      variables.checkedRowKeys = []
      getList()
    })
  }

  const batchBtnListClick = (key: string) => {
    if (variables.checkedRowKeys.length == 0) {
      window.$message.warning(t('project.select_task_instance'))
      return
    }
    switch (key) {
      case 'clean_state':
        onBatchCleanState(variables.checkedRowKeys)
        break
      case 'forced_success':
        onBatchForcedSuccess(variables.checkedRowKeys)
        break
    }
  }

  return {
    variables,
    createColumns,
    getTableData,
    onReset,
    batchBtnListClick,
    creatInstanceButtons
  }
}

const renderStateCell = (state: ITaskState, t: Function) => {
  if (!state) return ''

  const stateOption = tasksState(t)[state]
  if (!stateOption) return ''
  const Icon = h(
    NIcon,
    {
      color: stateOption.color,
      class: stateOption.classNames,
      style: {
        display: 'flex'
      },
      size: 20
    },
    () => h(stateOption.icon)
  )
  return h(NTooltip, null, {
    trigger: () => {
      if (!stateOption.isSpin) return Icon
      return h(NSpin, { size: 20 }, { icon: () => Icon })
    },
    default: () => stateOption.desc
  })
}
