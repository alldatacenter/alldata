import useList from '@/layouts/content/list/list';
import {useStore} from 'vuex';
import {getDefaultUseListOptions, onListFilterChangeByKey, setupListComponent} from '@/utils/list';
import {computed, h} from 'vue';
import NodeType from '@/components/node/NodeType.vue';
import {
  TABLE_COLUMN_NAME_ACTIONS,
  TABLE_ACTION_EDIT,
  TABLE_ACTION_CUSTOMIZE_COLUMNS,
  TABLE_ACTION_EXPORT
} from '@/constants/table';
import {ElMessageBox} from 'element-plus';
import useNodeService from '@/services/node/nodeService';
import NavLink from '@/components/nav/NavLink.vue';
import {useRouter} from 'vue-router';
import NodeRunners from '@/components/node/NodeRunners.vue';
import Switch from '@/components/switch/Switch.vue';
import NodeStatus from '@/components/node/NodeStatus.vue';
import {
  NODE_STATUS_OFFLINE,
  NODE_STATUS_ONLINE,
  NODE_STATUS_REGISTERED,
  NODE_STATUS_UNREGISTERED
} from '@/constants/node';
import {translate} from '@/utils/i18n';
import {sendEvent} from '@/admin/umeng';
import {
  ACTION_ADD,
  ACTION_DELETE,
  ACTION_ENABLE,
  ACTION_FILTER,
  ACTION_FILTER_SEARCH, ACTION_FILTER_SELECT,
  ACTION_VIEW,
  FILTER_OP_CONTAINS, FILTER_OP_EQUAL
} from '@/constants';
import {isAllowedAction} from '@/utils';

type Node = CNode;

