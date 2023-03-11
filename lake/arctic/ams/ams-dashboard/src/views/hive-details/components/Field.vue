<template>
  <div class="field-wrap">
    <a-table
      :loading="loading"
      class="ant-table-common"
      :columns="fieldsColumns"
      :data-source="props.fields"
      :pagination="false"
      >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'primaryKey'">
          <a-checkbox v-model:checked="record.checked"></a-checkbox>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { shallowReactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { DetailColumnItem } from '@/types/common.type'

const { t } = useI18n()

const props = defineProps<{ fields: DetailColumnItem[], loading: boolean }>()

const fieldsColumns = shallowReactive([
  { dataIndex: 'field', title: t('field'), ellipsis: true },
  { dataIndex: 'type', title: t('type'), ellipsis: true },
  { dataIndex: 'comment', title: t('description'), ellipsis: true },
  { dataIndex: 'primaryKey', title: t('primaryKey'), scopedSlots: { customRender: 'primaryKey' } }
])

defineExpose({
  getPkname() {
    return props.fields.filter((ele: DetailColumnItem) => ele.checked)
      .map((ele: DetailColumnItem) => ({ fieldName: ele.field || '' }))
  }
})

</script>
<style lang="less" scoped>
.field-wrap {
}
</style>
