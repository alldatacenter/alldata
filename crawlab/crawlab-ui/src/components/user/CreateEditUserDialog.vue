<template>
  <cl-create-edit-dialog
    :action-functions="actionFunctions"
    :batch-form-data="formList"
    :batch-form-fields="batchFormFields"
    :confirm-disabled="confirmDisabled"
    :confirm-loading="confirmLoading"
    :form-rules="formRules"
    :tab-name="createEditDialogTabName"
    :type="activeDialogKey"
    :visible="createEditDialogVisible"
  >
    <template #default>
      <cl-user-form/>
    </template>
  </cl-create-edit-dialog>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import {useStore} from 'vuex';
import useUser from '@/components/user/user';

export default defineComponent({
  name: 'CreateEditUserDialog',
  setup() {
    // store
    const store = useStore();

    return {
      ...useUser(store),
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
