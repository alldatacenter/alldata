<template>
  <div class="table-transactions">
    <a-table
      rowKey="id"
      :columns="columns"
      :data-source="dataSource"
      :pagination="pagination"
      v-if="!hasBreadcrumb"
      :loading="loading"
      @change="change"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'transactionId'">
          <a-button type="link" @click="toggleBreadcrumb(record)">
            {{ record.transactionId }}
          </a-button>
        </template>
      </template>
    </a-table>
    <template v-else>
      <a-breadcrumb separator=">">
        <a-breadcrumb-item @click="toggleBreadcrumb" class="text-active">All</a-breadcrumb-item>
        <a-breadcrumb-item>{{ `${$t('transactionId')} ${transactionId}`}}</a-breadcrumb-item>
      </a-breadcrumb>
      <a-table
        rowKey="file"
        :columns="breadcrumbColumns"
        :data-source="breadcrumbDataSource"
        :pagination="breadcrumbPagination"
        :loading="loading"
        @change="change"
        class="g-mt-8"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'path'">
            <a-tooltip>
              <template #title>{{record.path}}</template>
              <span>{{record.path}}</span>
            </a-tooltip>
          </template>
        </template>
      </a-table>
    </template>

  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref, shallowReactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { usePagination } from '@/hooks/usePagination'
import { BreadcrumbTransactionItem, IColumns, TransactionItem } from '@/types/common.type'
import { getDetailByTransactionId, getTransactions } from '@/services/table.service'
import { useRoute } from 'vue-router'
import { dateFormat } from '@/utils'

const hasBreadcrumb = ref<boolean>(false)
const { t } = useI18n()
const columns: IColumns[] = shallowReactive([
  { title: t('transactionId'), dataIndex: 'transactionId', ellipsis: true },
  { title: t('fileCount'), dataIndex: 'fileCount' },
  { title: t('size'), dataIndex: 'fileSize' },
  { title: t('commitTime'), dataIndex: 'commitTime' },
  { title: t('snapshotId'), dataIndex: 'snapshotId', ellipsis: true }
])
const breadcrumbColumns = shallowReactive([
  { title: t('operation'), dataIndex: 'operation', width: 120, ellipsis: true },
  { title: t('file'), dataIndex: 'file', ellipsis: true },
  // { title: t('fsn'), dataIndex: 'fsn' },
  { title: t('partition'), dataIndex: 'partition', width: 120 },
  { title: t('fileContent'), dataIndex: 'type', width: 120 },
  { title: t('size'), dataIndex: 'size', width: 120 },
  { title: t('commitTime'), dataIndex: 'commitTime', width: 200, ellipsis: true },
  { title: t('path'), dataIndex: 'path', ellipsis: true }
])
const dataSource = reactive<TransactionItem[]>([])
const breadcrumbDataSource = reactive<BreadcrumbTransactionItem[]>([])
const transactionId = ref<string>('')
const loading = ref<boolean>(false)
const pagination = reactive(usePagination())
const breadcrumbPagination = reactive(usePagination())
const route = useRoute()
const query = route.query
const sourceData = reactive({
  catalog: '',
  db: '',
  table: '',
  ...query
})

async function getTableInfo() {
  try {
    loading.value = true
    dataSource.length = 0
    const result = await getTransactions({
      ...sourceData,
      page: pagination.current,
      pageSize: pagination.pageSize
    })
    const { list = [], total } = result
    list.forEach((p: TransactionItem) => {
      p.commitTime = p.commitTime ? dateFormat(p.commitTime) : ''
      dataSource.push(p)
    })
    pagination.total = total
  } catch (error) {
  } finally {
    loading.value = false
  }
}

function change({ current = 1, pageSize = 25 }) {
  if (hasBreadcrumb.value) {
    breadcrumbPagination.current = current
    if (pageSize !== breadcrumbPagination.pageSize) {
      breadcrumbPagination.current = 1
    }
    breadcrumbPagination.pageSize = pageSize
  } else {
    pagination.current = current
    if (pageSize !== pagination.pageSize) {
      pagination.current = 1
    }
    pagination.pageSize = pageSize
  }
  refresh()
}

function refresh() {
  if (hasBreadcrumb.value) {
    getBreadcrumbTable()
  } else {
    getTableInfo()
  }
}

async function getBreadcrumbTable() {
  try {
    breadcrumbDataSource.length = 0
    loading.value = true
    const params = {
      ...sourceData,
      transactionId: transactionId.value,
      page: breadcrumbPagination.current,
      pageSize: breadcrumbPagination.pageSize
    }
    const result = await getDetailByTransactionId(params)
    const { list, total } = result
    breadcrumbPagination.total = total
    list.forEach((p: BreadcrumbTransactionItem) => {
      p.commitTime = p.commitTime ? dateFormat(p.commitTime) : ''
      breadcrumbDataSource.push(p)
    })
  } catch (error) {
  } finally {
    loading.value = false
  }
}

function toggleBreadcrumb(record: TransactionItem) {
  transactionId.value = record.transactionId
  hasBreadcrumb.value = !hasBreadcrumb.value
  if (hasBreadcrumb.value) {
    breadcrumbPagination.current = 1
    getBreadcrumbTable()
  }
}

onMounted(() => {
  hasBreadcrumb.value = false
  getTableInfo()
})

</script>

<style lang="less" scoped>
.table-transactions {
  padding: 18px 24px;
  .text-active {
    color: #1890ff;
    cursor: pointer;
  }
  :deep(.ant-btn-link) {
    padding: 0;
  }
}
</style>
