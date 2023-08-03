<template>
  <div class="list-wrap">
    <a-table
      class="ant-table-common"
      :columns="columns"
      :data-source="dataSource"
      :pagination="pagination"
      :loading="loading"
      @change="changeTable"
      >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'tableName'">
          <span :title="record.tableName" class="primary-link" @click="goTableDetail(record)">
            {{ record.tableName }}
          </span>
        </template>
        <template v-if="column.dataIndex === 'durationDisplay'">
          <span :title="record.durationDesc">
            {{ record.durationDisplay }}
          </span>
        </template>
        <template v-if="column.dataIndex === 'optimizeStatus'">
          <span :style="{'background-color': (STATUS_CONFIG[record.optimizeStatus] || {}).color}" class="status-icon"></span>
          <span>{{ record.optimizeStatus }}</span>
        </template>
        <template v-if="column.dataIndex === 'operation'">
          <span class="primary-link" :class="{'disabled': record.containerType === 'external'}" @click="releaseModal(record)">
            {{ t('release') }}
          </span>
        </template>
      </template>
    </a-table>
  </div>
  <u-loading v-if="releaseLoading" />
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref, shallowReactive, watch } from 'vue'
import { IOptimizeResourceTableItem, IOptimizeTableItem } from '@/types/common.type'
import { getOptimizerResourceList, getOptimizerTableList, releaseResource } from '@/services/optimize.service'
import { useI18n } from 'vue-i18n'
import { usePagination } from '@/hooks/usePagination'
import { bytesToSize, formatMS2Time, mbToSize, formatMS2DisplayTime } from '@/utils'
import { Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'

const { t } = useI18n()
const router = useRouter()

const props = defineProps<{ curGroupName: string, type: string }>()
const emit = defineEmits<{
 (e: 'refreshCurGroupInfo'): void
}>()
const STATUS_CONFIG = shallowReactive({
  pending: { title: 'pending', color: '#ffcc00' },
  idle: { title: 'idle', color: '#c9cdd4' },
  minor: { title: 'minor', color: '#0ad787' },
  major: { title: 'major', color: '#0ad787' },
  full: { title: 'full', color: '#0ad787' },
  committing: { title: 'committing', color: '#0ad787' }
})

const loading = ref<boolean>(false)
const releaseLoading = ref<boolean>(false)
const tableColumns = shallowReactive([
  { dataIndex: 'tableName', title: t('table'), ellipsis: true, scopedSlots: { customRender: 'tableName' } },
  { dataIndex: 'groupName', title: t('optimizerGroup'), width: '16%', ellipsis: true },
  { dataIndex: 'optimizeStatus', title: t('optimizingStatus'), width: '16%', ellipsis: true },
  { dataIndex: 'durationDisplay', title: t('duration'), width: '10%', ellipsis: true },
  { dataIndex: 'fileCount', title: t('fileCount'), width: '10%', ellipsis: true },
  { dataIndex: 'fileSizeDesc', title: t('fileSize'), width: '10%', ellipsis: true },
  { dataIndex: 'quota', title: t('quota'), width: '10%', ellipsis: true },
  { dataIndex: 'quotaOccupationDesc', title: t('occupation'), width: 120, ellipsis: true }
])
const optimizerColumns = shallowReactive([
  { dataIndex: 'index', title: t('order'), width: 80, ellipsis: true },
  { dataIndex: 'groupName', title: t('optimizerGroup'), ellipsis: true },
  { dataIndex: 'container', title: t('container'), ellipsis: true },
  { dataIndex: 'jobStatus', title: t('status'), ellipsis: true },
  { dataIndex: 'resourceAllocation', title: t('resourceAllocation'), width: '20%', ellipsis: true },
  { dataIndex: 'operation', title: t('operation'), key: 'operation', ellipsis: true, width: 160, scopedSlots: { customRender: 'operation' } }
])
const pagination = reactive(usePagination())
const optimizersList = reactive<IOptimizeResourceTableItem[]>([])
const tableList = reactive<IOptimizeTableItem[]>([])

const columns = computed(() => {
  return props.type === 'optimizers' ? optimizerColumns : tableColumns
})

const dataSource = computed(() => {
  return props.type === 'optimizers' ? optimizersList : tableList
})

watch(
  () => props.curGroupName,
  (value) => {
    value && refresh()
  }
)

function refresh(resetPage?: boolean) {
  if (resetPage) {
    pagination.current = 1
  }
  if (props.type === 'optimizers') {
    getOptimizersList()
  } else {
    getTableList()
  }
}

async function getOptimizersList () {
  try {
    optimizersList.length = 0
    loading.value = true
    const params = {
      optimizerGroup: props.curGroupName,
      page: pagination.current,
      pageSize: pagination.pageSize
    }
    const result = await getOptimizerResourceList(params)
    const { list, total } = result
    pagination.total = total;
    (list || []).forEach((p: IOptimizeResourceTableItem, index: number) => {
      p.resourceAllocation = `${p.coreNumber} ${t('core')} ${mbToSize(p.memory)}`
      p.index = (pagination.current - 1) * pagination.pageSize + index + 1
      optimizersList.push(p)
    })
  } catch (error) {
  } finally {
    loading.value = false
  }
}

async function getTableList () {
  try {
    tableList.length = 0
    loading.value = true
    const params = {
      optimizerGroup: props.curGroupName || '',
      page: pagination.current,
      pageSize: pagination.pageSize
    }
    const result = await getOptimizerTableList(params)
    const { list, total } = result
    pagination.total = total;
    (list || []).forEach((p: IOptimizeTableItem) => {
      p.quotaOccupationDesc = p.quotaOccupation - 0.0005 > 0 ? `${(p.quotaOccupation * 100).toFixed(1)}%` : '0'
      p.durationDesc = formatMS2Time(p.duration || 0)
      p.durationDisplay = formatMS2DisplayTime(p.duration || 0)
      p.fileSizeDesc = bytesToSize(p.fileSize)
      tableList.push(p)
    })
  } catch (error) {
  } finally {
    loading.value = false
  }
}

function releaseModal (record: IOptimizeResourceTableItem) {
  if (record.containerType === 'external') {
    return
  }
  Modal.confirm({
    title: t('releaseOptModalTitle'),
    content: '',
    okText: '',
    cancelText: '',
    onOk: () => {
      releaseJob(record)
    }
  })
}
async function releaseJob (record: IOptimizeResourceTableItem) {
  try {
    releaseLoading.value = true
    await releaseResource({
      optimizerGroup: record.groupName,
      jobId: record.jobId
    })
    refresh(true)
    emit('refreshCurGroupInfo')
  } finally {
    releaseLoading.value = false
  }
}
function changeTable ({ current = pagination.current, pageSize = pagination.pageSize }) {
  pagination.current = current
  const resetPage = pageSize !== pagination.pageSize
  pagination.pageSize = pageSize
  refresh(resetPage)
}

function goTableDetail (record: IOptimizeTableItem) {
  const { catalog, database, tableName } = record.tableIdentifier
  router.push({
    path: '/tables',
    query: {
      catalog,
      db: database,
      table: tableName
    }
  })
}

onMounted(() => {
  refresh()
})
</script>
<style lang="less" scoped>
.list-wrap {
  .primary-link {
    color: @primary-color;
    &:hover {
      cursor: pointer;
    }
    &.disabled {
      color: #999;
      &:hover {
        cursor: not-allowed;
      }
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
}
</style>
