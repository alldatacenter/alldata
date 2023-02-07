import {computed, h} from 'vue';
import {TABLE_COLUMN_NAME_ACTIONS} from '@/constants/table';
import {useStore} from 'vuex';
import {ElMessage} from 'element-plus';
import useList from '@/layouts/content/list/list';
import NavLink from '@/components/nav/NavLink.vue';
import {useRouter} from 'vue-router';
import {onListFilterChangeByKey, setupListComponent} from '@/utils/list';
import TaskMode from '@/components/task/TaskMode.vue';
import ScheduleCron from '@/components/schedule/ScheduleCron.vue';
import Switch from '@/components/switch/Switch.vue';
import useSpider from '@/components/spider/spider';
import useTask from '@/components/task/task';
import {translate} from '@/utils/i18n';
import {sendEvent} from '@/admin/umeng';
import {
  ACTION_ADD,
  ACTION_DELETE,
  ACTION_ENABLE,
  ACTION_FILTER,
  ACTION_FILTER_SEARCH,
  ACTION_FILTER_SELECT,
  ACTION_VIEW,
  FILTER_OP_CONTAINS,
  FILTER_OP_EQUAL,
  TASK_MODE_ALL_NODES,
  TASK_MODE_RANDOM,
  TASK_MODE_SELECTED_NODE_TAGS,
  TASK_MODE_SELECTED_NODES
} from '@/constants';
import {isAllowedAction} from '@/utils';

// i18n
const t = translate;

