<template>
  <cl-create-edit-dialog
      width="800px"
      :action-functions="actionFunctions"
      :confirm-disabled="confirmDisabled"
      :confirm-loading="confirmLoading"
      :tab-name="createEditDialogTabName"
      :type="activeDialogKey"
      :visible="createEditDialogVisible"
      no-batch
  >
    <template #default>
      <cl-plugin-form/>
    </template>
  </cl-create-edit-dialog>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import {useStore} from 'vuex';
import usePlugin from '@/components/plugin/plugin';

export default defineComponent({
  name: 'CreateEditPluginDialog',
  setup() {
    // store
    const store = useStore();

    return {
      ...usePlugin(store),
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
