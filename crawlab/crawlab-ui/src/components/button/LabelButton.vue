<template>
  <cl-button
    :circle="circle"
    :disabled="disabled"
    :plain="plain"
    :round="round"
    :size="size"
    :is-icon="isIcon"
    :tooltip="tooltip"
    :type="type"
    :id="id"
    :class-name="['label-button', className].join(' ')"
    :loading="loading"
    @click="() => $emit('click')"
  >
    <font-awesome-icon v-if="icon" :icon="icon" class="icon"/>
    {{ label }}
  </cl-button>
</template>

<script lang="ts">
import {defineComponent, PropType} from 'vue';
import {buttonProps} from '@/components/button/Button.vue';

export default defineComponent({
  name: 'LabelButton',
  props: {
    icon: {
      type: [Array, String] as PropType<Icon>,
    },
    label: {
      type: String,
      required: true,
    },
    ...buttonProps
  },
  emits: [
    'click',
  ],
  setup(props: LabelButtonProps, {emit}) {
    return {};
  },
});
</script>

<style lang="scss" scoped>
</style>
