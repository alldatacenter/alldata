<template lang="html">
  <label :class="`${prefixCls}-wrapper`">
    <span :class="checkboxClass">
      <span
        :class="`${prefixCls}__inner`"
        @click="toggle"></span>
    </span>
  </label>
</template>

<script>
  export default {
    name: 'zk-checkbox',
    props: {
      value: {
        type: Boolean,
        default: false,
      },
      disabled: {
        type: Boolean,
        default: false,
      },
      indeterminate: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      return {
        prefixCls: 'zk-checkbox',
      };
    },
    computed: {
      checkboxClass() {
        return [
          `${this.prefixCls}`,
          {
            [`${this.prefixCls}--disabled`]: this.disabled,
            [`${this.prefixCls}--checked`]: this.value,
            [`${this.prefixCls}--indeterminate`]: this.indeterminate,
          },
        ];
      },
    },
    methods: {
      toggle() {
        if (this.disabled) {
          return false;
        }
        const value = !this.value;
        this.$emit('input', value);
        return this.$emit('on-change', value);
      },
    },
  };
</script>

<style lang="less" scoped src="./Checkbox.less"></style>
