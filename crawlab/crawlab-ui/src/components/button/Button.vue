<template>
  <el-tooltip :content="tooltip" :disabled="!tooltip">
    <span
      :id="id"
      :class="cls"
    >
      <el-button
        :circle="circle"
        :disabled="disabled"
        :plain="plain"
        :round="round"
        :size="size"
        :title="tooltip"
        :type="type"
        :loading="loading"
        @click="() => $emit('click')"
      >
        <slot></slot>
      </el-button>
    </span>
  </el-tooltip>
</template>

<script lang="ts">
import {computed, defineComponent, PropType} from 'vue';

export const buttonProps = {
  tooltip: {
    type: String,
    required: false,
    default: '',
  },
  type: {
    type: String as PropType<BasicType>,
    required: false,
    default: 'primary',
  },
  size: {
    type: String as PropType<BasicSize>,
    required: false,
    default: 'default',
  },
  round: {
    type: Boolean,
    required: false,
    default: false,
  },
  circle: {
    type: Boolean,
    required: false,
    default: false,
  },
  plain: {
    type: Boolean,
    required: false,
    default: false,
  },
  disabled: {
    type: Boolean,
    required: false,
    default: false,
  },
  isIcon: {
    type: Boolean,
    required: false,
    default: false,
  },
  noMargin: {
    type: Boolean,
    required: false,
    default: false,
  },
  loading: {
    type: Boolean,
    required: false,
    default: false,
  },
  className: {
    type: String,
    required: false,
    default: '',
  },
  id: {
    type: String,
    required: false,
  },
};

export default defineComponent({
  name: 'Button',
  props: buttonProps,
  emits: [
    'click',
  ],
  setup(props: ButtonProps) {
    const cls = computed<string>(() => {
      const {noMargin, className, isIcon} = props;
      const classes = [
        'button-wrapper',
      ];
      if (noMargin) classes.push('no-margin');
      if (isIcon) classes.push('icon-button');
      if (className) classes.push(className);
      return classes.join(' ');
    });

    return {
      cls,
    };
  },
});
</script>

<style lang="scss" scoped>
.button-wrapper {
  position: relative;
  margin-right: 10px;

  &.no-margin {
    margin-right: 0;
  }

  .el-button {
    vertical-align: inherit;
  }
}
</style>

<style scoped>
.button-wrapper >>> .icon-button {
  padding: 7px;
}

.button-wrapper.label-button >>> .icon,
.button-wrapper.icon-button >>> .icon {
  width: 20px;
}

.button-wrapper.fa-icon-button >>> .el-button--small {
  width: 32px;
  height: 32px;
}
</style>
