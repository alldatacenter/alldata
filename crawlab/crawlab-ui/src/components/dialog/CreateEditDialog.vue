<template>
  <cl-dialog
    class-name="create-edit-dialog"
    :title="computedTitle"
    :visible="visible"
    :width="width"
    :confirm-loading="confirmLoading"
    :confirm-disabled="confirmDisabled"
    @close="onClose"
    @confirm="onConfirm"
  >
    <el-tabs
      v-model="internalTabName"
      :class="[type, visible ? 'visible' : '']"
      class="create-edit-dialog-tabs"
      @tab-click="onTabChange"
    >
      <el-tab-pane :label="t('components.dialog.type.single')" name="single">
        <slot/>
      </el-tab-pane>
      <el-tab-pane v-if="!noBatch" :label="t('components.dialog.type.batch')" name="batch">
        <cl-create-dialog-content-batch
          :data="batchFormData"
          :fields="batchFormFields"
        />
      </el-tab-pane>
    </el-tabs>
  </cl-dialog>
</template>

<script lang="ts">
import {computed, defineComponent, PropType, provide, ref, SetupContext, watch} from 'vue';
import CreateDialogContentBatch from '@/components/dialog/CreateDialogContentBatch.vue';
import Dialog from '@/components/dialog/Dialog.vue';
import {emptyArrayFunc, emptyObjectFunc} from '@/utils/func';
import {TabsPaneContext} from 'element-plus/lib/tokens/tabs.d';
import {useI18n} from 'vue-i18n';
import {sendEvent} from '@/admin/umeng';

export default defineComponent({
  name: 'CreateEditDialog',
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    type: {
      type: String as PropType<CreateEditDialogType>,
      default: 'create',
    },
    width: {
      type: String,
      default: '80vw',
    },
    batchFormData: {
      type: Array as PropType<TableData>,
      default: emptyArrayFunc,
    },
    batchFormFields: {
      type: Array as PropType<FormTableField[]>,
      default: emptyArrayFunc,
    },
    confirmDisabled: {
      type: Boolean,
      default: false,
    },
    confirmLoading: {
      type: Boolean,
      default: false,
    },
    actionFunctions: {
      type: Object as PropType<CreateEditDialogActionFunctions>,
      default: emptyObjectFunc,
    },
    tabName: {
      type: String as PropType<CreateEditTabName>,
      default: 'single',
    },
    title: {
      type: String,
      default: undefined,
    },
    noBatch: {
      type: Boolean,
      default: false,
    },
    formRules: {
      type: Array as PropType<FormRuleItem[]>,
      default: emptyArrayFunc,
    },
  },
  setup(props: CreateEditDialogProps, ctx: SetupContext) {
    // i18n
    const {t} = useI18n();

    const computedTitle = computed<string>(() => {
      const {visible, type, title} = props;
      if (title) return title;
      if (!visible) return '';
      switch (type) {
        case 'create':
          return t('components.dialog.create');
        case 'edit':
          return t('components.dialog.edit');
        default:
          return t('components.dialog.dialog');
      }
    });

    const onClose = () => {
      const {actionFunctions} = props;
      actionFunctions?.onClose?.();

      sendEvent('click_create_edit_dialog_close');
    };

    const onConfirm = () => {
      const {actionFunctions, tabName} = props;
      actionFunctions?.onConfirm?.();

      sendEvent('click_create_edit_dialog_confirm', {
        tabName,
      });
    };

    const internalTabName = ref<CreateEditTabName>('single');
    const onTabChange = (tab: TabsPaneContext) => {
      const tabName = tab.paneName as CreateEditTabName;
      const {actionFunctions} = props;
      actionFunctions?.onTabChange?.(tabName);

      sendEvent('click_create_edit_dialog_tab_change', {tabName});
    };
    watch(() => props.tabName, () => {
      internalTabName.value = props.tabName as CreateEditTabName;
    });

    provide<CreateEditDialogActionFunctions | undefined>('action-functions', props.actionFunctions);
    provide<FormRuleItem[] | undefined>('form-rules', props.formRules);

    return {
      computedTitle,
      onClose,
      onConfirm,
      internalTabName,
      onTabChange,
      t,
    };
  },
});
</script>

<style lang="scss">
.create-edit-dialog-tabs {
  &.edit,
  &:not(.visible) {
    .el-tabs__header {
      display: none;
    }
  }
}
</style>
