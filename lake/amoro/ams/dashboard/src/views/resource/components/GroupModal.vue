<template>
  <a-modal
    :visible="true"
    :title="edit ? $t('editgroup') : $t('addgroup')"
    :confirmLoading="confirmLoading"
    :closable="false"
    class="group-modal"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <a-form ref="formRef" :model="formState" class="label-120">
      <a-form-item
        name="name"
        :label="$t('name')"
        :rules="[{ required: true, message: `${placeholder.groupNamePh}` }]"
      >
        <a-input
          v-model:value="formState.name"
          :placeholder="placeholder.groupNamePh"
          :disabled="edit"
        />
      </a-form-item>
      <a-form-item
        name="container"
        :label="$t('container')"
        :rules="[{ required: true, message: `${placeholder.groupContainer}` }]"
      >
        <a-select
          v-model:value="formState.container"
          :showSearch="true"
          :options="selectList.containerList"
          :placeholder="placeholder.groupContainer"
        />
      </a-form-item>
      <a-form-item :label="$t('properties')"> </a-form-item>
      <a-form-item>
        <Properties
          :propertiesObj="formState.properties"
          :isEdit="true"
          ref="propertiesRef"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { usePlaceholder } from '@/hooks/usePlaceholder'
import { IIOptimizeGroupItem } from '@/types/common.type'
import {
  getGroupContainerListAPI,
  addResourceGroupsAPI,
  updateResourceGroupsAPI
} from '@/services/optimize.service'
import Properties from '@/views/catalogs/Properties.vue'
import { message } from 'ant-design-vue'

import { useI18n } from 'vue-i18n'

const { t } = useI18n()

interface FormState {
  name: string;
  container: undefined | string;
  properties: { [prop: string]: string };
}

const placeholder = reactive(usePlaceholder())
const props = defineProps<{
  edit: boolean;
  editRecord: IIOptimizeGroupItem | null;
}>()

const selectList = ref<{ containerList: any }>({ containerList: [] })
async function getContainerList() {
  const result = await getGroupContainerListAPI()
  const list = (result || []).map((item: string) => ({
    label: item,
    value: item
  }))
  selectList.value.containerList = list
}

const formState: FormState = reactive({
  name: '',
  container: undefined,
  properties: {}
})

const confirmLoading = ref<boolean>(false)
const emit = defineEmits<{
  (e: 'cancel'): void;
  (e: 'refresh'): void;
}>()

const handleCancel = () => {
  emit('cancel')
}

const formRef = ref()
const propertiesRef = ref()
const handleOk = () => {
  formRef.value.validateFields().then(async () => {
    try {
      const properties = await propertiesRef.value.getProperties()
      const params = {
        name: formState.name,
        container: formState.container as string,
        properties
      }
      if (props.edit) {
        await updateResourceGroupsAPI(params)
      } else {
        await addResourceGroupsAPI(params)
      }
      message.success(`${t('save')} ${t('success')}`)
      emit('refresh')
    } catch (error) {
      message.error(`${t('save')} ${t('failed')}`)
    }
  })
}

onMounted(() => {
  getContainerList()
  if (props.edit) {
    formState.name = props.editRecord?.name as string
    formState.container = props.editRecord?.container
    formState.properties = props.editRecord?.resourceGroup.properties as {
      [props: string]: string;
    }
  }
})
</script>

<style lang="less">
.group-modal {
  .ant-modal-body {
    max-height: 600px;
    overflow: auto;
  }
}
</style>
