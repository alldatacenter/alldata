<template>
  <cl-tag
    :key="data"
    :color="data.color"
    :icon="data.icon"
    :label="data.label"
    :size="size"
    :spinning="data.spinning"
    :tooltip="data.tooltip"
    :type="data.type"
    effect="plain"
    @click="$emit('click')"
  />
</template>

<script lang="ts">
import {computed, defineComponent, PropType} from 'vue';
import {getPriorityLabel} from '@/utils/task';

export default defineComponent({
  name: 'TaskPriority',
  props: {
    priority: {
      type: Number,
      required: false,
      default: 5,
    },
    size: {
      type: String as PropType<BasicSize>,
      required: false,
      default: 'default',
    },
  },
  emits: ['click'],
  setup(props: TaskPriorityProps, {emit}) {
    const data = computed<TagData>(() => {
      const priority = props.priority as number;

      if (priority <= 2) {
        return {
          label: getPriorityLabel(priority),
          color: 'var(--cl-red)',
        };
      } else if (priority <= 4) {
        return {
          label: getPriorityLabel(priority),
          color: 'var(--cl-orange)',
        };
      } else if (priority <= 6) {
        return {
          label: getPriorityLabel(priority),
          color: 'var(--cl-lime-green)',
        };
      } else if (priority <= 8) {
        return {
          label: getPriorityLabel(priority),
          color: 'var(--cl-cyan)',
        };
      } else {
        return {
          label: getPriorityLabel(priority),
          color: 'var(--cl-blue)',
        };
      }
    });

    return {
      data,
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
