<template>
  <cl-button
    :circle="circle"
    :disabled="disabled"
    :plain="plain"
    :round="round"
    :size="size"
    :tooltip="tooltip"
    :type="type"
    is-icon
    :id="id"
    :class-name="cls"
    @click="() => $emit('click')"
  >
    <font-awesome-icon :icon="icon" :spin="spin"/>
    <div v-if="badgeIcon" class="badge-icon">
      <font-awesome-icon :icon="badgeIcon"/>
    </div>
  </cl-button>
</template>

<script lang="ts">
import {computed, defineComponent, PropType} from 'vue';
import {buttonProps} from './Button.vue';

export const faIconButtonProps = {
  icon: {
    type: [Array, String] as PropType<Icon>,
    required: true,
  },
  badgeIcon: {
    type: [Array, String] as PropType<Icon>,
    required: false,
  },
  spin: {
    type: Boolean,
    required: false,
  },
  ...buttonProps,
};

export default defineComponent({
  name: 'FaIconButton',
  props: faIconButtonProps,
  emits: [
    'click',
  ],
  setup(props: FaIconButtonProps) {
    const cls = computed<string>(() => {
      const {className} = props;
      const classes = [
        'fa-icon-button',
      ];
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
.badge-icon {
  position: absolute;
  top: -2px;
  right: 2px;
  font-size: 8px;
  color: var(--cl-white);
}
</style>

<style scoped>
.el-button,
.el-button--mini,
.fa-icon-button,
.fa-icon-button >>> .el-button,
.fa-icon-button >>> .el-button--mini,
.fa-icon-button >>> .button {
  padding: 7px;
}
</style>
