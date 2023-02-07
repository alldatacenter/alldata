<template>
  <div class="metric-line-chart">
    <div class="metric-line-chart-top">
      <div class="title">
        {{ title }}
      </div>
    </div>
    <cl-line-chart :config="config"/>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent} from 'vue';
import {lineChartProps} from './LineChart.vue';
import {plainClone} from '@/utils/object';

export default defineComponent({
  name: 'MetricLineChart',
  props: {
    ...lineChartProps,
    metric: {
      type: String,
    }
  },
  setup(props: MetricLineChartProps, {emit}) {
    const config = computed<EChartsConfig>(() => {
      const config = plainClone(props.config);
      return Object.assign(config || {}, {
        option: {
          title: {
            show: false,
          },
          tooltip: {
            axisPointer: {
              type: 'cross',
            },
          },
          yAxis: {
            scale: true,
          }
        }
      }) as EChartsConfig;
    });

    const title = computed<string>(() => `Metric: ${props.metric}`);

    return {
      config,
      title,
    };
  }
});
</script>

<style lang="scss" scoped>
.metric-line-chart {
  height: 100%;

  .metric-line-chart-top {
    padding: 10px;

    .title {
      font-size: 18px;
      font-weight: 600;
      color: var(--cl-info-color);
    }
  }
}
</style>
