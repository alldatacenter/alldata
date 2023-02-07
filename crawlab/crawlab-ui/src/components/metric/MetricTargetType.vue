<template>
  <div class="metric-target-type">
    <cl-node-type
      v-if="type === 'node'"
      :is-master="isMaster"
    />
    <cl-tag
      v-else
      :label="label"
      :type="tagType"
      :icon="icon"
    />
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, PropType} from 'vue';
import Tag from '@/components/tag/Tag.vue';
import {useI18n} from 'vue-i18n';
import NodeType from '@/components/node/NodeType.vue';

export default defineComponent({
  name: 'MetricTargetType',
  props: {
    type: {
      type: String as PropType<MetricTargetType>,
      default: 'node',
    },
    isMaster: {
      type: Boolean
    },
  },
  setup(props: MetricTargetTypeProps, {emit}) {
    const {t} = useI18n();

    const label = computed<string>(() => {
      switch (props.type) {
        case 'node':
          return t('components.metric.type.node');
        case 'mongo':
          return t('components.metric.type.mongo');
        case 'seaweedfs':
          return t('components.metric.type.seaweedfs');
        default:
          return '';
      }
    });

    const icon = computed<Icon>(() => {
      switch (props.type) {
        case 'mongo':
          return ['fa', 'database'];
        case 'seaweedfs':
          return ['fa', 'file'];
        default:
          return '';
      }
    });

    const tagType = computed<BasicType>(() => {
      switch (props.type) {
        case 'mongo':
          return 'success';
        case 'seaweedfs':
          return 'danger';
        default:
          return 'primary';
      }
    });

    return {
      label,
      icon,
      tagType,
    };
  }
});
</script>

<style lang="scss" scoped>
.metric-target-type {
  * {
    margin-right: 10px;
  }
}
</style>
