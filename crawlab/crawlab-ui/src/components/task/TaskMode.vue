<template>
  <cl-tag
    :type="type"
    :icon="icon"
    :label="label"
    :tooltip="tooltip"
  />
</template>

<script lang="ts">
import {computed, defineComponent} from 'vue';
import {
  TASK_MODE_ALL_NODES,
  TASK_MODE_RANDOM,
  TASK_MODE_SELECTED_NODE_TAGS,
  TASK_MODE_SELECTED_NODES
} from '@/constants/task';
import {useI18n} from 'vue-i18n';

export default defineComponent({
  name: 'TaskMode',
  props: {
    mode: {
      type: String,
      required: false,
    }
  },
  setup(props: TaskModeProps, {emit}) {
    // i18n
    const {t} = useI18n();

    const type = computed<string>(() => {
      const {mode} = props;
      switch (mode) {
        case TASK_MODE_RANDOM:
          return 'warning';
        case TASK_MODE_ALL_NODES:
          return 'success';
        case TASK_MODE_SELECTED_NODES:
          return 'primary';
        case TASK_MODE_SELECTED_NODE_TAGS:
          return 'primary';
        default:
          return 'info';
      }
    });

    const label = computed<string>(() => {
      const {mode} = props;
      switch (mode) {
        case TASK_MODE_RANDOM:
          return t('components.task.mode.label.randomNode');
        case TASK_MODE_ALL_NODES:
          return t('components.task.mode.label.allNodes');
        case TASK_MODE_SELECTED_NODES:
          return t('components.task.mode.label.selectedNodes');
        case TASK_MODE_SELECTED_NODE_TAGS:
          return t('components.task.mode.label.selectedTags');
        default:
          return t('components.task.mode.label.unknown');
      }
    });

    const icon = computed<string[]>(() => {
      const {mode} = props;
      switch (mode) {
        case TASK_MODE_RANDOM:
          return ['fa', 'random'];
        case TASK_MODE_ALL_NODES:
          return ['fa', 'sitemap'];
        case TASK_MODE_SELECTED_NODES:
          return ['fa', 'network-wired'];
        case TASK_MODE_SELECTED_NODE_TAGS:
          return ['fa', 'tags'];
        default:
          return ['fa', 'question'];
      }
    });

    const tooltip = computed<string>(() => {
      const {mode} = props;
      switch (mode) {
        case TASK_MODE_RANDOM:
          return t('components.task.mode.tooltip.randomNode');
        case TASK_MODE_ALL_NODES:
          return t('components.task.mode.tooltip.allNodes');
        case TASK_MODE_SELECTED_NODES:
          return t('components.task.mode.tooltip.selectedNodes');
        case TASK_MODE_SELECTED_NODE_TAGS:
          return t('components.task.mode.tooltip.selectedTags');
        default:
          return t('components.task.mode.tooltip.unknown');
      }
    });

    return {
      type,
      label,
      icon,
      tooltip,
    };
  },
});
</script>

<style lang="scss" scoped>
.task-mode {
  min-width: 80px;
  cursor: default;

  .icon {
    width: 10px;
    margin-right: 5px;
  }
}
</style>
