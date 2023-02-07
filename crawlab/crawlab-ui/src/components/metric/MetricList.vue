<template>
  <div class="metric-list">
    <div class="sidebar">
      <cl-nav-sidebar
        ref="navSidebarRef"
        type="tree"
        :items="metrics"
        show-checkbox
        :default-checked-keys="checkedKeys"
        no-search
        @check="onCheck"
      />
    </div>

    <div class="content">
      <!--Top-->
      <div class="top">
        <cl-nav-action-back @click="() => $emit('back')"/>
        <slot name="top-prefix"/>
        <cl-form label-width="120px">
          <cl-form-item :label="t('components.metric.filters.metricSource')">
            <el-select
              :model-value="activeMetricSnapshotKey"
              @change="onMetricSnapshotChange"
            >
              <el-option
                v-for="(op, $index) in metricSnapshots"
                :key="$index"
                :label="op.name"
                :value="op.key"
              />
            </el-select>
          </cl-form-item>
          <cl-form-item :label="t('components.metric.filters.timeRange')">
            <cl-date-time-range-picker
              :model-value="dateRange"
              @change="onDateRangeChange"
            />
          </cl-form-item>
          <cl-form-item :label="t('components.metric.filters.timeUnit')">
            <el-select :model-value="duration" @change="onDurationChange">
              <el-option
                v-for="(op, $index) in durationOptions"
                :key="$index"
                :label="op.label"
                :value="op.value"
              />
            </el-select>
          </cl-form-item>
          <slot name="top-suffix"/>
        </cl-form>
      </div>
      <!--./Top-->

      <!--Chart List-->
      <div
        v-if="checkedNormalizedMetrics.length > 0"
        class="chart-list"
      >
        <div
          v-for="(metric, $index) in checkedNormalizedMetrics"
          :key="$index"
          class="metric-chart"
        >
          <cl-line-chart
            height="240px"
            :config="getChartConfig(metric)"
          />
        </div>
      </div>
      <cl-empty
        v-else
        :description="t('components.metric.empty.noMetricsSelected')"
      />
      <!--./Chart List-->
    </div>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, onBeforeMount, onBeforeUnmount, PropType, ref, watch} from 'vue';
import {emptyArrayFunc, voidFunc} from '@/utils/func';
import {cloneArray} from '@/utils/object';
import {translate} from '@/utils/i18n';
import dayjs from 'dayjs';
import {useStore} from 'vuex';

const t = translate;

