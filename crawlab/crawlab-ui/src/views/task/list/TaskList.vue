<template>
  <cl-list-layout
    class="task-list"
    :action-functions="actionFunctions"
    :nav-actions="navActions"
    :table-pagination="tablePagination"
    :table-columns="tableColumns"
    :table-data="tableData"
    :table-total="tableTotal"
    :no-actions="noActions"
    :embedded="embedded"
  >
    <template #extra>
      <!-- Dialogs (handled by store) -->
      <cl-create-task-dialog/>
      <!-- ./Dialogs -->
    </template>
  </cl-list-layout>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import useTaskList from '@/views/task/list/useTaskList';

export default defineComponent({
  name: 'TaskList',
  props: {
    noActions: {
      type: Boolean,
      default: false,
    },
    embedded: {
      type: Boolean,
      default: false,
    },
  },
  setup(props: TaskListProps, {emit}) {
    return {
      ...useTaskList(),
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
