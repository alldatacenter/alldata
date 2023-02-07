<template>
  <el-switch
    v-model="internalValue"
    :active-color="activeColor"
    :active-icon="activeIconComp"
    :active-text="activeText"
    :disabled="disabled"
    :inactive-color="inactiveColor"
    :inactive-icon="inactiveIconComp"
    :inactive-text="inactiveText"
    :loading="loading"
    :width="width"
    :inline-prompt="inlinePrompt"
    @change="onChange"
  />
</template>

<script lang="ts">
import {Component, computed, defineComponent, h, onBeforeMount, PropType, ref, watch} from 'vue';
import Icon from "@/components/icon/Icon.vue";

export default defineComponent({
  name: 'Switch',
  props: {
    modelValue: {
      type: Boolean,
      default: false,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    activeColor: {
      type: String,
      default: 'var(--cl-success-color)',
    },
    inactiveColor: {
      type: String,
      default: 'var(--cl-info-medium-color)',
    },
    activeIcon: {
      type: [String, Array] as PropType<Icon>,
    },
    inactiveIcon: {
      type: [String, Array] as PropType<Icon>,
    },
    activeText: {
      type: String,
      default: '',
    },
    inactiveText: {
      type: String,
      default: '',
    },
    width: {
      type: Number,
      default: 40,
    },
    loading: {
      type: Boolean,
      default: false,
    },
    inlinePrompt: {
      type: Boolean,
      default: false,
    },
  },
  emits: [
    'update:model-value',
    'change',
  ],
  setup(props: SwitchProps, {emit}) {
    const internalValue = ref<boolean>(false);
    watch(() => props.modelValue, () => {
      internalValue.value = props.modelValue;
    });

    const onChange = (value: boolean) => {
      internalValue.value = value;
      emit('update:model-value', value);
      emit('change', value);
    };

    onBeforeMount(() => {
      const {modelValue} = props;
      internalValue.value = modelValue;
    });

    const activeIconComp = computed(() => {
      if (props.activeIcon) {
        return h(Icon, {icon: props.activeIcon});
      }
    });

    const inactiveIconComp = computed(() => {
      if (props.inactiveIcon) {
        return h(Icon, {icon: props.inactiveIcon});
      }
    });

    return {
      internalValue,
      onChange,
      activeIconComp,
      inactiveIconComp,
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
