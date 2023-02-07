<template>
  <div class="filter-input">
    <label v-if="label" class="label">
      {{ label }}
    </label>
    <el-input
      v-model="internalModelValue"
      :placeholder="placeholder"
      clearable
      @clear="onClear"
      @input="onChange"
    />
  </div>
</template>

<script lang="ts">
import {defineComponent, ref} from 'vue';
import {debounce} from 'vue-debounce';

export default defineComponent({
  name: 'FilterInput',
  props: {
    label: {
      type: String,
    },
    placeholder: {
      type: String,
    },
  },
  emits: [
    'change',
  ],
  setup(props: FilterInputProps, {emit}) {
    const internalModelValue = ref();

    const onChange = debounce((value: any) => {
      emit('change', value);
    }, 500);

    const onClear = () => {
      internalModelValue.value = undefined;
      emit('change', internalModelValue.value);
    };

    return {
      internalModelValue,
      onChange,
      onClear,
    };
  }
});
</script>

<style lang="scss" scoped>
</style>