const useScheduleList = () => {
  // router
  const router = useRouter();

  // store
  const ns = 'schedule';
  const store = useStore<RootStoreState>();
  const {commit} = store;

  // use list
  const {
    actionFunctions,
  } = useList<Schedule>(ns, store);

  // action functions
  const {
    deleteByIdConfirm,
  } = actionFunctions;

  // all spider dict
  const allSpiderDict = computed<Map<string, Spider>>(() => store.getters['spider/allDict']);

  const {
    allListSelectOptions: allSpiderListSelectOptions,
  } = useSpider(store);

  const {
    modeOptions,
  } = useTask(store);

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
          label: t('views.schedules.navActions.new.label'),
          tooltip: t('views.schedules.navActions.new.tooltip'),
          icon: ['fa', 'plus'],
          type: 'success',
          onClick: () => {
            commit(`${ns}/showDialog`, 'create');

            sendEvent('click_schedule_list_new');
          }
        }
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
          placeholder: t('views.schedules.navActions.filter.search.placeholder'),
          onChange: onListFilterChangeByKey(store, ns, 'name', FILTER_OP_CONTAINS),
        },
        {
          action: ACTION_FILTER_SELECT,
          id: 'filter-select-spider',
          className: 'filter-select-spider',
          label: t('views.schedules.navActionsExtra.filter.select.spider.label'),
          optionsRemote: {
            colName: 'spiders',
          },
          onChange: onListFilterChangeByKey(store, ns, 'spider_id', FILTER_OP_EQUAL),
        },
        {
          action: ACTION_FILTER_SELECT,
          id: 'filter-select-mode',
          className: 'filter-select-mode',
          label: t('views.schedules.navActionsExtra.filter.select.mode.label'),
          options: [
            {label: t('components.task.mode.label.randomNode'), value: TASK_MODE_RANDOM},
            {label: t('components.task.mode.label.allNodes'), value: TASK_MODE_ALL_NODES},
            {label: t('components.task.mode.label.selectedNodes'), value: TASK_MODE_SELECTED_NODES},
            {label: t('components.task.mode.label.selectedTags'), value: TASK_MODE_SELECTED_NODE_TAGS},
          ],
          onChange: onListFilterChangeByKey(store, ns, 'mode', FILTER_OP_EQUAL),
        },
        {
          action: ACTION_FILTER_SEARCH,
          id: 'filter-search-cron',
          className: 'search-cron',
          placeholder: t('views.schedules.navActionsExtra.filter.search.cron.placeholder'),
          onChange: onListFilterChangeByKey(store, ns, 'cron', FILTER_OP_CONTAINS),
        },
        {
          action: ACTION_FILTER_SELECT,
          id: 'filter-select-enabled',
          className: 'filter-select-enabled',
          label: t('views.schedules.navActionsExtra.filter.select.enabled.label'),
          options: [
            {label: t('common.control.enabled'), value: true},
            {label: t('common.control.disabled'), value: false},
          ],
          onChange: onListFilterChangeByKey(store, ns, 'enabled', FILTER_OP_EQUAL),
        },
      ]
    }
  ]);

  // table columns
  const tableColumns = computed<TableColumns<Schedule>>(() => [
    {
      key: 'name',
      label: t('views.schedules.table.columns.name'),
      icon: ['fa', 'font'],
      width: '150',
      value: (row: Schedule) => h(NavLink, {
        path: `/schedules/${row._id}`,
        label: row.name,
      }),
      hasSort: true,
      hasFilter: true,
      allowFilterSearch: true,
    },
    {
      key: 'spider_id',
      label: t('views.schedules.table.columns.spider'),
      icon: ['fa', 'spider'],
      width: '160',
      value: (row: Schedule) => {
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
      key: 'mode',
      label: t('views.schedules.table.columns.mode'),
      icon: ['fa', 'cog'],
      width: '160',
      value: (row: Schedule) => {
        return h(TaskMode, {mode: row.mode} as TaskModeProps);
      },
      hasFilter: true,
      allowFilterItems: true,
      filterItems: modeOptions,
    },
    {
      key: 'cron',
      label: t('views.schedules.table.columns.cron'),
      icon: ['fa', 'clock'],
      width: '160',
      value: (row: Schedule) => {
        return h(ScheduleCron, {cron: row.cron} as ScheduleCronProps);
      },
      hasFilter: true,
      allowFilterSearch: true,
    },
    {
      key: 'enabled',
      label: t('views.schedules.table.columns.enabled'),
      icon: ['fa', 'toggle-on'],
      width: '120',
      value: (row: Schedule) => {
        return h(Switch, {
          modelValue: row.enabled,
          disabled: !isAllowedAction(router.currentRoute.value.path, ACTION_ENABLE),
          'onUpdate:modelValue': async (value: boolean) => {
            if (value) {
              await store.dispatch(`${ns}/enable`, row._id);
              ElMessage.success(t('components.schedule.message.success.enable'));
            } else {
              await store.dispatch(`${ns}/disable`, row._id);
              ElMessage.success(t('components.schedule.message.success.disable'));
            }

            value ? sendEvent('click_schedule_list_enable') : sendEvent('click_schedule_list_disable');

            await store.dispatch(`${ns}/getList`);
          },
        } as SwitchProps);
      },
      hasFilter: true,
      allowFilterItems: true,
      filterItems: [
        {label: t('common.control.enabled'), value: true},
        {label: t('common.control.disabled'), value: false},
      ],
    },
    {
      key: 'entry_id',
      label: t('views.schedules.table.columns.entryId'),
      icon: ['fa', 'hash'],
      width: '120',
      defaultHidden: true,
    },
    {
      key: 'description',
      label: t('views.schedules.table.columns.description'),
      icon: ['fa', 'comment-alt'],
      width: 'auto',
      hasFilter: true,
      allowFilterSearch: true,
    },
    {
      key: TABLE_COLUMN_NAME_ACTIONS,
      label: t('components.table.columns.actions'),
      fixed: 'right',
      width: '200',
      buttons: [
        {
          className: 'view-btn',
          type: 'primary',
          icon: ['fa', 'search'],
          tooltip: t('common.actions.view'),
          onClick: (row) => {
            router.push(`/schedules/${row._id}`);

            sendEvent('click_schedule_list_actions_view');
          },
          action: ACTION_VIEW,
        },
        // {
        //   type: 'info',
        //   size: 'small',
        //   icon: ['fa', 'clone'],
        //   tooltip: t('common.actions.clone'),
        //   onClick: (row) => {
        //     // TODO: implement
        //     console.log('clone', row);
        //   }
        // },
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
    }
  ]);

  // options
  const opts = {
    navActions,
    tableColumns,
  } as UseListOptions<Schedule>;

  // init
  setupListComponent(ns, store, ['node', 'spider']);

  return {
    ...useList<Schedule>(ns, store, opts),
  };
};

export default useScheduleList;
