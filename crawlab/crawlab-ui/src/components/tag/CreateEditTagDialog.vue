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
  >
    <template #default>
      <cl-tag-form/>
    </template>
  </cl-create-edit-dialog>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import {useStore} from 'vuex';
import useTag from '@/components/tag/tag';

export default defineComponent({
  name: 'CreateEditTagDialog',
  setup() {
    // store
    const store = useStore();

    return {
      ...useTag(store),
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
