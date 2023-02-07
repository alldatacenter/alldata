<template>
  <cl-list-layout
    class="notification-list"
    :action-functions="actionFunctions"
    :nav-actions="navActions"
    :table-pagination="tablePagination"
    :table-columns="tableColumns"
    :table-data="tableData"
    :table-total="tableTotal"
  >
    <template #extra>
      <!-- Dialogs (handled by store) -->
      <cl-create-edit-notification-dialog/>
      <!-- ./Dialogs -->
    </template>
  </cl-list-layout>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import useNotificationList from '@/views/notification/list/useNotificationList';

export default defineComponent({
  name: 'NotificationList',
  setup() {
    const {
      navActions,
      tableColumns,
      tableData,
      tableTotal,
      tablePagination,
      tableListFilter,
      tableListSort,
      actionFunctions,
      activeDialogKey,
    } = useNotificationList();

    return {
      navActions,
      tableColumns,
      tableData,
      tableTotal,
      tablePagination,
      tableListFilter,
      tableListSort,
      actionFunctions,
      activeDialogKey,
    };
  },
});
</script>

<style scoped>

</style>
