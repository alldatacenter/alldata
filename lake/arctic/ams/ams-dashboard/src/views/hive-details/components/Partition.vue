<template>
  <div class="partition-field-wrap">
    <a-table
      :loading="loading"
      class="ant-table-common"
      :columns="partitionFieldsColumns"
      :data-source="props.partitionFields"
      :pagination="false"
      >
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { shallowReactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { DetailColumnItem } from '@/types/common.type'

const { t } = useI18n()

const props = defineProps<{ partitionFields: DetailColumnItem[], loading: boolean }>()

const partitionFieldsColumns = shallowReactive([
  { dataIndex: 'field', title: t('field'), ellipsis: true },
  { dataIndex: 'type', title: t('type'), ellipsis: true },
  { dataIndex: 'comment', title: t('description'), ellipsis: true }
])

</script>
