import useList from '@/layouts/content/list/list';
import {useStore} from 'vuex';
import {computed, h} from 'vue';
import NavLink from '@/components/nav/NavLink.vue';
import TaskStatus from '@/components/task/TaskStatus.vue';
import {TABLE_COLUMN_NAME_ACTIONS} from '@/constants/table';
import {useRouter} from 'vue-router';
import {ElMessage, ElMessageBox} from 'element-plus';
import useRequest from '@/services/request';
import TaskPriority from '@/components/task/TaskPriority.vue';
import NodeType from '@/components/node/NodeType.vue';
import Time from '@/components/time/Time.vue';
import Duration from '@/components/time/Duration.vue';
import {onListFilterChangeByKey, setupListComponent} from '@/utils/list';
import {getStatusOptions, isCancellable} from '@/utils/task';
import TaskResults from '@/components/task/TaskResults.vue';
import useNode from '@/components/node/node';
import useSpider from '@/components/spider/spider';
import useTask from '@/components/task/task';
import {translate} from '@/utils/i18n';
import {sendEvent} from '@/admin/umeng';
import useSchedule from '@/components/schedule/schedule';
import TaskCommand from '@/components/task/TaskCommand.vue';
import {
  ACTION_ADD,
  ACTION_CANCEL,
  ACTION_DELETE,
  ACTION_FILTER,
  ACTION_FILTER_SEARCH,
  ACTION_FILTER_SELECT,
  ACTION_RESTART,
  ACTION_VIEW, ACTION_VIEW_DATA,
  ACTION_VIEW_LOGS,
  FILTER_OP_CONTAINS,
  FILTER_OP_EQUAL,
} from '@/constants';

const {
  post,
} = useRequest();

