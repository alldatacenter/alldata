<template>
  <div class="metric-target-name">
    <cl-nav-link
      :label="computedName"
      @click="onClick"
    />
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, PropType} from 'vue';
import {useI18n} from 'vue-i18n';

export default defineComponent({
  name: 'MetricTargetName',
  props: {
    name: {
      type: String
    },
    type: {
      type: String as PropType<MetricTargetType>,
    },
  },
  emits: [
    'click',
  ],
  setup(props: MetricTargetNameProps, {emit}) {
    const {t} = useI18n();

    const computedName = computed<string | undefined>(() => {
      switch (props.type) {
        case 'node':
          return props.name;
        case 'mongo':
          return t('components.metric.type.mongo');
        case 'seaweedfs':
          return t('components.metric.type.mongo');
        default:
          return '';
      }
    });

    const onClick = () => emit('click');

    return {
      computedName,
      onClick,
    };
  }
});
</script>

<style lang="scss" scoped>
</style>
