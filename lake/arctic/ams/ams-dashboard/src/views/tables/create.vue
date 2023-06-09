<template>
  <div class="create-table">
    <div class="nav-bar">
      <left-outlined @click="goBack" />
      <span class="title g-ml-8">{{$t('createTable')}}</span>
    </div>
    <div class="content">
      <div class="basic">
        <p class="title">{{$t('basicInformation')}}</p>
        <a-form ref="formRef" :model="formState" class="label-120">
          <a-form-item name="catalog" label="Catalog" :rules="[{ required: true, message: `${placeholder.selectClPh}` }]">
            <a-select
              v-model:value="formState.catalog"
              :options="catalogOptions"
              showSearch
              @change="changeCatalog"
              :placeholder="placeholder.selectClPh"
            />
          </a-form-item>
          <a-form-item name="database" label="Database" :rules="[{ required: true, message: `${placeholder.selectDBPh}` }]">
            <a-select
              v-model:value="formState.database"
              :options="databaseOptions"
              showSearch
              @change="changeDatabase"
              :placeholder="placeholder.selectDBPh"
            />
          </a-form-item>
          <a-form-item name="tableName" label="Table" :rules="[{ required: true, message: `${placeholder.inputTNPh}` }]">
            <a-input v-model:value="formState.tableName" :placeholder="placeholder.inputTNPh" />
          </a-form-item>
        </a-form>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import type { SelectProps } from 'ant-design-vue'
import { LeftOutlined } from '@ant-design/icons-vue'
import { TableBasicInfo } from '@/types/common.type'
import { usePlaceholder } from '@/hooks/usePlaceholder'

const emit = defineEmits<{
 (e: 'goBack'): void
}>()

const formRef = ref()
const catalogOptions = ref<SelectProps['options']>([
  {
    value: 'catalog1',
    label: 'catalog1'
  },
  {
    value: 'catalog2',
    label: 'catalog2'
  }
])
const databaseOptions = ref<SelectProps['options']>([
  {
    value: 'database1',
    label: 'database1'
  },
  {
    value: 'database2',
    label: 'database2'
  }
])
const formState: TableBasicInfo = reactive({
  catalog: 'catalog1',
  database: '',
  tableName: ''
})
const placeholder = reactive(usePlaceholder())

function changeCatalog() {}
function changeDatabase() {}
function goBack() {
  emit('goBack')
}
</script>

<style lang="less" scoped>
.create-table {
  .nav-bar {
    padding-left: 12px;
  }
}
</style>
