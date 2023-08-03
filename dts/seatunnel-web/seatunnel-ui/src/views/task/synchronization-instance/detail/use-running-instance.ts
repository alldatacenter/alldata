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

import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { queryRunningInstancePaging } from '@/service/sync-task-instance'
import { useRoute } from 'vue-router'

export function useRunningInstance() {
  const { t } = useI18n()
  const route = useRoute()

  const variables = reactive({
    columns: [],
    tableData: [],
    loadingRef: ref(false)
  })

  const createColumns = (variables: any) => {
    variables.columns = [
      {
        title: t('project.synchronization_instance.pipeline_id'),
        key: 'pipelineId'
      },
      {
        title: t('project.synchronization_instance.source'),
        key: 'sourceTableNames'
      },
      {
        title: t('project.synchronization_instance.read_rate'),
        key: 'readQps'
      },
      {
        title: t('project.synchronization_instance.amount_of_data_read'),
        key: 'readRowCount'
      },
      {
        title: t('project.synchronization_instance.delay_of_data'),
        key: 'recordDelay',
        render: (row: any) => row.recordDelay / 1000
      },
      {
        title: t('project.synchronization_instance.sink'),
        key: 'sinkTableNames'
      },
      {
        title: t('project.synchronization_instance.processing_rate'),
        key: 'writeQps'
      },
      {
        title: t('project.synchronization_instance.amount_of_data_written'),
        key: 'writeRowCount'
      },
      {
        title: t('project.synchronization_instance.state'),
        key: 'status'
      }
    ]
  }

  const getTableData = () => {
    if (variables.loadingRef) return
    variables.loadingRef = true

    queryRunningInstancePaging({
      jobInstanceId: route.query.jobInstanceId
    })
      .then((res: any) => {
        variables.tableData = res
        variables.loadingRef = false
      })
      .catch(() => {
        variables.loadingRef = false
      })
  }

  return {
    variables,
    createColumns,
    getTableData
  }
}
