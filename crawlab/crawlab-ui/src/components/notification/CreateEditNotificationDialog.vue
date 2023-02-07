<template>
  <cl-create-edit-dialog
    :type="activeDialogKey"
    :tab-name="createEditDialogTabName"
    :visible="createEditDialogVisible"
    :action-functions="actionFunctions"
    :batch-form-data="formList"
    :confirm-disabled="confirmDisabled"
    :confirm-loading="confirmLoading"
    :batch-form-fields="batchFormFields"
  >
    <template #default>
      <cl-notification-form/>
    </template>
  </cl-create-edit-dialog>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import {useStore} from 'vuex';
import useNotification from '@/components/notification/notification';

export default defineComponent({
  name: 'CreateEditNotificationDialog',
  setup() {
    // store
    const store = useStore();

    return {
      ...useNotification(store),
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
