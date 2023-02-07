<template>
  <cl-tag
    class-name="task-status"
    :key="data"
    :icon="data.icon"
    :label="data.label"
    :spinning="data.spinning"
    :type="data.type"
    :size="size"
    @click="$emit('click')"
  >
    <template #tooltip>
      <div v-html="data.tooltip"/>
    </template>
  </cl-tag>
</template>

<script lang="ts">
import {computed, defineComponent, PropType} from 'vue';
import {
  TASK_STATUS_ABNORMAL,
  TASK_STATUS_CANCELLED,
  TASK_STATUS_ERROR,
  TASK_STATUS_FINISHED,
  TASK_STATUS_PENDING,
  TASK_STATUS_RUNNING
} from '@/constants/task';
import {useI18n} from 'vue-i18n';

export default defineComponent({
  name: 'TaskStatus',
  props: {
    status: {
      type: String as PropType<TaskStatus>,
      required: false,
    },
    size: {
      type: String as PropType<BasicSize>,
      required: false,
      default: 'default',
    },
    error: {
      type: String,
      required: false,
    }
  },
  emits: ['click'],
  setup(props: TaskStatusProps, {emit}) {
    // i18n
    const {t} = useI18n();

    const data = computed<TagData>(() => {
      const {status, error} = props;
      switch (status) {
        case TASK_STATUS_PENDING:
          return {
            label: t('components.task.status.label.pending'),
            tooltip: t('components.task.status.tooltip.pending'),
            type: '',
            icon: ['fa', 'hourglass-start'],
            spinning: true,
          };
        case TASK_STATUS_RUNNING:
          return {
            label: t('components.task.status.label.running'),
            tooltip: t('components.task.status.tooltip.running'),
            type: 'warning',
            icon: ['fa', 'spinner'],
            spinning: true,
          };
        case TASK_STATUS_FINISHED:
          return {
            label: t('components.task.status.label.finished'),
            tooltip: t('components.task.status.tooltip.finished'),
            type: 'success',
            icon: ['fa', 'check'],
          };
        case TASK_STATUS_ERROR:
          return {
            label: t('components.task.status.label.error'),
            tooltip: `${t('components.task.status.tooltip.error')}<br><span style="color: 'var(--cl-red)">${error}</span>`,
            type: 'danger',
            icon: ['fa', 'times'],
          };
        case TASK_STATUS_CANCELLED:
          return {
            label: t('components.task.status.label.cancelled'),
            tooltip: t('components.task.status.tooltip.cancelled'),
            type: 'info',
            icon: ['fa', 'ban'],
          };
        case TASK_STATUS_ABNORMAL:
          return {
            label: t('components.task.status.label.abnormal'),
            tooltip: t('components.task.status.tooltip.abnormal'),
            type: 'info',
            icon: ['fa', 'exclamation'],
          };
        default:
          return {
            label: t('components.task.status.label.unknown'),
            tooltip: t('components.task.status.tooltip.unknown'),
            type: 'info',
            icon: ['fa', 'question'],
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
.task-status {
}
</style>
