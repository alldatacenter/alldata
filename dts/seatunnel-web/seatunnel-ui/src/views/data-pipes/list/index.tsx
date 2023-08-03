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

import { defineComponent, onMounted, toRefs } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTable } from './use-table'
import { NButton, NCard, NDataTable, NPagination, NSpace } from 'naive-ui'
import { useRouter } from 'vue-router'
import DeleteModal from './components/delete-modal'
import PublishModal from './components/publish-modal'
import type { Router } from 'vue-router'

const DataPipesList = defineComponent({
  setup() {
    const { t } = useI18n()
    const router: Router = useRouter()
    const {
      state,
      createColumns,
      getTableData,
      handleConfirmDeleteModal,
      handleConfirmPublishModal
    } = useTable()

    const requestData = () => {
      getTableData({
        pageSize: state.pageSize,
        pageNo: state.pageNo
      })
    }

    const handleCancelDeleteModal = () => {
      state.showDeleteModal = false
    }

    const handleCancelPublishModal = () => {
      state.showPublishModal = false
    }

    const handleCreate = () => {
      router.push({ path: '/data-pipes/create' })
    }

    const handlePageSize = () => {
      state.pageNo = 1
      requestData()
    }

    onMounted(() => {
      createColumns(state)
      requestData()
    })

    return {
      t,
      ...toRefs(state),
      handleCancelDeleteModal,
      handleConfirmDeleteModal,
      handleCancelPublishModal,
      handleConfirmPublishModal,
      handleCreate,
      requestData,
      handlePageSize
    }
  },
  render() {
    return (
      <NSpace vertical>
        <NCard title={this.t('data_pipes.data_pipes')}>
          {{
            'header-extra': () => (
              <NButton onClick={this.handleCreate} type='success'>
                {this.t('data_pipes.create')}
              </NButton>
            )
          }}
        </NCard>
        <NCard>
          <NSpace vertical>
            <NDataTable
              loading={this.loading}
              columns={this.columns}
              data={this.tableData}
            />
            <NSpace justify='center'>
              <NPagination
                v-model:page={this.pageNo}
                v-model:page-size={this.pageSize}
                page-count={this.totalPage}
                show-size-picker
                page-sizes={[10, 30, 50]}
                show-quick-jumper
                onUpdatePage={this.requestData}
                onUpdatePageSize={this.handlePageSize}
              />
            </NSpace>
          </NSpace>
        </NCard>
        <DeleteModal
          showModal={this.showDeleteModal}
          row={this.row}
          onCancelModal={this.handleCancelDeleteModal}
          onConfirmModal={this.handleConfirmDeleteModal}
        />
        <PublishModal
          showModal={this.showPublishModal}
          row={this.row}
          onCancelModal={this.handleCancelPublishModal}
          onConfirmModal={this.handleConfirmPublishModal}
        />
      </NSpace>
    )
  }
})

export default DataPipesList
