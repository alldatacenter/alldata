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
        <template v-if="column.dataIndex === 'operationGroup'">
          <span class="primary-link g-mr-12" :class="{'disabled': record.container === 'external'}" @click="scaleOutGroup(record)">
            {{ t('scaleOut') }}
          </span>
          <span class="primary-link g-mr-12" @click="editGroup(record)">
            {{ t('edit') }}
          </span>
          <span class="primary-link" @click="removeGroup(record)">
            {{ t('remove') }}
          </span>
        </template>
      </template>
    </a-table>
  </div>
  <ScaleOut v-if="scaleOutViseble" :groupRecord="groupRecord" @cancel="scaleOutViseble = false" @refresh="refresh" />
  <u-loading v-if="releaseLoading" />
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref, shallowReactive } from 'vue'
import { IOptimizeResourceTableItem, IIOptimizeGroupItem } from '@/types/common.type'
import { getOptimizerResourceList, getResourceGroupsListAPI, groupDeleteCheckAPI, groupDeleteAPI, releaseResource } from '@/services/optimize.service'
import { useI18n } from 'vue-i18n'
import { usePagination } from '@/hooks/usePagination'
import { mbToSize } from '@/utils'
import { Modal, message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import ScaleOut from '@/views/resource/components/ScaleOut.vue'

const { t } = useI18n()
const router = useRouter()

const props = defineProps<{ curGroupName: string, type: string }>()

const emit = defineEmits<{(e: 'editGroup', record: IIOptimizeGroupItem): void; (e: 'refresh'): void}>()

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
  { dataIndex: 'name', title: t('name'), ellipsis: true },
  { dataIndex: 'container', title: t('container'), width: '23%', ellipsis: true },
  { dataIndex: 'resourceOccupation', title: t('resourceOccupation'), width: '23%', ellipsis: true },
  { dataIndex: 'operationGroup', title: t('operation'), key: 'operationGroup', ellipsis: true, width: 230, scopedSlots: { customRender: 'operationGroup' } }
])
const optimizerColumns = shallowReactive([
  { dataIndex: 'index', title: t('order'), width: 80, ellipsis: true },
  { dataIndex: 'groupName', title: t('optimizerGroup'), ellipsis: true },
  { dataIndex: 'container', title: t('container'), ellipsis: true },
  { dataIndex: 'jobStatus', title: t('status'), ellipsis: true },
  { dataIndex: 'resourceAllocation', title: t('resourceAllocation'), width: '20%', ellipsis: true },
  { dataIndex: 'operation', title: t('operation'), key: 'operation', ellipsis: true, width: 160, scopedSlots: { customRender: 'operationGroup' } }
])
const pagination = reactive(usePagination())
const optimizersList = reactive<IOptimizeResourceTableItem[]>([])
const groupList = reactive<IIOptimizeGroupItem[]>([])

const columns = computed(() => {
  return props.type === 'optimizers' ? optimizerColumns : tableColumns
})

const dataSource = computed(() => {
  return props.type === 'optimizers' ? optimizersList : groupList
})

function refresh(resetPage?: boolean) {
  if (resetPage) {
    pagination.current = 1
  }
  if (props.type === 'optimizers') {
    getOptimizersList()
  } else {
    getResourceGroupsList()
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

async function getOptimizersList () {
  try {
    optimizersList.length = 0
    loading.value = true
    const params = {
      optimizerGroup: 'all',
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

async function getResourceGroupsList () {
  try {
    groupList.length = 0
    loading.value = true
    const result = await getResourceGroupsListAPI()
    pagination.total = result.length;
    (result || []).forEach((p: IIOptimizeGroupItem) => {
      p.name = p.resourceGroup.name
      p.container = p.resourceGroup.container
      p.resourceOccupation = `${p.occupationCore} ${t('core')} ${mbToSize(p.occupationMemory)}`
      groupList.push(p)
    })
  } catch (error) {
  } finally {
    loading.value = false
  }
}

const editGroup = (record: IIOptimizeGroupItem) => {
  emit('editGroup', record)
}
const removeGroup = async(record: IIOptimizeGroupItem) => {
  const res = await groupDeleteCheckAPI({ name: record.name })
  if (res) {
    Modal.confirm({
      title: t('deleteGroupModalTitle'),
      onOk: async() => {
        await groupDeleteAPI({ name: record.name })
        message.success(`${t('remove')} ${t('success')}`)
        refresh()
      }
    })
    return
  }
  Modal.warning({
    title: t('cannotDeleteGroupModalTitle'),
    content: t('cannotDeleteGroupModalContent')
  })
}

const groupRecord = ref({})
const scaleOutViseble = ref<boolean>(false)
const scaleOutGroup = (record: IIOptimizeGroupItem) => {
  if (record.container === 'external') {
    return
  }
  groupRecord.value = { ...record }
  scaleOutViseble.value = true
}

function changeTable ({ current = pagination.current, pageSize = pagination.pageSize }) {
  pagination.current = current
  const resetPage = pageSize !== pagination.pageSize
  pagination.pageSize = pageSize
  refresh(resetPage)
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
