<template>
  <cl-list-layout
    class="user-list"
    :action-functions="actionFunctions"
    :nav-actions="navActions"
    :table-pagination="tablePagination"
    :table-columns="tableColumns"
    :table-data="tableData"
    :table-total="tableTotal"
    :no-actions="noActions"
    :embedded="embedded"
    :selectable-function="selectableFunction"
  >
    <template #extra>
      <!-- Dialogs (handled by store) -->
      <cl-create-edit-user-dialog/>
      <!-- ./Dialogs -->
    </template>
  </cl-list-layout>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import useUserList from './useUserList';

export default defineComponent({
  name: 'UserList',
  props: {
    noActions: {
      type: Boolean,
      default: false,
    },
    embedded: {
      type: Boolean,
      default: false,
    }
  },
  setup() {
    const {
      navActions,
      tableColumns,
      tableData,
      tableTotal,
      tablePagination,
      actionFunctions,
      selectableFunction,
    } = useUserList();

    return {
      navActions,
      tableColumns,
      tableData,
      tableTotal,
      tablePagination,
      actionFunctions,
      selectableFunction,
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
