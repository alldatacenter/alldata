<template>
  <cl-tag
      :key="data"
      :icon="data.icon"
      :label="data.label"
      :size="size"
      :spinning="data.spinning"
      :tooltip="data.tooltip"
      :type="data.type"
      @click="$emit('click')"
  />
</template>

<script lang="ts">
import {computed, defineComponent, PropType} from 'vue';
import Tag from '@/components/tag/Tag.vue';
import {
  NODE_STATUS_OFFLINE,
  NODE_STATUS_ONLINE,
  NODE_STATUS_REGISTERED,
  NODE_STATUS_UNREGISTERED
} from '@/constants/node';
import {useI18n} from 'vue-i18n';

export default defineComponent({
  name: 'NodeStatus',
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
  },
  emits: ['click'],
  setup(props: NodeStatusProps, {emit}) {
    const {t} = useI18n();

    const data = computed<TagData>(() => {
      const {status} = props;
      switch (status) {
        case NODE_STATUS_UNREGISTERED:
          return {
            label: t('components.node.nodeStatus.label.unregistered'),
            tooltip: t('components.node.nodeStatus.tooltip.unregistered'),
            type: 'danger',
            icon: ['fa', 'exclamation'],
          };
        case NODE_STATUS_REGISTERED:
          return {
            label: t('components.node.nodeStatus.label.registered'),
            tooltip: t('components.node.nodeStatus.tooltip.registered'),
            type: 'warning',
            icon: ['far', 'check-square'],
          };
        case NODE_STATUS_ONLINE:
          return {
            label: t('components.node.nodeStatus.label.online'),
            tooltip: t('components.node.nodeStatus.tooltip.online'),
            type: 'success',
            icon: ['fa', 'check'],
          };
        case NODE_STATUS_OFFLINE:
          return {
            label: t('components.node.nodeStatus.label.offline'),
            tooltip: t('components.node.nodeStatus.tooltip.offline'),
            type: 'info',
            icon: ['fa', 'times'],
          };
        default:
          return {
            label: t('components.node.nodeStatus.label.unknown'),
            tooltip: t('components.node.nodeStatus.tooltip.unknown'),
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
  width: 80px;
  cursor: default;

  .icon {
    width: 10px;
    margin-right: 5px;
  }
}
</style>
