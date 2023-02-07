<template>
  <div class="range-picker" :class="className">
    <el-select
      v-model="internalValue"
      @change="onChange"
    >
      <el-option
        v-for="(op, $index) in options"
        :key="$index"
        :label="op.label"
        :value="op.value"
      />
    </el-select>
    <el-date-picker
      v-if="internalValue?.key === 'custom'"
      v-model="internalRange"
      :type="type"
      @change="onChange"
    />
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, onBeforeMount, PropType, ref, watch} from 'vue';
import {emptyArrayFunc} from '@/utils/func';

export const rangePickerProps = {
  className: {
    type: String,
  },
  type: {
    type: String as PropType<RangePickerType>,
    default: 'daterange',
  },
  modelValue: {
    type: Object as PropType<RangeItem>,
  },
  options: {
    type: Array as PropType<RangeItemOption[]>,
    default: emptyArrayFunc,
  },
};

export const getRangeItemOption = (label: string, key: RangeItemKey, value: RangeItemValue): RangeItemOption => {
  return {
    label,
    value: {
      key,
      value,
    }
  } as RangeItemOption;
};

export default defineComponent({
  name: 'RangePicker',
  props: {
    ...rangePickerProps,
  },
  emits: [
    'change',
  ],
  setup(props: RangePickerProps, {emit}) {
    const internalValue = ref<RangeItem>();
    const internalRange = ref<DateRange>();

    const selectedValue = computed<RangeItem | undefined>(() => {
      return props.options?.find(op => op.value?.key === props.modelValue?.key)?.value;
    });

    const onChange = () => {
      emit('change', internalValue.value);
    };

    const updateInternalValue = () => {
      internalValue.value = selectedValue.value;
    };
    onBeforeMount(updateInternalValue);
    watch(() => props.modelValue, updateInternalValue);

    watch(() => internalRange.value, () => {
      if (!internalValue.value) return;
      internalValue.value.value = internalRange.value;
    });

    return {
      internalValue,
      internalRange,
      onChange,
    };
  }
});
</script>

<style lang="scss" scoped>
.range-picker {
  display: flex;
  align-items: center;

  .el-select {
    margin-right: 10px;
  }
}
</style>
