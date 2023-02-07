<template>
  <div class="metric-snapshot">
    <template v-if="snapshot.type === 'node'">
      <cl-metric-progress
        type="dashboard"
        :width="80"
        :percentage="snapshot.metrics?.['performance:node:cpu:percent']?.value"
        :format="format"
        :label="{key: 'cpu', title: t('components.metric.snapshot.node.cpu')}"
        :label-icon="['fa', 'microchip']"
        :status="status.node.cpu"
        :detail-metrics="getChildMetrics(':node:cpu:')"
      />
      <cl-metric-progress
        type="dashboard"
        :width="80"
        :percentage="snapshot.metrics?.['performance:node:mem:used_percent']?.value"
        :format="format"
        :label="{key: 'mem', title: t('components.metric.snapshot.node.mem')}"
        :label-icon="['fa', 'memory']"
        :status="status.node.mem"
        :detail-metrics="getChildMetrics(':node:mem:')"
      />
      <cl-metric-progress
        type="dashboard"
        :width="80"
        :percentage="snapshot.metrics?.['performance:node:disk:used_percent']?.value"
        :format="format"
        :label="{key: 'disk', title: t('components.metric.snapshot.node.disk')}"
        :label-icon="['fa', 'hard-drive']"
        :status="status.node.disk"
        :detail-metrics="getChildMetrics(':node:disk:')"
      />
      <cl-metric-progress
        v-if="false"
        type="dashboard"
        :width="80"
        :percentage="null && snapshot.metrics?.['performance:node:net:io_bytes_recv_rate']?.value"
        :format="format"
        :label="{key: 'net', title: t('components.metric.snapshot.node.net')}"
        :label-icon="['fa', 'wifi']"
        :status="status.net"
        :detail-metrics="getChildMetrics(':node:net:')"
      />
    </template>

    <template v-else-if="snapshot.type === 'mongo'">
      <cl-metric-progress
        type="dashboard"
        :width="80"
        :percentage="snapshot.metrics?.['performance:mongo:size:fs_used_size_percent']?.value"
        :format="format"
        :label="{key: 'fs', title: t('components.metric.snapshot.mongo.fs')}"
        :label-icon="['fa', 'hard-drive']"
        :status="status.mongo.size"
        :detail-metrics="getChildMetrics(':mongo:size:fs')"
      />
      <cl-metric-progress
        type="dashboard"
        :width="80"
        :percentage="snapshot.metrics?.['performance:mongo:size:total_size_percent']?.value"
        :format="format"
        :label="{key: 'db', title: t('components.metric.snapshot.mongo.db')}"
        :label-icon="['fa', 'database']"
        :status="status.mongo.size"
        :detail-metrics="getChildMetrics(':mongo:size:')"
      />
    </template>

    <template v-else-if="snapshot.type === 'seaweedfs'">

    </template>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, PropType, readonly} from 'vue';
import {emptyObjectFunc} from '@/utils/func';
import {useI18n} from 'vue-i18n';

export default defineComponent({
  name: 'MetricSnapshot',
  props: {
    snapshot: {
      type: Object as PropType<MetricSnapshot>,
      default: emptyObjectFunc,
    },
  },
  setup(props: MetricSnapshotProps, {emit}) {
    const {t} = useI18n();

    const format = computed(() => {
      return (percentage: number | null): string => {
        if (percentage === null) return 'N/A';
        return `${percentage.toFixed(0)}%`;
      };
    });

    const getDetailMetrics = (key: string): SelectOption[] => {
      return Object.keys(props.snapshot?.metrics || [])
        .filter(n => n.match(key))
        .map(n => {
          const m = props.snapshot?.metrics?.[n] as Metric;
          return {
            label: n,
            value: m.value?.toLocaleString(),
          };
        });
    };

    const statusItems = readonly<{ [key: string]: MetricProgressStatusData }>({
      danger: {
        color: 'var(--cl-danger-color)',
        icon: ['fa', 'circle-exclamation'],
        label: t('components.metric.status.danger')
      },
      warning: {
        color: 'var(--cl-warning-color)',
        icon: ['fa', 'triangle-exclamation'],
        label: t('components.metric.status.warning')
      },
      healthy: {
        color: 'var(--cl-success-color)',
        icon: ['fa', 'circle-check'],
        label: t('components.metric.status.healthy')
      },
      unknown: {
        color: 'var(--cl-info-medium-color)',
        icon: ['fa', 'circle-question'],
        label: t('components.metric.status.unknown')
      },
    });

    const status = readonly<{ [key: string]: { [key: string]: MetricProgressStatus } }>({
      node: {
        cpu: (p: number | null) => {
          if (p === null) {
            return statusItems.unknown as MetricProgressStatusData;
          } else if (p >= 80) {
            return statusItems.danger as MetricProgressStatusData;
          } else if (p >= 50) {
            return statusItems.warning as MetricProgressStatusData;
          } else {
            return statusItems.healthy as MetricProgressStatusData;
          }
        },
        mem: (p: number | null) => {
          if (p === null) {
            return statusItems.unknown as MetricProgressStatusData;
          } else if (p >= 80) {
            return statusItems.danger as MetricProgressStatusData;
          } else if (p >= 50) {
            return statusItems.warning as MetricProgressStatusData;
          } else {
            return statusItems.healthy as MetricProgressStatusData;
          }
        },
        disk: (p: number | null) => {
          if (p === null) {
            return statusItems.unknown as MetricProgressStatusData;
          } else if (p >= 80) {
            return statusItems.danger as MetricProgressStatusData;
          } else if (p >= 50) {
            return statusItems.warning as MetricProgressStatusData;
          } else {
            return statusItems.healthy as MetricProgressStatusData;
          }
        },
        net: (p: number | null) => {
          if (p === null) {
            return statusItems.unknown as MetricProgressStatusData;
          } else if (p >= 80) {
            return statusItems.danger as MetricProgressStatusData;
          } else if (p >= 50) {
            return statusItems.warning as MetricProgressStatusData;
          } else {
            return statusItems.healthy as MetricProgressStatusData;
          }
        },
      },
      mongo: {
        size: (p: number | null) => {
          if (p === null) {
            return statusItems.unknown as MetricProgressStatusData;
          } else if (p >= 80) {
            return statusItems.danger as MetricProgressStatusData;
          } else if (p >= 50) {
            return statusItems.warning as MetricProgressStatusData;
          } else {
            return statusItems.healthy as MetricProgressStatusData;
          }
        },
      }
    });

    return {
      format,
      getChildMetrics: getDetailMetrics,
      status,
      t,
    };
  }
});
</script>

<style lang="scss" scoped>
.metric-snapshot {
  .metric-progress {
    margin-right: 20px;
  }
}
</style>

<style scoped>
</style>
