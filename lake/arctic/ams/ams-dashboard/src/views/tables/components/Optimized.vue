<template>
  <div class="table-optinize">
    <a-table
      rowKey="recordId"
      :columns="columns"
      :data-source="dataSource"
      :pagination="pagination"
      @change="change"
      :loading="loading"

    >
      <!-- <template #headerCell="{ column }">
        <template v-if="column.dataIndex === 'parallelism'">
          {{column.title}}
          <a-tooltip>
            <template #title>prompt text</template>
            <question-circle-outlined />
          </a-tooltip>
        </template>
      </template> -->
    </a-table>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref, shallowReactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { usePagination } from '@/hooks/usePagination'
import { IColumns } from '@/types/common.type'
import { getOptimizes } from '@/services/table.service'
import { useRoute } from 'vue-router'
// import { QuestionCircleOutlined } from '@ant-design/icons-vue'
import { bytesToSize, dateFormat, formatMS2Time } from '@/utils/index'

const { t } = useI18n()
const columns: IColumns[] = shallowReactive([
  { title: t('commitTime'), dataIndex: 'commitTime' },
  { title: t('optimizeType'), dataIndex: 'optimizeType' },
  { title: t('duration'), dataIndex: 'duration' },
  // { title: t('parallelism'), dataIndex: 'parallelism' },
  {
    title: t('input'),
    children: [
      { title: t('count'), dataIndex: 'inputCount' },
      { title: t('size'), dataIndex: 'inputSize' }
    ]
  },
  {
    title: t('output'),
    children: [
      { title: t('count'), dataIndex: 'outputCount' },
      { title: t('size'), dataIndex: 'outputSize' }
    ]
  }
])

const dataSource = reactive<any[]>([])

const loading = ref<boolean>(false)
const pagination = reactive(usePagination())
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
    const result = await getOptimizes({
      ...sourceData,
      page: pagination.current,
      pageSize: pagination.pageSize
    })
    const { list, total = 0 } = result
    pagination.total = total
    dataSource.push(...[...list || []].map(item => {
      const { recordId, totalFilesStatBeforeCompact, totalFilesStatAfterCompact } = item
      return {
        ...item,
        recordId,
        // startTime: item.commitTime ? d(new Date(item.commitTime), 'long') : '',
        commitTime: item.commitTime ? dateFormat(item.commitTime) : '',
        duration: formatMS2Time(item.duration || 0),
        inputCount: totalFilesStatBeforeCompact?.fileCnt,
        inputSize: bytesToSize(totalFilesStatBeforeCompact?.totalSize),
        outputCount: totalFilesStatAfterCompact?.fileCnt,
        outputSize: bytesToSize(totalFilesStatAfterCompact?.totalSize)
      }
    }))
  } catch (error) {
  } finally {
    loading.value = false
  }
}

function change({ current = 1, pageSize = 25 } = pagination) {
  pagination.current = current
  if (pageSize !== pagination.pageSize) {
    pagination.current = 1
  }
  pagination.pageSize = pageSize
  getTableInfo()
}

onMounted(() => {
  getTableInfo()
})

</script>

<style lang="less" scoped>
.table-optinize {
  padding: 18px 24px;
  :deep(.ant-table-thead > tr > th:not(:last-child):not(.ant-table-selection-column):not(.ant-table-row-expand-icon-cell):not([colspan])::before) {
    height: 100% !important;
  }
  :deep(.ant-table-thead > tr:not(:last-child) > th[colspan]) {
    border-bottom: 1px solid #e8e8f0;
  }
  :deep(.ant-table-thead > tr > th) {
    padding: 4px 16px !important;
  }
}
</style>
