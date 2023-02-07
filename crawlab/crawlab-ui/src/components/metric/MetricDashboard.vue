<template>
  <div class="metric-dashboard">
    <cl-table
      :data="tableData"
      :columns="tableColumns"
      hide-footer
    />
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, onBeforeUnmount, onBeforeMount, PropType, ref, h} from 'vue';
import {useI18n} from 'vue-i18n';
import MetricTargetType from '@/components/metric/MetricTargetType.vue';
import NodeStatus from '@/components/node/NodeStatus.vue';
import MetricTargetName from '@/components/metric/MetricTargetName.vue';
import MetricSnapshotComp from '@/components/metric/MetricSnapshot.vue';
import {TABLE_COLUMN_NAME_ACTIONS} from '@/constants/table';

export default defineComponent({
  name: 'MetricDashboard',
  props: {
    metricDataFunc: {
      type: Function as PropType<MetricDashboardDataFunc>,
    },
  },
  emits: [
    'row-click',
  ],
  setup(props: MetricDashboardProps, {emit}) {
    const {t} = useI18n();

    const tableData = ref<TableData<MetricSnapshot>>([]);
    const tableColumns = computed<TableColumns<MetricSnapshot>>(() => {
      return [
        {
          key: 'name',
          label: t('components.metric.dashboard.columns.name'),
          icon: ['fa', 'font'],
          width: '180',
          value: (row: MetricSnapshot) => h(MetricTargetName, {
            name: row.name,
            type: row.type,
            onClick: () => emit('row-click', row)
          }),
        },
        {
          key: 'type',
          label: t('components.metric.dashboard.columns.type'),
          icon: ['fa', 'font'],
          width: '180',
          value: (row: MetricSnapshot) => h(MetricTargetType, {type: row.type, isMaster: row.is_master}),
        },
        {
          key: 'status',
          label: t('components.metric.dashboard.columns.status'),
          icon: ['fa', 'heartbeat'],
          width: '120',
          value: (row: MetricSnapshot) => h(NodeStatus, {status: row.status}),
        },
        {
          key: 'metrics',
          label: t('components.metric.dashboard.columns.metrics.title'),
          icon: ['fa', 'chart-line'],
          width: 'auto',
          value: (row: MetricSnapshot) => h(MetricSnapshotComp, {snapshot: row}),
        },
        {
          key: TABLE_COLUMN_NAME_ACTIONS,
          label: t('components.table.columns.actions'),
          icon: ['fa', 'tools'],
          fixed: 'right',
          width: '200',
          buttons: [
            {
              type: 'primary',
              icon: ['fa', 'search'],
              tooltip: t('common.actions.view'),
              onClick: (row: MetricSnapshot) => emit('row-click', row),
            },
          ],
        }
      ] as TableColumns;
    });

    const getData = async () => {
      tableData.value = await props.metricDataFunc?.() || [];
    };

    // timer
    let handle: any;
    onBeforeMount(() => {
      getData();
      handle = setInterval(getData, 15 * 1e3);
    });
    onBeforeUnmount(() => {
      clearInterval(handle);
    });

    return {
      tableData,
      tableColumns,
    };
  }
});
</script>

<style lang="scss" scoped>
.metric-dashboard {
  width: 100%;

  .table {
    width: 100%;
  }
}
</style>
