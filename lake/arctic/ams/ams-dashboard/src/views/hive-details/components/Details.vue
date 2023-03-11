<template>
  <div class="hive-table-detail g-flex">
    <div class="left-content">
      <div v-if="props.partitionColumnList && props.partitionColumnList.length" class="table-attrs">
        <p class="attr-title">{{$t('partitionKey')}}</p>
        <a-table
          rowKey="field"
          :columns="partitionColumns"
          :data-source="props.partitionColumnList"
          :pagination="false"
        />
      </div>
      <div class="table-attrs">
        <p class="attr-title">{{$t('schema')}}</p>
        <a-table
          rowKey="field"
          :columns="primaryColumns"
          :data-source="props.schema"
          :pagination="false"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { shallowReactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { IColumns, DetailColumnItem } from '@/types/common.type'

const { t } = useI18n()

const primaryColumns: IColumns[] = shallowReactive([
  { title: t('field'), dataIndex: 'field', width: '30%' },
  { title: t('type'), dataIndex: 'type', width: '30%' },
  { title: t('description'), dataIndex: 'comment', ellipsis: true }
])
const partitionColumns: IColumns[] = shallowReactive([
  { title: t('field'), dataIndex: 'field', width: '30%' },
  { title: t('type'), dataIndex: 'type', width: '30%' },
  { title: t('description'), dataIndex: 'comment', ellipsis: true }
])

const props = defineProps<{ schema: DetailColumnItem[], partitionColumnList: DetailColumnItem[]}>()

</script>

<style lang="less" scoped>
.hive-table-detail {
  .left-content {
    padding: 0 24px 12px;
    width: 66%;
  }
  .table-attrs {
    margin-top: 16px;
  }
  .attr-title {
    font-size: 16px;
    line-height: 24px;
    font-weight: bold;
    color: #102048;
    padding-bottom: 12px;
  }
}
</style>
