<template>
  <cl-create-edit-dialog
    :action-functions="actionFunctions"
    :batch-form-data="formList"
    :batch-form-fields="batchFormFields"
    :confirm-disabled="confirmDisabled"
    :confirm-loading="confirmLoading"
    :tab-name="createEditDialogTabName"
    :type="activeDialogKey"
    :visible="createEditDialogVisible"
    :no-batch="noBatch"
    :title="title"
  >
    <template #default>
      <cl-task-form/>
    </template>
  </cl-create-edit-dialog>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import {useStore} from 'vuex';
import useTask from '@/components/task/task';

export default defineComponent({
  name: 'CreateTaskDialog',
  props: {
    title: {
      type: String,
    },
    noBatch: {
      type: Boolean,
    },
  },
  setup() {
    // store
    const store = useStore();

    return {
      ...useTask(store),
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
