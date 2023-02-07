<template>
  <div class="plugin-status">
    <cl-tag
      v-for="(t, $index) in tags"
      :key="$index"
      :icon="t.icon"
      :label="t.label"
      :spinning="t.spinning"
      :type="t.type"
      :size="size"
      @click="$emit('click')"
    >
      <template #tooltip>
        <div v-html="t.tooltip"/>
      </template>
    </cl-tag>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, PropType} from 'vue';
import {
  PLUGIN_STATUS_INSTALLING,
  PLUGIN_STATUS_STOPPED,
  PLUGIN_STATUS_RUNNING,
  PLUGIN_STATUS_ERROR,
} from '@/constants/plugin';
import {emptyArrayFunc} from '@/utils/func';
import {useI18n} from 'vue-i18n';

const statusList = [
  PLUGIN_STATUS_INSTALLING,
  PLUGIN_STATUS_STOPPED,
  PLUGIN_STATUS_RUNNING,
  PLUGIN_STATUS_ERROR,
];

export default defineComponent({
  name: 'PluginStatusMultiNode',
  props: {
    status: {
      type: Array as PropType<PluginStatus[]>,
      required: false,
      default: emptyArrayFunc,
    },
    size: {
      type: String as PropType<BasicSize>,
      required: false,
      default: 'default',
    },
  },
  emits: ['click'],
  setup(props: PluginStatusMultiNodeProps, {emit}) {
    // i18n
    const {t} = useI18n();

    const tags = computed<TagData[]>(() => {
      const {status} = props;
      if (!status) return [];
      const dict = {} as { [key: string]: PluginStatus[] };
      status.forEach(s => {
        if (!dict[s.status]) {
          dict[s.status] = [];
        }
        dict[s.status].push(s);
      });
      const statusKeys = Object.keys(dict);
      statusKeys.sort((a, b) => {
        return statusList.indexOf(a) > statusList.indexOf(b) ? 1 : -1;
      });
      const tags = statusKeys.map(s => {
        const status = dict[s];
        const tooltipNodes = status.map(_s => _s.node?.name).join('<br>');
        switch (s) {
          case PLUGIN_STATUS_INSTALLING:
            return {
              label: status.length.toString(),
              tooltip: `<span style="color: #e6a23c">${t('components.plugin.status.nodes.tooltip.installing')}:</span><br>${tooltipNodes}`,
              type: 'warning',
              icon: ['fa', 'spinner'],
              spinning: true,
            };
          case PLUGIN_STATUS_STOPPED:
            return {
              label: status.length.toString(),
              tooltip: `<span style="color: #909399">${t('components.plugin.status.nodes.tooltip.stopped')}:</span><br>${tooltipNodes}`,
              type: 'info',
              icon: ['fa', 'stop'],
            };
          case PLUGIN_STATUS_RUNNING:
            return {
              label: status.length.toString(),
              tooltip: `<span style="color: #67c23a">${t('components.plugin.status.nodes.tooltip.running')}:</span><br>${tooltipNodes}`,
              type: 'success',
              icon: ['fa', 'check'],
            };
          case PLUGIN_STATUS_ERROR:
            return {
              label: status.length.toString(),
              tooltip: `<span style="color: #f56c6c">${t('components.plugin.status.nodes.tooltip.error')}:</span><br>${tooltipNodes}`,
              type: 'danger',
              icon: ['fa', 'times'],
            };
          default:
            return {
              label: status.length.toString(),
              tooltip: `<span style="color: #909399">${t('components.plugin.status.nodes.tooltip.unknown')}:</span><br>${tooltipNodes}`,
              type: 'info',
              icon: ['fa', 'question'],
            };
        }
      }) as TagData[];
      return tags;
    });

    return {
      tags,
    };
  },
});
</script>

<style lang="scss" scoped>
.plugin-status {
}
</style>
