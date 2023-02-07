<template>
  <cl-list-layout
    class="plugin-list"
    :action-functions="actionFunctions"
    :nav-actions="navActions"
    :table-pagination="tablePagination"
    :table-columns="tableColumns"
    :table-data="tableData"
    :table-total="tableTotal"
  >
    <template #extra>
      <!-- Dialogs (handled by store) -->
      <cl-install-plugin-dialog v-if="activeDialogKey === 'install'"/>
      <cl-settings-plugin-dialog v-if="activeDialogKey === 'settings'"/>
      <!-- ./Dialogs -->
    </template>
  </cl-list-layout>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import usePluginList from '@/views/plugin/list/usePluginList';

export default defineComponent({
  name: 'PluginList',
  setup() {
    return {
      ...usePluginList(),
    };
  }
});
</script>

<style lang="scss" scoped>
.plugin-list {
  .filter {
    height: 100%;
    display: flex;
    align-items: center;
  }
}
</style>

<style scoped>
.plugin-list >>> .filter .el-form-item {
  display: flex;
  align-items: center;
  margin: 0 0 0 10px;
  padding: 0;
}
</style>
