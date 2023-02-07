<template>
  <cl-dialog
    :title="t('components.result.dedup.dialog.fields.title')"
    :visible="dialogVisible"
    @confirm="onDialogConfirm"
    @close="onDialogClose"
  >
    <cl-input-list
      v-model="keys"
      :placeholder="t('components.result.dedup.dialog.fields.placeholder')"
    />
  </cl-dialog>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue';
import {useStore} from 'vuex';
import {translate} from "@/utils";
import {ElMessage} from "element-plus";

const t = translate;

// store
const nsDc = 'dataCollection';
const store = useStore();
const {
  dataCollection: dataCollectionState,
} = store.state as RootStoreState;

const keys = ref<string[]>(dataCollectionState.form?.dedup?.keys || []);
watch(() => dataCollectionState.form?.dedup?.keys, (val) => {
  keys.value = val || [''];
});

const dialogVisible = computed<boolean>(() => dataCollectionState.dedupFieldsDialogVisible)
const onDialogClose = () => {
  store.commit(`${nsDc}/setDedupFieldsDialogVisible`, false);
};
const onDialogConfirm = async () => {
  store.commit(`${nsDc}/setForm`, {
    ...dataCollectionState.form,
    dedup: {
      ...dataCollectionState.form.dedup,
      keys: keys.value.filter(k => !!k),
    },
  });
  await store.dispatch(`${nsDc}/updateById`, {
    id: dataCollectionState.form?._id,
    form: dataCollectionState.form,
  });
  await ElMessage.success(t('common.message.success.update'));
  onDialogClose();
};
</script>

<style lang="scss" scoped>
</style>
