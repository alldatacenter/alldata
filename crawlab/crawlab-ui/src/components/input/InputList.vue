<template>
  <div class="input-list">
    <div
      class="input-list-item"
      v-for="(v, $index) in internalModelValue"
      :key="$index"
    >
      <el-input
        v-model="internalModelValue[$index]"
        :placeholder="placeholder"
        :size="size"
        :disabled="disabled"
        @change="getOnChangeFn($index)"
      />
      <div class="actions">
        <cl-tag
          clickable
          :icon="['fa', 'plus']"
          :size="actionSize"
          :disabled="disabled"
          effect="plain"
          type="info"
          :tooltip="t('common.actions.add')"
          @click="() => onAdd($index)"
        />
        <cl-tag
          clickable
          :icon="['fa', 'xmark']"
          :size="actionSize"
          :disabled="disabled"
          effect="plain"
          type="info"
          :tooltip="t('common.actions.delete')"
          @click="() => onDelete($index)"
        />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {defineComponent, PropType, ref, watch} from 'vue';
import {cloneArray, translate} from '@/utils';

// i18n
const t = translate;

export default defineComponent({
  name: 'InputList',
  props: {
    modelValue: {
      type: Array as PropType<string[]>,
      default: () => {
        return [''];
      },
    },
    placeholder: {
      type: String,
      default: '',
    },
    size: {
      type: String as PropType<BasicType>,
    },
    actionSize: {
      type: String as PropType<BasicType>,
      default: 'small',
    },
    disabled: {
      type: Boolean,
      default: false,
    },
  },
  emits: [
    'update:model-value'
  ],
  setup(props: InputListProps, {emit}) {
    const internalModelValue = ref(props.modelValue);

    const getOnChangeFn = (index: number) => {
      return (value: string) => {
        const newModelValue = cloneArray(internalModelValue.value || []);
        newModelValue[index] = value;
        emit('update:model-value', newModelValue);
      };
    };

    const onAdd = (index: number) => {
      const newModelValue = cloneArray(internalModelValue.value || []);
      newModelValue.splice(index + 1, 0, '');
      internalModelValue.value = newModelValue;
      emit('update:model-value', newModelValue);
    };

    const onDelete = (index: number) => {
      const newModelValue = cloneArray(internalModelValue.value || []);
      newModelValue.splice(index, 1);
      if (newModelValue.length === 0) {
        newModelValue.push('');
      }
      internalModelValue.value = newModelValue;
      emit('update:model-value', newModelValue);
    };

    watch(() => props.modelValue, () => {
      internalModelValue.value = props.modelValue || [''];
    });

    return {
      internalModelValue,
      getOnChangeFn,
      onAdd,
      onDelete,
      t,
    };
  }
});
</script>

<style lang="scss" scoped>
.input-list {
  display: flex;
  flex-direction: column;

  .input-list-item {
    display: flex;
    align-items: center;

    &:not(:first-child) {
      margin-top: 5px;
    }

    .el-input {
      flex: 1 0;
    }

    .actions {
      padding-left: 5px;
    }
  }
}
</style>

<style scoped>
.input-list >>> .actions .tag:not(:first-child) {
  margin-left: 5px;
}
</style>
