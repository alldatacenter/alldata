<template>
  <div class="filter-select">
    <label v-if="label" class="label">
      {{ label }}
    </label>
    <el-select
      class="content"
      v-model="internalModelValue"
      :placeholder="placeholder"
      :filterable="filterable"
      clearable
      @clear="onClear"
      @change="onChange"
    >
      <el-option
        v-for="(option, $index) in computedOptions"
        :key="$index"
        :label="option.label"
        :value="option.value"
      />
    </el-select>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, onBeforeMount, PropType, ref} from 'vue';
import useRequest from '@/services/request';
import {cloneArray, prependAllToSelectOptions} from '@/utils';

const {
  get,
} = useRequest();

export default defineComponent({
  name: 'FilterSelect',
  props: {
    label: {
      type: String,
    },
    placeholder: {
      type: String,
    },
    filterable: {
      type: Boolean,
      default: true,
    },
    options: {
      type: Array as PropType<SelectOption[]>,
    },
    optionsRemote: {
      type: Object as PropType<FilterSelectOptionsRemote>,
    },
  },
  emits: [
    'change',
  ],
  setup(props: FilterSelectProps, {emit}) {
    const internalModelValue = ref();
    const internalOptions = ref<SelectOption[]>([]);

    const computedOptions = computed<SelectOption[]>(() => {
      const options = cloneArray(props.options || internalOptions.value || []);
      return prependAllToSelectOptions(options);
    });

    const onChange = (value: any) => {
      if (value === '') return;
      emit('change', value);
    };

    const onClear = () => {
      internalModelValue.value = undefined;
      emit('change', undefined);
    };

    const getOptions = async () => {
      if (!props.optionsRemote) return;
      const {colName, value, label} = props.optionsRemote;
      let url = `/filters/${colName}`;
      if (value) url += `/${value}`;
      if (label) url += `/${label}`;
      const res = await get(url);
      internalOptions.value = res.data;
    };

    onBeforeMount(async () => {
      await getOptions();
    });

    return {
      internalModelValue,
      computedOptions,
      onChange,
      onClear,
    };
  }
});
</script>

<style lang="scss" scoped>
.filter-select {
  display: flex;
  align-items: center;
  flex: 1 0 auto;

  .label {
  }

  .content {
    flex: 1 0 180px;
  }
}
</style>
