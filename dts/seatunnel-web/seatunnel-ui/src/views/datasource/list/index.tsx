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

import { defineComponent, onMounted, ref, toRefs, watch } from 'vue'
import {
  NButton,
  NInput,
  NIcon,
  NDataTable,
  NPagination,
  NSpace,
  NCard
} from 'naive-ui'
import { SearchOutlined, ReloadOutlined } from '@vicons/antd'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useColumns } from './use-columns'
import { useTable } from './use-table'
import styles from '../index.module.scss'
import SourceModal from '../components/source-modal'
import type { Ref } from 'vue'
import type { TableColumns } from 'naive-ui/es/data-table/src/interface'

const DatasourceList = defineComponent({
  setup: function() {
    const { t } = useI18n()
    const showSourceModal = ref(false)
    const columns: Ref<TableColumns> = ref([])
    const router = useRouter()
    const route = useRoute()
    const { data, changePage, changePageSize, deleteRecord, updateList } =
      useTable()

    const handleSearch = () => {
      updateList()
    }

    const { getColumns } = useColumns((id: string, type: 'edit' | 'delete') => {
      if (type === 'edit') {
        router.push({ name: 'datasource-edit', params: { id } })
      } else {
        deleteRecord(id)
      }
    })

    const onCreate = () => {
      showSourceModal.value = true
    }

    const closeSourceModal = () => {
      showSourceModal.value = false
    }

    const handleSelectSourceType = (value: string) => {
      router.push({ name: 'datasource-create', query: { type: value } })
      closeSourceModal()
    }

    const initSearch = () => {
      const { searchVal } = route.query
      if (searchVal) {
        data.searchVal = searchVal as string
      }
    }

    onMounted(() => {
      initSearch()
      if (!route.query.tab || route.query.tab === 'datasource') {
        changePage(1)
        columns.value = getColumns()
      }
    })

    watch(useI18n().locale, () => {
      columns.value = getColumns()
    })

    return {
      t,
      showSourceModal,
      columns,
      ...toRefs(data),
      changePage,
      changePageSize,
      onCreate,
      handleSearch,
      handleSelectSourceType,
      closeSourceModal
    }
  },
  render() {
    const {
      t,
      showSourceModal,
      columns,
      list,
      page,
      pageSize,
      itemCount,
      changePage,
      changePageSize,
      onCreate,
      handleSelectSourceType,
      closeSourceModal
    } = this

    return (
      <NSpace vertical>
        <NCard title={t('datasource.datasource')}>
          {{
            'header-extra': () => (
              <NSpace>
                <NInput
                  v-model={[this.searchVal, 'value']}
                  placeholder={t('datasource.search_input_tips')}
                  style={{ width: '200px' }}
                />
                <NButton onClick={this.handleSearch} type='primary'>
                  {this.t('datasource.search')}
                </NButton>
                <NButton
                  onClick={onCreate}
                  type='success'
                >
                  {t('datasource.create')}
                </NButton>
              </NSpace>
            )
          }}
        </NCard>
        <NCard>
          <NDataTable
            row-class-name='data-source-items'
            columns={columns}
            data={list}
            striped
          />
          <NPagination
            page={page}
            page-size={pageSize}
            item-count={itemCount}
            show-quick-jumper
            show-size-picker
            page-sizes={[10, 30, 50]}
            class={styles['pagination']}
            on-update:page={changePage}
            on-update:page-size={changePageSize}
          />
        </NCard>
        <SourceModal
          show={showSourceModal}
          onChange={handleSelectSourceType}
          onCancel={closeSourceModal}
        />
      </NSpace>
    )
  }
})
export default DatasourceList
