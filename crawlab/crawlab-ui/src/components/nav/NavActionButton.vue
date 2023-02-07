<template>
  <div class="nav-action-button">
    <cl-label-button
      v-if="buttonType === 'label'"
      :disabled="disabled"
      :icon="icon"
      :is-icon="!!icon"
      :label="label"
      :size="size"
      :tooltip="tooltip"
      :type="type"
      :id="id"
      :class-name="className"
      @click="onClick"
    />
    <cl-fa-icon-button
      v-else-if="buttonType === 'fa-icon'"
      :disabled="disabled"
      :icon="icon"
      :size="size"
      :tooltip="tooltip"
      :type="type"
      :id="id"
      :class-name="className"
      @click="onClick"
    />
  </div>
</template>

<script lang="ts">
import {defineComponent, PropType} from 'vue';
import {buttonProps} from '@/components/button/Button.vue';

export default defineComponent({
  name: 'NavActionButton',
  props: {
    buttonType: {
      type: String as PropType<ButtonType>,
      required: true,
    },
    label: {
      type: String,
    },
    icon: {
      type: [String, Array] as PropType<Icon>
    },
    onClick: {
      type: Function as PropType<() => void>
    },
    ...buttonProps,
  },
  setup(props, {emit}) {
    return {};
  },
});
</script>

<style lang="scss" scoped>

</style>
