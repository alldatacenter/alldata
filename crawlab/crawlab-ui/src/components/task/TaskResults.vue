<template>
  <cl-tag
    :key="data"
    :icon="data.icon"
    :label="data.label"
    :size="size"
    :spinning="data.spinning"
    :type="data.type"
    class-name="task-results"
    :clickable="clickable"
    @click="onClick"
  >
    <template #tooltip>
      <div v-html="data.tooltip"/>
    </template>
  </cl-tag>
</template>

<script lang="ts">
import {computed, defineComponent, PropType} from 'vue';
import {isCancellable} from '@/utils/task';
import {TASK_STATUS_PENDING} from '@/constants/task';
import {useI18n} from 'vue-i18n';
import {voidFunc} from '@/utils';

export default defineComponent({
  name: 'TaskResults',
  props: {
    results: {
      type: Number,
      required: false,
    },
    status: {
      type: String as PropType<TaskStatus>,
      required: false,
    },
    size: {
      type: String as PropType<BasicSize>,
      required: false,
      default: 'default',
    },
    clickable: {
      type: Boolean,
    },
    onClick: {
      type: Function,
      default: voidFunc,
    }
  },
  setup(props: TaskResultsProps, {emit}) {
    const {t} = useI18n();

    const data = computed<TagData>(() => {
      const {results, status} = props;
      if (isCancellable(status)) {
        if (status === TASK_STATUS_PENDING) {
          return {
            label: results?.toFixed(0),
            tooltip: `${t('components.task.results.results')}: ${results}`,
            type: 'primary',
            icon: ['fa', 'hourglass-start'],
            spinning: true,
          };
        } else {
          return {
            label: results?.toFixed(0),
            tooltip: `${t('components.task.results.results')}: ${results}`,
            type: 'warning',
            icon: ['fa', 'spinner'],
            spinning: true,
          };
        }
      } else {
        if (results === 0) {
          return {
            label: results?.toFixed(0),
            tooltip: t('components.task.results.noResults'),
            type: 'danger',
            icon: ['fa', 'exclamation'],
          };
        } else {
          return {
            label: results?.toFixed(0),
            tooltip: `${t('components.task.results.results')}: ${results}`,
            type: 'success',
            icon: ['fa', 'table'],
          };
        }
      }
    });

    return {
      data,
    };
  },
});
</script>

<style lang="scss" scoped>
.task-results {
  &.clickable {
    cursor: pointer;
  }
}
</style>
