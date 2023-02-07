<template>
  <el-dialog
    :custom-class="[className, visible ? 'visible' : 'hidden'].join(' ')"
    :modal-class="modalClass"
    :before-close="onClose"
    :model-value="visible"
    :top="top"
    :width="width"
    :z-index="zIndex"
  >
    <slot/>
    <template #title>
      <div v-html="title"/>
    </template>
    <template #footer>
      <slot name="prefix"/>
      <cl-button
        id="cancel-btn"
        class-name="cancel-btn"
        plain
        type="info"
        @click="onClose"
      >
        {{ t('common.actions.cancel') }}
      </cl-button>
      <cl-button
        id="confirm-btn"
        class-name="confirm-btn"
        :disabled="confirmDisabled"
        :loading="confirmLoading"
        type="primary"
        @click="onConfirm"
      >
        {{ t('common.actions.confirm') }}
      </cl-button>
      <slot name="suffix"/>
    </template>
  </el-dialog>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import {useI18n} from 'vue-i18n';

export default defineComponent({
  name: 'Dialog',
  props: {
    visible: {
      type: Boolean,
      required: false,
      default: false,
    },
    modalClass: {
      type: String,
    },
    title: {
      type: String,
      required: false,
    },
    top: {
      type: String,
      required: false,
      default: '15vh'
    },
    width: {
      type: String,
      required: false,
    },
    zIndex: {
      type: Number,
      required: false,
    },
    confirmDisabled: {
      type: Boolean,
      default: false,
    },
    confirmLoading: {
      type: Boolean,
      default: false,
    },
    className: {
      type: String,
    },
  },
  emits: [
    'close',
    'confirm',
  ],
  setup(props: DialogProps, {emit}) {
    // i18n
    const {t} = useI18n();

    const onClose = () => {
      emit('close');
    };

    const onConfirm = () => {
      emit('confirm');
    };

    return {
      onClose,
      onConfirm,
      t,
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
