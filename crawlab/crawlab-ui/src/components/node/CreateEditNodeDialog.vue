<template>
  <!--TODO: implement creating node actively later-->
  <cl-create-edit-dialog
    v-if="false"
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
      <cl-node-form/>
    </template>
  </cl-create-edit-dialog>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import {useStore} from 'vuex';
import useNode from '@/components/node/node';

export default defineComponent({
  name: 'CreateEditProjectDialog',
  setup() {
    // store
    const store = useStore();

    return {
      ...useNode(store),
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
