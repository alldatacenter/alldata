<template>
  <cl-list-layout
    class="spider-list"
    :action-functions="actionFunctions"
    :nav-actions="navActions"
    :table-pagination="tablePagination"
    :table-columns="tableColumns"
    :table-data="tableData"
    :table-total="tableTotal"
    :table-filter="tableListFilter"
    :table-sort="tableListSort"
    :table-actions-prefix="tableActionsPrefix"
    :no-actions="noActions"
    :embedded="embedded"
  >
    <template #extra>
      <!-- Dialogs (handled by store) -->
      <cl-create-edit-spider-dialog/>
      <cl-run-spider-dialog/>
      <cl-upload-spider-files-dialog/>
      <!-- ./Dialogs -->
    </template>
  </cl-list-layout>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import useSpiderList from '@/views/spider/list/useSpiderList';

export default defineComponent({
  name: 'SpiderList',
  props: {
    noActions: {
      type: Boolean,
      required: false
    },
    embedded: {
      type: Boolean,
      required: false
    }
  },
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
      tableActionsPrefix,
      activeDialogKey,
    } = useSpiderList();

    return {
      navActions,
      tableColumns,
      tableData,
      tableTotal,
      tablePagination,
      tableListFilter,
      tableListSort,
      actionFunctions,
      tableActionsPrefix,
      activeDialogKey,
    };
  },
});
</script>

<style lang="scss" scoped>
.spider-list {
  .nav-actions {
    border-bottom: none;
  }

  .content {
    background-color: var(--cl-container-white-bg);
  }
}
</style>
