<template>
  <cl-detail-layout no-sidebar store-namespace="task">
    <template #actions>
      <cl-task-detail-actions-common/>
      <cl-task-detail-actions-logs v-if="activeTabName === 'logs'"/>
      <cl-task-detail-actions-data v-if="activeTabName === 'data'"/>
    </template>
  </cl-detail-layout>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import useTaskDetail from '@/views/task/detail/useTaskDetail';

export default defineComponent({
  name: 'TaskDetail',
  setup() {
    return {
      ...useTaskDetail(),
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
