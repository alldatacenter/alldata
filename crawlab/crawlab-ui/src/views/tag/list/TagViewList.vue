<template>
  <cl-list-layout
    class="tag-list"
    :action-functions="actionFunctions"
    :nav-actions="navActions"
    :table-pagination="tablePagination"
    :table-columns="tableColumns"
    :table-data="tableData"
    :table-total="tableTotal"
  >
    <template #extra>
      <!-- Dialogs (handled by store) -->
      <cl-create-edit-tag-dialog/>
      <!-- ./Dialogs -->
    </template>
  </cl-list-layout>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import useTagList from '@/views/tag/list/useTagList';

export default defineComponent({
  name: 'TagViewList',
  setup() {
    const {
      navActions,
      tableColumns,
      tableData,
      tableTotal,
      tablePagination,
      actionFunctions,
    } = useTagList();

    return {
      navActions,
      tableColumns,
      tableData,
      tableTotal,
      tablePagination,
      actionFunctions,
    };
  },
});
</script>

<style lang="scss" scoped>

.tag-list {
  .nav-actions {
    border-bottom: none;
  }

  .content {
    background-color: var(--container-white-bg);
  }
}
</style>
