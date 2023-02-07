<template>
  <cl-dialog
    :visible="dialogVisible"
    :title="dialogTitle"
    @close="onClose"
  >
    <p class="result-dialog-content" v-html="dialogContent"/>
  </cl-dialog>
</template>

<script lang="ts">
import {computed, defineComponent, h} from 'vue';
import {useStore} from 'vuex';
import {translate} from "@/utils";
import {getDataFieldIconClassNameByType} from "@/utils/dataFields";

const t = translate;

export default defineComponent({
  name: 'ResultCellDialog',
  setup(props: ResultCellDialogProps) {
    const ns = 'dataCollection';
    const store = useStore();
    const {
      dataCollection: state,
    } = store.state as RootStoreState;

    const dialogVisible = computed<boolean>(() => state.resultDialogVisible);
    const dialogContent = computed<string>(() => state.resultDialogContent);
    const dialogType = computed<DataFieldType>(() => state.resultDialogType);
    const dialogKey = computed<string>(() => state.resultDialogKey);

    const dialogTitle = computed(() => {
      const icon = getDataFieldIconClassNameByType(dialogType.value);
      return `<i class="${icon}" style="margin-right: 5px"></i>${dialogKey.value} (${t('components.result.types.' + dialogType.value)})`;
    });

    const onClose = () => {
      store.commit(`${ns}/setResultDialogVisible`, false);
      store.commit(`${ns}/resetResultDialogContent`);
      store.commit(`${ns}/resetResultDialogType`);
      store.commit(`${ns}/resetResultDialogKey`);
    };

    return {
      dialogVisible,
      dialogContent,
      dialogType,
      dialogKey,
      dialogTitle,
      onClose,
      t,
    };
  }
});
</script>

<style lang="scss" scoped>
.result-dialog-content {
  font-size: 14px;
  line-height: 1.6;
}
</style>
