<template>
  <el-tag :type="type" class="node-type">
    <font-awesome-icon :icon="icon" class="icon"/>
    <span>{{ computedLabel }}</span>
  </el-tag>
</template>

<script lang="ts">
import {computed, defineComponent} from 'vue';
import {useI18n} from 'vue-i18n';

export default defineComponent({
  name: 'NodeType',
  props: {
    isMaster: {
      type: Boolean,
    },
    label: {
      type: String,
    },
  },
  setup(props: NodeTypeProps, {emit}) {
    const {t} = useI18n();

    const type = computed<string>(() => {
      const {isMaster} = props;
      return isMaster ? '' : 'warning';
    });

    const computedLabel = computed<string>(() => {
      const {isMaster, label} = props;
      if (label) return label;
      return isMaster ?
        t('components.node.nodeType.label.master') :
        t('components.node.nodeType.label.worker');
    });

    const icon = computed<string[]>(() => {
      const {isMaster} = props;
      return isMaster ? ['fa', 'home'] : ['fa', 'server'];
    });

    return {
      type,
      computedLabel,
      icon,
    };
  },
});
</script>

<style lang="scss" scoped>
.node-type {
  cursor: pointer;

  .icon {
    margin-right: 5px;
  }
}
</style>
