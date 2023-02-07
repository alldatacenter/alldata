<template>
  <el-form
      ref="formRef"
      :inline="inline"
      :label-width="labelWidth"
      :size="size"
      :model="model"
      class="form"
      :rules="rules"
      hide-required-asterisk
      @validate="$emit('validate')"
  >
    <slot></slot>
  </el-form>
</template>

<script lang="ts">
import {computed, defineComponent, PropType, provide, reactive, ref, SetupContext} from 'vue';
import {emptyObjectFunc} from '@/utils/func';

export default defineComponent({
  name: 'Form',
  props: {
    model: {
      type: Object as PropType<FormModel>,
      default: emptyObjectFunc,
    },
    inline: {
      type: Boolean,
      default: true,
    },
    labelWidth: {
      type: String,
      default: '150px',
    },
    size: {
      type: String,
      default: 'default',
    },
    grid: {
      type: Number,
      default: 4,
    },
    rules: {
      type: Object as PropType<FormRules>,
    },
  },
  emits: [
    'validate',
  ],
  setup(props: FormProps, {emit}: SetupContext) {
    const form = computed<FormContext>(() => {
      const {labelWidth, size, grid} = props;
      return {labelWidth, size, grid};
    });

    provide('form-context', reactive<FormContext>(form.value));

    const formRef = ref();

    const validate = async () => {
      return await formRef.value?.validate();
    };

    const resetFields = () => {
      return formRef.value?.resetFields();
    };

    const clearValidate = () => {
      return formRef.value?.clearValidate();
    };

    return {
      formRef,
      validate,
      resetFields,
      clearValidate,
    };
  },
});
</script>

<style lang="scss" scoped>
.form {
  display: flex;
  flex-wrap: wrap;
}
</style>
