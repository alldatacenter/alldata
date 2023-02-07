<template>
  <cl-table
    :columns="tableColumns"
    :data="tableData"
    :page="tablePagination.page"
    :page-size="tablePagination.size"
    :total="tableTotal"
    :visible-buttons="['']"
    @pagination-change="onPaginationChange"
  />
  <cl-dialog
    :title="t('views.env.deps.task.form.logs')"
    :visible="dialogVisible.logs"
    width="1200px"
    @confirm="onLogsClose"
    @close="onLogsClose"
  >
    <cl-logs-view :logs="logs"/>
  </cl-dialog>
</template>

<script lang="ts">
import {computed, defineComponent, h, onBeforeUnmount, onMounted, ref, watch} from 'vue';
import {useStore} from 'vuex';
import {translate} from '@/utils';
import useRequest from '@/services/request';
import TaskAction from '@/views/env/deps/task/TaskAction.vue';
import NodeType from '@/components/node/NodeType.vue';
import TaskStatus from '@/components/task/TaskStatus.vue';
import Tag from '@/components/tag/Tag.vue';
import Time from '@/components/time/Time.vue';

const t = translate;

const endpoint = '/env/deps/tasks';

const {
  getList: getList_,
} = useRequest();

export default defineComponent({
  name: 'DependencyTaskList',
  props: {
    type: {
      type: String,
    },
  },
  setup(props, {emit}) {
    const store = useStore();

    const dialogVisible = ref({
      logs: false,
    });

    const logs = ref<EnvDepsLog[]>([]);

    const getLogs = async (id: string) => {
      const res = await getList_(`${endpoint}/${id}/logs`);
      const {data} = res;
      logs.value = data?.map(d => d.content) || [];
    };

    let logsHandle: any;

    const onLogsOpen = async (id: string) => {
      await getLogs(id);
      dialogVisible.value.logs = true;
      logsHandle = setInterval(() => getLogs(id), 5000);
    };

    const onLogsClose = () => {
      dialogVisible.value.logs = false;
      clearInterval(logsHandle);
    };

    const allNodeDict = computed(() => store.getters[`node/allDict`]);

    const tableColumns = computed<TableColumns<EnvDepsTask>>(() => [
      {
        key: 'action',
        label: t('views.env.deps.task.form.action'),
        icon: ['fa', 'hammer'],
        width: '120',
        value: (row: EnvDepsTask) => {
          return h(TaskAction, {action: row.action});
        },
      },
      {
        key: 'node',
        label: t('views.env.deps.task.form.node'),
        icon: ['fa', 'server'],
        width: '120',
        value: (row: EnvDepsTask) => {
          const n = allNodeDict.value.get(row.node_id);
          if (!n) return;
          return h(NodeType, {
            isMaster: n.is_master,
            label: n.name,
          });
        },
      },
      {
        key: 'status',
        label: t('views.env.deps.task.form.status'),
        icon: ['fa', 'check-square'],
        width: '120',
        value: (row: EnvDepsTask) => {
          return h(TaskStatus, {status: row.status, error: row.error});
        },
      },
      {
        key: 'dep_names',
        label: t('views.env.deps.task.form.dependencies'),
        icon: ['fa', 'puzzle-piece'],
        width: '380',
        value: (row: EnvDepsTask) => {
          if (!row.dep_names) return [];
          return row.dep_names.map(depName => {
            return h(Tag, {label: depName});
          });
        },
      },
      {
        key: 'update_ts',
        label: t('views.env.deps.task.form.time'),
        icon: ['fa', 'clock'],
        width: '150',
        value: (row: EnvDepsTask) => {
          return h(Time, {time: row.update_ts});
        },
      },
      {
        key: 'actions',
        label: t('components.table.columns.actions'),
        fixed: 'right',
        width: '80',
        buttons: (row: EnvDepsTask) => {
          return [
            {
              type: 'primary',
              icon: ['fa', 'file-alt'],
              tooltip: t('views.env.deps.task.form.logs'),
              onClick: async (row: EnvDepsTask) => {
                await onLogsOpen(row._id as string);
              },
            },
          ];
        },
        disableTransfer: true,
      },
    ]);

    const tableData = ref<TableData<EnvDepsTask>>([]);

    const tablePagination = ref({
      page: 1,
      size: 10,
    });

    const tableTotal = ref(0);

    const onPaginationChange = (pagination: TablePagination) => {
      tablePagination.value = {...pagination};
    };

    const getList = async () => {
      const res = await getList_(`${endpoint}`, {
        ...tablePagination.value,
        conditions: [{
          key: 'type',
          op: 'eq',
          value: props.type,
        }]
      });
      const {data, total} = res;
      tableData.value = data || [];
      tableTotal.value = total || 0;
    };

    watch(() => tablePagination.value.size, getList);
    watch(() => tablePagination.value.page, getList);

    let handle: any;

    onMounted(async () => {
      await getList();
      handle = setInterval(getList, 5000);
    });

    onBeforeUnmount(() => {
      clearInterval(handle);
      clearInterval(logsHandle);
    });

    return {
      dialogVisible,
      tableColumns,
      tableData,
      tablePagination,
      tableTotal,
      onPaginationChange,
      getList,
      logs,
      onLogsClose,
      t,
    };
  },
});
</script>

<style scoped>

</style>