const useTaskList = () => {
  // i18n
  const t = translate;

  // store
  const ns = 'task';
  const store = useStore<RootStoreState>();
  const {commit} = store;

  // router
  const router = useRouter();

  // use list
  const {
    actionFunctions,
  } = useList<Task>(ns, store);

  const {
    priorityOptions,
  } = useTask(store);

  // action functions
  const {
    deleteByIdConfirm,
  } = actionFunctions;

  // all node dict
  const allNodeDict = computed<Map<string, CNode>>(() => store.getters['node/allDict']);

  // all spider dict
  const allSpiderDict = computed<Map<string, Spider>>(() => store.getters['spider/allDict']);

  // all schedule dict
  const allScheduleDict = computed<Map<string, Schedule>>(() => store.getters['schedule/allDict']);

  // nav actions
  const navActions = computed<ListActionGroup[]>(() => [
    {
      name: 'common',
      children: [
        {
          action: ACTION_ADD,
          id: 'add-btn',
          className: 'add-btn',
          buttonType: 'label',
          label: t('views.tasks.navActions.new.label'),
          tooltip: t('views.tasks.navActions.new.tooltip'),
          icon: ['fa', 'plus'],
          type: 'success',
          onClick: () => {
            commit(`${ns}/showDialog`, 'create');

            sendEvent('click_task_list_new');
          }
        },
      ]
    },
    {
      action: ACTION_FILTER,
      name: 'filter',
      children: [
        {
          action: ACTION_FILTER_SEARCH,
          id: 'filter-search',
          className: 'search',
          placeholder: t('views.tasks.navActions.filter.search.placeholder'),
          onChange: onListFilterChangeByKey(store, ns, 'name', FILTER_OP_CONTAINS),
        },
        {
          action: ACTION_FILTER_SELECT,
          id: 'filter-select-node',
          className: 'filter-select-node',
          label: t('views.tasks.navActionsExtra.filter.select.node.label'),
          optionsRemote: {
            colName: 'nodes',
          },
          onChange: onListFilterChangeByKey(store, ns, 'node_id', FILTER_OP_EQUAL),
        },
        {
          action: ACTION_FILTER_SELECT,
          id: 'filter-select-spider',
          className: 'filter-select-spider',
          label: t('views.tasks.navActionsExtra.filter.select.spider.label'),
          optionsRemote: {
            colName: 'spiders',
          },
          onChange: onListFilterChangeByKey(store, ns, 'spider_id', FILTER_OP_EQUAL),
        },
        {
          action: ACTION_FILTER_SELECT,
          id: 'filter-select-schedule',
          className: 'filter-select-schedule',
          label: t('views.tasks.navActionsExtra.filter.select.schedule.label'),
          optionsRemote: {
            colName: 'schedules',
          },
          onChange: onListFilterChangeByKey(store, ns, 'schedule_id', FILTER_OP_EQUAL),
        },
        {
          action: ACTION_FILTER_SELECT,
          id: 'filter-select-priority',
          className: 'filter-select-priority',
          label: t('views.tasks.navActionsExtra.filter.select.priority.label'),
          options: priorityOptions,
          onChange: onListFilterChangeByKey(store, ns, 'priority', FILTER_OP_EQUAL),
        },
        {
          action: ACTION_FILTER_SEARCH,
          id: 'filter-search-cmd',
          className: 'search-cmd',
          placeholder: t('views.tasks.navActionsExtra.filter.search.cmd.placeholder'),
          onChange: onListFilterChangeByKey(store, ns, 'cmd', FILTER_OP_CONTAINS),
        },
        {
          action: ACTION_FILTER_SELECT,
          id: 'filter-select-status',
          className: 'filter-select-status',
          label: t('views.tasks.navActionsExtra.filter.select.status.label'),
          options: getStatusOptions(),
          onChange: onListFilterChangeByKey(store, ns, 'status', FILTER_OP_EQUAL),
        },
      ]
    }
  ]);

  const {
    allListSelectOptions: allNodeListSelectOptions,
  } = useNode(store);

  const {
    allListSelectOptions: allSpiderListSelectOptions,
  } = useSpider(store);

  const {
    allListSelectOptions: allScheduleListSelectOptions,
  } = useSchedule(store);

  // table columns
  const tableColumns = computed<TableColumns<Task>>(() => [
    {
      key: 'node_id',
      label: t('views.tasks.table.columns.node'),
      icon: ['fa', 'server'],
      width: '160',
      value: (row: Task) => {
        if (!row.node_id) return;
        const node = allNodeDict.value.get(row.node_id);
        if (!node) return;
        return h(NodeType, {
          isMaster: node?.is_master,
          label: node?.name,
          onClick: () => {
            router.push(`/nodes/${node?._id}`);

            sendEvent('click_task_list_actions_view');
          }
        } as NodeTypeProps);
      },
      hasFilter: true,
      allowFilterSearch: true,
      allowFilterItems: true,
      filterItems: allNodeListSelectOptions.value,
    },
    {
      key: 'spider_id',
      label: t('views.tasks.table.columns.spider'),
      icon: ['fa', 'spider'],
      width: '160',
      value: (row: Task) => {
        if (!row.spider_id) return;
        const spider = allSpiderDict.value.get(row.spider_id);
        return h(NavLink, {
          label: spider?.name,
          path: `/spiders/${spider?._id}`,
        });
      },
      hasFilter: true,
      allowFilterSearch: true,
      allowFilterItems: true,
      filterItems: allSpiderListSelectOptions.value,
    },
    {
      key: 'schedule_id',
      label: t('views.tasks.table.columns.schedule'),
      icon: ['fa', 'clock'],
      width: '160',
      value: (row: Task) => {
        if (!row.schedule_id) return;
        const schedule = allScheduleDict.value.get(row.schedule_id);
        return h(NavLink, {
          label: schedule?.name,
          path: `/schedules/${schedule?._id}`,
        });
      },
      hasFilter: true,
      allowFilterSearch: true,
      allowFilterItems: true,
      filterItems: allScheduleListSelectOptions.value,
    },
    {
      key: 'priority',
      label: t('views.tasks.table.columns.priority'),
      icon: ['fa', 'sort-numeric-down'],
      width: '120',
      value: (row: Task) => {
        return h(TaskPriority, {priority: row.priority} as TaskPriorityProps);
      },
      hasSort: true,
      hasFilter: true,
      allowFilterItems: true,
      filterItems: priorityOptions,
    },
    {
      key: 'config',
      label: t('views.tasks.table.columns.cmd'),
      icon: ['fa', 'terminal'],
      width: '160',
      value: (row: Task) => {
        return h(TaskCommand, {
          task: row,
          spider: allSpiderDict.value?.get(row.spider_id as string),
          size: 'small'
        } as TaskConfigProps);
      },
    },
    {
      key: 'status',
      label: t('views.tasks.table.columns.status'),
      icon: ['fa', 'check-square'],
      width: '120',
      value: (row: Task) => {
        return h(TaskStatus, {status: row.status, error: row.error} as TaskStatusProps);
      },
      hasFilter: true,
      allowFilterItems: true,
      filterItems: getStatusOptions(),
    },
    {
      key: 'stat_create_ts',
      label: t('views.tasks.table.columns.stat.create_ts'),
      icon: ['fa', 'clock'],
      width: '120',
      value: (row: Task) => {
        if (!row.stat?.create_ts || row.stat?.create_ts.startsWith('000')) return;
        return h(Time, {time: row.stat?.create_ts as string} as TimeProps);
      },
      defaultHidden: true,
    },
    {
      key: 'stat_start_ts',
      label: t('views.tasks.table.columns.stat.start_ts'),
      icon: ['fa', 'clock'],
      width: '120',
      value: (row: Task) => {
        if (!row.stat?.start_ts || row.stat?.start_ts.startsWith('000')) return;
        return h(Time, {time: row.stat?.start_ts as string} as TimeProps);
      },
    },
    {
      key: 'stat_end_ts',
      label: t('views.tasks.table.columns.stat.end_ts'),
      icon: ['fa', 'clock'],
      width: '120',
      value: (row: Task) => {
        if (!row.stat?.end_ts || row.stat?.end_ts.startsWith('000')) return;
        return h(Time, {time: row.stat?.end_ts as string} as TimeProps);
      },
    },
    {
      key: 'stat_wait_duration',
      label: t('views.tasks.table.columns.stat.wait_duration'),
      icon: ['fa', 'stopwatch'],
      width: '160',
      value: (row: Task) => {
        if (!row.stat?.wait_duration) return;
        return h(Duration, {duration: row.stat?.wait_duration as number} as DurationProps);
      },
      defaultHidden: true,
    },
    {
      key: 'stat_runtime_duration',
      label: t('views.tasks.table.columns.stat.runtime_duration'),
      icon: ['fa', 'stopwatch'],
      width: '160',
      value: (row: Task) => {
        if (!row.stat?.runtime_duration) return;
        return h(Duration, {duration: row.stat?.runtime_duration as number} as DurationProps);
      },
      defaultHidden: true,
    },
    {
      key: 'stat_total_duration',
      label: t('views.tasks.table.columns.stat.total_duration'),
      icon: ['fa', 'stopwatch'],
      width: '160',
      value: (row: Task) => {
        if (!row.stat?.total_duration) return;
        return h(Duration, {duration: row.stat?.total_duration as number} as DurationProps);
      },
    },
    {
      key: 'stat_result_count',
      label: t('views.tasks.table.columns.stat.results'),
      icon: ['fa', 'table'],
      width: '150',
      value: (row: Task) => {
        if (row.stat?.result_count === undefined) return;
        return h(TaskResults, {
          results: row.stat.result_count,
          status: row.status,
          clickable: true,
          onClick: () => {
            router.push(`/tasks/${row._id}/data`);
            sendEvent('click_task_list_actions_view_data');
          },
        } as TaskResultsProps);
      },
    },
    {
      key: TABLE_COLUMN_NAME_ACTIONS,
      label: t('components.table.columns.actions'),
      icon: ['fa', 'tools'],
      width: '240',
      fixed: 'right',
      buttons: (row) => [
        {
          className: 'view-btn',
          type: 'primary',
          size: 'small',
          icon: ['fa', 'search'],
          tooltip: t('common.actions.view'),
          onClick: (row) => {
            router.push(`/tasks/${row._id}`);

            sendEvent('click_task_list_actions_view');
          },
          action: ACTION_VIEW,
        },
        {
          className: 'view-logs-btn',
          type: 'info',
          size: 'small',
          icon: ['fa', 'file-alt'],
          tooltip: t('common.actions.viewLogs'),
          onClick: (row) => {
            router.push(`/tasks/${row._id}/logs`);

            sendEvent('click_task_list_actions_view_logs');
          },
          action: ACTION_VIEW_LOGS,
        },
        {
          className: 'restart-btn',
          type: 'warning',
          size: 'small',
          icon: ['fa', 'redo'],
          tooltip: t('common.actions.restart'),
          onClick: async (row) => {
            sendEvent('click_task_list_actions_restart');

            await ElMessageBox.confirm(
              t('common.messageBox.confirm.restart'),
              t('common.actions.restart'),
              {type: 'warning', confirmButtonClass: 'restart-confirm-btn'},
            );

            sendEvent('click_task_list_actions_restart_confirm');

            await post(`/tasks/${row._id}/restart`);
            await ElMessage.success(t('common.message.success.restart'));
            await store.dispatch(`task/getList`);
          },
          action: ACTION_RESTART,
        },
        {
          className: 'view-data-btn',
          type: 'success',
          size: 'small',
          icon: ['fa', 'database'],
          tooltip: t('common.actions.viewData'),
          onClick: (row) => {
            router.push(`/tasks/${row._id}/data`);

            sendEvent('click_task_list_actions_view_data');
          },
          action: ACTION_VIEW_DATA,
        },
        isCancellable(row.status) ?
          {
            className: 'cancel-btn',
            type: 'info',
            size: 'small',
            icon: ['fa', 'stop'],
            tooltip: t('common.actions.cancel'),
            onClick: async (row: Task) => {
              sendEvent('click_task_list_actions_cancel');

              await ElMessageBox.confirm(
                t('common.messageBox.confirm.cancel'),
                t('common.actions.cancel'),
                {type: 'warning', confirmButtonClass: 'cancel-confirm-btn'},
              );

              sendEvent('click_task_list_actions_cancel_confirm');

              await ElMessage.info(t('common.message.info.cancel'));
              await post(`/tasks/${row._id}/cancel`);
              await store.dispatch(`${ns}/getList`);
            },
            action: ACTION_CANCEL,
          }
          :
          {
            className: 'delete-btn',
            type: 'danger',
            size: 'small',
            icon: ['fa', 'trash-alt'],
            tooltip: t('common.actions.delete'),
            onClick: deleteByIdConfirm,
            action: ACTION_DELETE,
          },
      ],
      disableTransfer: true,
    },
  ]);

  // options
  const opts = {
    navActions,
    tableColumns,
  } as UseListOptions<Task>;

  // init
  setupListComponent(ns, store, ['node', 'project', 'spider', 'schedule']);

  return {
    ...useList<Task>(ns, store, opts),
  };
};

export default useTaskList;
