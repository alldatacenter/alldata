<template>
  <div class="table-optinize">
    <a-table
      rowKey="processId"
      :columns="columns"
      :data-source="dataSource"
      :pagination="pagination"
      @change="change"
      :loading="loading"

    >
      <template #headerCell="{ column }">
        <template v-if="column.dataIndex === 'tasks'">
          <div class="">{{column.title}}</div>
          <div class="">success / total</div>
        </template>
        <template v-if="column.dataIndex === 'inputFiles'">
          <div class="">{{column.title}}</div>
          <div class="">size / count</div>
        </template>
        <template v-if="column.dataIndex === 'outputFiles'">
          <div class="">{{column.title}}</div>
          <div class="">size / count</div>
        </template>
      </template>
      <template #bodyCell="{record, column }">
        <template v-if="column.dataIndex === 'status'">
          <div class="g-flex-ac">
            <span :style="{'background-color': (STATUS_CONFIG[record.status] || {}).color}" class="status-icon"></span>
            <span>{{ record.status }}</span>
            <a-tooltip v-if="record.status === 'FAILED'" placement="topRight" class="g-ml-4" overlayClassName="table-failed-tip">
              <template #title><div class="tip-title">{{record.failReason}}</div></template>
              <question-circle-outlined />
            </a-tooltip>
          </div>
        </template>
      </template>
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
import { QuestionCircleOutlined } from '@ant-design/icons-vue'
import { bytesToSize, dateFormat, formatMS2Time } from '@/utils/index'

// const statusMap = { RUNNING: 'RUNNING', CLOSED: 'CLOSED', SUCCESS: 'SUCCESS', FAILED: 'FAILED' }
const STATUS_CONFIG = shallowReactive({
  RUNNING: { title: 'RUNNING', color: '#1890ff' },
  CLOSED: { title: 'CLOSED', color: '#c9cdd4' },
  SUCCESS: { title: 'SUCCESS', color: '#0ad787' },
  FAILED: { title: 'FAILED', color: '#f5222d' }
})

const { t } = useI18n()
const columns: IColumns[] = shallowReactive([
  { title: t('processId'), dataIndex: 'processId' },
  { title: t('startTime'), dataIndex: 'startTime' },
  { title: t('type'), dataIndex: 'optimizingType' },
  { title: t('status'), dataIndex: 'status' },
  { title: t('duration'), dataIndex: 'duration' },
  { title: t('tasks'), dataIndex: 'tasks' },
  { title: t('finishTime'), dataIndex: 'finishTime' },
  { title: t('input'), dataIndex: 'inputFiles' },
  { title: t('output'), dataIndex: 'outputFiles' }

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
      const { inputFiles = {}, outputFiles = {} } = item
      return {
        ...item,
        // recordId,
        // startTime: item.commitTime ? d(new Date(item.commitTime), 'long') : '',
        // commitTime: item.commitTime ? dateFormat(item.commitTime) : '',
        startTime: item.startTime ? dateFormat(item.startTime) : '-',
        finishTime: item.finishTime ? dateFormat(item.finishTime) : '-',
        duration: formatMS2Time(item.duration || 0),
        inputFiles: `${bytesToSize(inputFiles.totalSize)} / ${inputFiles.fileCnt}`,
        outputFiles: `${bytesToSize(outputFiles.totalSize)} / ${outputFiles.fileCnt}`,
        tasks: `${item.successTasks} / ${item.totalTasks}（${item.runningTasks} running）`
      }
    }))
    console.log(dataSource)
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
  :deep(.ant-table-thead > tr > th) {
    padding: 4px 16px !important;
  }
}
.status-icon {
  width: 8px;
  height: 8px;
  border-radius: 8px;
  background-color: #c9cdd4;
  display: inline-block;
  margin-right: 8px;
}
</style>
<style lang="less">
.table-failed-tip{
  .ant-tooltip-content{
    width: 800px;
  }
  .tip-title{
    display: block;
    max-height: 700px;
    overflow: auto;
    white-space: pre-wrap;
  }
}
</style>
