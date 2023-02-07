<template>
  <div :style="style" class="line-chart">
    <div ref="elRef" class="echarts-element"></div>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, onMounted, onUnmounted, PropType, ref, watch} from 'vue';
import {init} from 'echarts';
import {translate} from '@/utils/i18n';

export const lineChartProps = {
  config: {
    type: Object as PropType<EChartsConfig>,
    required: true,
  },
  width: {
    type: String,
    default: '100%',
  },
  height: {
    type: String,
    default: '100%',
  },
  theme: {
    type: String,
    default: 'light',
  },
  labelKey: {
    type: String,
  },
  valueKey: {
    type: String,
    default: 'value',
  },
  isTimeSeries: {
    type: Boolean,
    default: true,
  },
};

export default defineComponent({
  name: 'LineChart',
  props: lineChartProps,
  setup(props: LineChartProps, {emit}) {
    const t = translate;

    const style = computed<Partial<CSSStyleDeclaration>>(() => {
      const {width, height} = props;
      return {
        width,
        height,
      };
    });

    const elRef = ref<HTMLDivElement>();
    const chart = ref<ECharts>();

    const isMultiSeries = computed<boolean>(() => {
      const {config} = props;
      const {dataMetas} = config as EChartsConfig;
      return dataMetas ? dataMetas.length > 1 : false;
    });

    const getSeriesData = (data?: StatsResult[], key?: string) => {
      const {valueKey, labelKey, isTimeSeries} = props;
      const _valueKey = !key ? valueKey : key;

      if (_valueKey) {
        if (isTimeSeries) {
          // time series
          return data?.map(d => [d[labelKey || 'date'], d[_valueKey] || 0]);
        } else {
          // not time series
          return data?.map(d => d[_valueKey] || 0);
        }
      } else {
        // default
        return data;
      }
    };

    const getSeries = (): EChartSeries[] => {
      const {config} = props;
      const {data, dataMetas} = config as EChartsConfig;

      if (!isMultiSeries.value) {
        // single series
        return [{
          type: 'line',
          data: getSeriesData(data),
        }];
      } else {
        // multiple series
        const series = [] as EChartSeries[];
        if (!dataMetas) return series;
        dataMetas.forEach(({key, name, yAxisIndex}) => {
          series.push({
            name: t(name),
            yAxisIndex,
            type: 'line',
            data: getSeriesData(data, key),
          });
        });
        return series;
      }
    };

    const render = () => {
      const {config, theme, isTimeSeries} = props;
      const option = {...config?.option};

      // dom
      const el = elRef.value;
      if (!el) return;

      // xAxis
      if (!option.xAxis) {
        option.xAxis = {};
        if (isTimeSeries) {
          option.xAxis.type = 'time';
        }
      }

      // yAxis
      if (!option.yAxis) {
        option.yAxis = {};
      } else if (Array.isArray(option.yAxis)) {
        option.yAxis = option.yAxis.map((d: any) => {
          if (d.name) {
            d.name = t(d.name);
          }
          return d;
        });
      } else {
        if (option.yAxis.name) {
          option.yAxis.name = t(option.yAxis.name);
        }
      }

      // series
      option.series = getSeries();

      // tooltip
      if (!option.tooltip) {
        option.tooltip = {
          // trigger: 'axis',
          // valueFormatter: (value: StatsResult) => {
          //   console.debug(value);
          // },
          // position: ['50%', '50%'],
          // axisPointer: {
          // type: 'cross',
          // },
        };
      }

      // title
      if (option?.title?.text) {
        option.title.text = t(option.title.text);
      }

      // legend
      option.legend = {};

      // debug
      // console.debug(option);

      // render
      if (!chart.value) {
        // init
        chart.value = init(el, theme);
      }
      (chart.value as ECharts).setOption(option);
    };

    const destroy = () => {
      chart.value?.dispose();
    };

    watch(() => props.config?.option, render);
    watch(() => props.config?.data, render);
    watch(() => props.theme, render);

    onMounted(() => {
      render();
    });

    onUnmounted(() => {
      destroy();
    });

    return {
      style,
      elRef,
      render,
    };
  },
});
</script>

<style lang="scss" scoped>
.line-chart {
  .echarts-element {
    width: 100%;
    height: 100%;
  }
}
</style>
