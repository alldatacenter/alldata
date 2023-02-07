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
    :form-rules="formRules"
  >
    <template #default>
      <cl-schedule-form/>
    </template>
  </cl-create-edit-dialog>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import {useStore} from 'vuex';
import useSchedule from '@/components/schedule/schedule';

export default defineComponent({
  name: 'CreateEditScheduleDialog',
  setup() {
    // store
    const store = useStore();

    return {
      ...useSchedule(store),
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
