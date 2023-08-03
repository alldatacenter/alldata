<template>
  <a-modal :visible="props.visible" :title="$t('scaleOut')" :confirmLoading="confirmLoading" :closable="false" @ok="handleOk" @cancel="handleCancel">
    <a-form ref="formRef" :model="formState" class="label-120">
      <a-form-item name="resourceGroup" :label="$t('resourceGroup')" :rules="[{ required: true, message: `${placeholder.resourceGroupPh}` }]">
        <a-select
          v-model:value="formState.resourceGroup"
          :showSearch="true"
          :options="groupList"
          :placeholder="placeholder.resourceGroupPh"
        />
      </a-form-item>
      <a-form-item name="parallelism" :label="$t('parallelism')" :rules="[{ required: true, message: `${placeholder.parallelismPh}` }]">
        <a-input v-model:value="formState.parallelism" type="number" :placeholder="placeholder.parallelismPh" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
<script lang="ts" setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { usePlaceholder } from '@/hooks/usePlaceholder'
import { IGroupItem } from '@/types/common.type'
import { getOptimizerGroups, scaleoutResource } from '@/services/optimize.service'
import { message } from 'ant-design-vue'

interface FormState {
  resourceGroup: undefined | string
  parallelism: number
}

const emit = defineEmits<{
 (e: 'cancel'): void
 (e: 'refreshOptimizersTab'): void
}>()

const props = defineProps<{ visible: boolean, resourceGroup: string }>()
const confirmLoading = ref<boolean>(false)
const placeholder = reactive(usePlaceholder())
const formRef = ref()
const formState:FormState = reactive({
  resourceGroup: props.resourceGroup || undefined,
  parallelism: 1
})

const groupList = reactive<IGroupItem[]>([])
async function getCompactQueues () {
  const result = await getOptimizerGroups()
  groupList.length = 0;
  (result || []).forEach((item: IGroupItem) => {
    groupList.push({
      ...item,
      label: item.optimizerGroupName,
      value: item.optimizerGroupName
    })
  })
}

function handleOk () {
  formRef.value
    .validateFields()
    .then(async() => {
      confirmLoading.value = true
      await scaleoutResource({
        optimizerGroup: formState.resourceGroup || '',
        parallelism: Number(formState.parallelism)
      })
      formRef.value.resetFields()
      emit('cancel')
      emit('refreshOptimizersTab')
      confirmLoading.value = false
    })
    .catch(() => {
      confirmLoading.value = false
    })
}

function handleCancel() {
  formRef.value.resetFields()
  emit('cancel')
}
onMounted(() => {
  getCompactQueues()
})
</script>