export default defineComponent({
  name: 'MetricList',
  props: {
    metricSnapshots: {
      type: Array as PropType<MetricSnapshot[]>,
      default: emptyArrayFunc,
    },
    activeMetricSnapshotKey: {
      type: String,
    },
    metrics: {
      type: Array as PropType<NavItem[]>,
      default: emptyArrayFunc,
    },
    metricDataFunc: {
      type: Function as PropType<MetricListDataFunc>,
      default: voidFunc,
    },
    metricTitleFunc: {
      type: Function as PropType<MetricListTitleFunc>,
      default: (metric: NavItem) => metric.id,
    },
    dateRange: {
      type: Object as PropType<RangeItem>,
      default: () => {
        return {
          key: 'past-1h',
        } as RangeItem;
      }
    },
    duration: {
      type: String,
      default: '5m',
    },
    durationOptions: {
      type: Array as PropType<SelectOption[]>,
      default: () => {
        return [
          {label: t('components.date.units.second', {n: 15}), value: '15s'},
          {label: t('components.date.units.second', {n: 30}), value: '30s'},
          {label: t('components.date.units.minute', {n: 1}), value: '1m'},
          {label: t('components.date.units.minute', {n: 5}), value: '5m'},
          {label: t('components.date.units.minute', {n: 15}), value: '15m'},
          {label: t('components.date.units.hour', {n: 1}), value: '1h'},
        ];
      }
    },
    defaultCheckedAll: {
      type: Boolean,
      default: true,
    },
  },
  emits: [
    'date-range-change',
    'duration-change',
    'metric-snapshot-change',
    'back',
  ],
  setup(props: MetricListProps, {emit}) {
    const store = useStore();
    const {
      common: commonState,
    } = store.state as RootStoreState;

    const navSidebarRef = ref();

    const getNormalizedMetrics = (items?: NavItem[]): NavItem[] => {
      let normalizedItems = [] as NavItem[];

      if (items === undefined) {
        items = cloneArray(props.metrics || []);
      }

      items.forEach(item => {
        if (item.children) {
          normalizedItems = normalizedItems.concat(getNormalizedMetrics(item.children));
        } else {
          normalizedItems.push(item);
        }
      });

      return normalizedItems;
    };

    const normalizedMetrics = computed<NavItem[]>(() => getNormalizedMetrics());

    const checkedKeys = ref<string[]>([]);

    const checkedNormalizedMetrics = computed<NavItem[]>(() => normalizedMetrics.value.filter(m => checkedKeys.value.includes(m.id)));

    const chartConfigMap = ref<{ [key: string]: EChartsConfig }>({});

    const initChartConfig = (metric: NavItem) => {
      if (!chartConfigMap.value[metric.id]) {
        chartConfigMap.value[metric.id] = {
          option: {
            title: {
              text: props.metricTitleFunc?.(metric),
            },
            tooltip: {
              formatter: (params: { marker: string; value: [number, number] }) => {
                const time = dayjs.unix(params.value[0] / 1e3).format('YYYY-MM-DD HH:mm:ss');
                return `${time}<br>${params.marker} ${params.value?.[1]?.toLocaleString()}`;
              },
            },
            grid: {
              top: 50,
              bottom: 30,
              right: 20,
            },
            yAxis: {
              scale: true,
            }
          },
        } as EChartsConfig;
      }
    };

    const getChartConfig = (metric: NavItem): EChartsConfig | undefined => {
      initChartConfig(metric);
      return chartConfigMap.value[metric.id];
    };

    const updateAllChartData = async () => {
      await Promise.all(checkedNormalizedMetrics.value.map(metric => updateChartData(metric)));
    };

    const updateChartData = async (metric: NavItem) => {
      initChartConfig(metric);
      chartConfigMap.value[metric.id].data = await props.metricDataFunc?.(metric.id) || [];
    };

    const updateAllChartTitle = async () => {
      await Promise.all(checkedNormalizedMetrics.value.map(metric => updateChartTitle(metric)));
    };

    const updateChartTitle = async (metric: NavItem) => {
      initChartConfig(metric);
      if (chartConfigMap.value[metric.id].option) {
        chartConfigMap.value[metric.id].option.title = props.metricTitleFunc?.(metric);
      }
    };

    const onCheck = (item: NavItem, checked: boolean, items: NavItem[]) => {
      checkedKeys.value = items.map(item => item.id);
    };

    const lang = computed<Lang>(() => {
      // console.debug(commonState.lang);
      return commonState.lang || 'en';
    });

    watch(() => checkedNormalizedMetrics.value.map(m => m.value), updateAllChartData);
    watch(() => props.dateRange, updateAllChartData);
    watch(() => props.duration, updateAllChartData);
    watch(lang, updateAllChartTitle);

    // timer
    let handle: any;
    onBeforeMount(() => {
      updateAllChartData();
      handle = setInterval(updateAllChartData, 60 * 1e3);
    });
    onBeforeUnmount(() => {
      clearInterval(handle);
    });

    const onDateRangeChange = (value: RangeItem) => {
      emit('date-range-change', value);
    };
    const onDurationChange = (value: string) => {
      emit('duration-change', value);
    };

    watch(() => props.metrics, () => {
      if (props.defaultCheckedAll) {
        checkedKeys.value = getNormalizedMetrics(props.metrics).map(m => m.id);
      }
    });

    const activeMetricSnapshot = computed<MetricSnapshot | undefined>(() => {
      return props.metricSnapshots?.find(ms => ms.key === props.activeMetricSnapshotKey);
    });
    const onMetricSnapshotChange = (value: string) => {
      emit('metric-snapshot-change', value);
    };

    return {
      navSidebarRef,
      normalizedMetrics,
      chartConfigMap,
      checkedNormalizedMetrics,
      getChartConfig,
      updateAllChartData,
      onCheck,
      onDateRangeChange,
      onDurationChange,
      checkedKeys,
      activeMetricSnapshot,
      onMetricSnapshotChange,
      t,
    };
  }
});
</script>

<style lang="scss" scoped>
.metric-list {
  display: flex;
  height: 100%;
  background: var(--cl-white);

  .sidebar {
    height: 100%;
    flex: 0 0;
    border-right: 1px solid var(--cl-info-light-color);
  }

  .content {
    height: 100%;
    flex: 1 0;
    overflow-y: auto;

    .top {
      margin: 0;
      padding: 10px 20px;
      border-bottom: 1px solid var(--cl-info-light-color);
      display: flex;
      align-items: center;

      .nav-action-back {
        flex: 0 0 inherit;
      }

      .form {
        flex: 1 0;
        display: flex;

        .form-item {
          margin-right: 10px;
        }
      }
    }

    .chart-list {
      .metric-chart {
        padding: 10px;
        border-bottom: 1px solid var(--cl-info-light-color);
      }
    }
  }
}
</style>

<style scoped>
.metric-list >>> .content .top .form-item .el-form-item {
  margin-bottom: 0;
}
</style>