const useNodeList = () => {
  // router
  const router = useRouter();

  // store
  const ns = 'node';
  const store = useStore<RootStoreState>();

  // i18n
  const t = translate;

  // services
  const {
    getList,
    deleteById,
  } = useNodeService(store);

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
          label: t('views.nodes.navActions.new.label'),
          tooltip: t('views.nodes.navActions.new.tooltip'),
          icon: ['fa', 'plus'],
          type: 'success',
          onClick: async () => {
            const message = h('div', [
              h('div', {
                style: {
                  fontSize: '16px',
                  marginBottom: '10px',
                  lineHeight: '1.5',
                },
              }, [t('views.nodes.notice.create.content')]),
              h('a', {
                href: t('views.nodes.notice.create.link.url'),
                style: {
                  color: '#409eff',
                  fontSize: '16px',
                  fontWeight: '600',
                },
                target: '_blank',
              }, [t('views.nodes.notice.create.link.label')]),
            ]);
            const title = t('views.nodes.notice.create.title');
            await ElMessageBox({
              title,
              message,
            });

            // TODO: implement active nodes creation later
            // commit(`${ns}/showDialog`, 'create');

            sendEvent('click_node_list_new');
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
          placeholder: t('views.nodes.navActions.filter.search.placeholder'),
          onChange: onListFilterChangeByKey(store, ns, 'name', FILTER_OP_CONTAINS),
        },
        {
          action: ACTION_FILTER_SELECT,
          id: 'filter-select-type',
          className: 'filter-select-type',
          label: t('views.nodes.navActionsExtra.filter.select.type.label'),
          options: [
            {label: t('components.node.nodeType.label.master'), value: true},
            {label: t('components.node.nodeType.label.worker'), value: false},
          ],
          onChange: onListFilterChangeByKey(store, ns, 'is_master', FILTER_OP_EQUAL),
        },
        {
          action: ACTION_FILTER_SELECT,
          id: 'filter-select-status',
          className: 'filter-select-status',
          label: t('views.nodes.navActionsExtra.filter.select.status.label'),
          options: [
            {label: t('components.node.nodeStatus.label.unregistered'), value: NODE_STATUS_UNREGISTERED},
            {label: t('components.node.nodeStatus.label.registered'), value: NODE_STATUS_REGISTERED},
            {label: t('components.node.nodeStatus.label.online'), value: NODE_STATUS_ONLINE},
            {label: t('components.node.nodeStatus.label.offline'), value: NODE_STATUS_OFFLINE},
          ],
          onChange: onListFilterChangeByKey(store, ns, 'status', FILTER_OP_EQUAL),
        },
        {
          action: ACTION_FILTER_SELECT,
          id: 'filter-select-enabled',
          className: 'filter-select-enabled',
          label: t('views.nodes.navActionsExtra.filter.select.enabled.label'),
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
  const tableColumns = computed<TableColumns<Node>>(() => [
    {
      key: 'name', // name
      className: 'name',
      label: t('views.nodes.table.columns.name'),
      icon: ['fa', 'font'],
      width: '150',
      value: (row: Node) => h(NavLink, {
        path: `/nodes/${row._id}`,
        label: row.name,
      }),
      hasSort: true,
      hasFilter: true,
      allowFilterSearch: true,
    },
    {
      key: 'is_master', // is_master
      className: 'is_master',
      label: t('views.nodes.table.columns.nodeType'),
      icon: ['fa', 'list'],
      width: '150',
      value: (row: Node) => {
        return h(NodeType, {isMaster: row.is_master} as NodeTypeProps);
      },
      hasFilter: true,
      allowFilterItems: true,
      filterItems: [
        {label: t('components.node.nodeType.label.master'), value: true},
        {label: t('components.node.nodeType.label.worker'), value: false},
      ],
    },
    {
      key: 'status', // status
      className: 'status',
      label: t('views.nodes.table.columns.status'),
      icon: ['fa', 'heartbeat'],
      width: '150',
      value: (row: Node) => {
        return h(NodeStatus, {status: row.status} as NodeStatusProps);
      },
      hasFilter: true,
      allowFilterItems: true,
      filterItems: [
        {label: t('components.node.nodeStatus.label.unregistered'), value: NODE_STATUS_UNREGISTERED},
        {label: t('components.node.nodeStatus.label.registered'), value: NODE_STATUS_REGISTERED},
        {label: t('components.node.nodeStatus.label.online'), value: NODE_STATUS_ONLINE},
        {label: t('components.node.nodeStatus.label.offline'), value: NODE_STATUS_OFFLINE},
      ],
    },
    {
      key: 'ip',
      className: 'ip',
      label: t('views.nodes.table.columns.ip'),
      icon: ['fa', 'map-marker-alt'],
      width: '150',
      defaultHidden: true,
    },
    {
      key: 'mac',
      className: 'mac',
      label: t('views.nodes.table.columns.mac'),
      icon: ['fa', 'map-marker-alt'],
      width: '150',
      defaultHidden: true,
    },
    {
      key: 'hostname',
      className: 'hostname',
      label: t('views.nodes.table.columns.hostname'),
      icon: ['fa', 'map-marker-alt'],
      width: '150',
      defaultHidden: true,
    },
    {
      key: 'runners',
      className: 'runners',
      label: t('views.nodes.table.columns.runners'),
      icon: ['fa', 'play'],
      width: '160',
      value: (row: Node) => {
        if (row.max_runners === undefined ||
          !row.status ||
          ![NODE_STATUS_ONLINE, NODE_STATUS_OFFLINE].includes(row.status)
        ) return;
        return h(NodeRunners, {available: row.available_runners, max: row.max_runners} as NodeRunnersProps);
      },
    },
    {
      key: 'enabled', // enabled
      className: 'enabled',
      label: t('views.nodes.table.columns.enabled'),
      icon: ['fa', 'toggle-on'],
      width: '120',
      value: (row: Node) => {
        return h(Switch, {
          modelValue: row.enabled,
          disabled: !isAllowedAction(router.currentRoute.value.path, ACTION_ENABLE),
          'onUpdate:modelValue': async (value: boolean) => {
            row.enabled = value;
            await store.dispatch(`${ns}/updateById`, {id: row._id, form: row});

            value ? sendEvent('click_node_list_enable') : sendEvent('click_node_list_disable');
          },
        } as SwitchProps);
      },
      hasFilter: true,
      allowFilterItems: true,
      filterItems: [
        {label: t('common.control.enabled'), value: true},
        {label: t('common.control.disabled'), value: false},
      ]
    },
    // {
    //   key: 'tags',
    //   className: 'tags',
    //   label: t('views.nodes.table.columns.tags'),
    //   icon: ['fa', 'hashtag'],
    //   value: ({tags}: Node) => {
    //     return h(TagList, {tags});
    //   },
    //   width: '150',
    // },
    {
      key: 'description',
      className: 'description',
      label: t('views.nodes.table.columns.description'),
      icon: ['fa', 'comment-alt'],
      width: 'auto',
      hasFilter: true,
      allowFilterSearch: true,
    },
    {
      key: TABLE_COLUMN_NAME_ACTIONS,
      className: TABLE_COLUMN_NAME_ACTIONS,
      label: t('components.table.columns.actions'),
      fixed: 'right',
      width: '200',
      buttons: [
        {
          type: 'primary',
          icon: ['fa', 'search'],
          tooltip: t('common.actions.view'),
          onClick: (row) => {
            router.push(`/nodes/${row._id}`);

            sendEvent('click_node_list_actions_view');
          },
          action: ACTION_VIEW,
        },
        // {
        //   type: 'info',
        //   size: 'small',
        //   icon: ['fa', 'clone'],
        //   tooltip: 'Clone',
        //   onClick: (row) => {
        //     console.log('clone', row);
        //   }
        // },
        {
          type: 'danger',
          size: 'small',
          icon: ['fa', 'trash-alt'],
          tooltip: t('common.actions.delete'),
          disabled: (row: Node) => !!row.active,
          onClick: async (row: Node) => {
            sendEvent('click_node_list_actions_delete');

            const res = await ElMessageBox.confirm(
              t('common.messageBox.confirm.delete'),
              t('common.actions.delete'),
              {
                type: 'warning',
                confirmButtonClass: 'el-button--danger'
              }
            );

            sendEvent('click_node_list_actions_delete_confirm');

            if (res) {
              await deleteById(row._id as string);
            }
            await getList();
          },
          action: ACTION_DELETE,
        },
      ],
      disableTransfer: true,
    }
  ]);

  // options
  const opts = getDefaultUseListOptions<Node>(navActions, tableColumns);

  // init
  setupListComponent(ns, store, []);

  // visible table actions buttons
  const visibleButtons: BuiltInTableActionButtonName[] = [
    TABLE_ACTION_EDIT,
    TABLE_ACTION_EXPORT,
    TABLE_ACTION_CUSTOMIZE_COLUMNS,
  ];

  return {
    ...useList<Node>(ns, store, opts),
    visibleButtons,
  };
};

export default useNodeList;
