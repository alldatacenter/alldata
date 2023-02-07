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
      <cl-spider-form/>
    </template>
  </cl-create-edit-dialog>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import {useStore} from 'vuex';
import useSpider from '@/components/spider/spider';

export default defineComponent({
  name: 'CreateSpiderDialog',
  setup() {
    // store
    const store = useStore();

    return {
      ...useSpider(store),
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
