import useList from '@/layouts/content/list/list';
import {useStore} from 'vuex';
import {getDefaultUseListOptions, setupListComponent} from '@/utils/list';
import {computed, h, onBeforeMount} from 'vue';
import {TABLE_COLUMN_NAME_ACTIONS} from '@/constants/table';
import {ElMessage, ElMessageBox} from 'element-plus';
import NavLink from '@/components/nav/NavLink.vue';
import {useRouter} from 'vue-router';
import useRequest from '@/services/request';
import {
  PLUGIN_DEPLOY_MODE_ALL,
  PLUGIN_DEPLOY_MODE_MASTER, PLUGIN_STATUS_ERROR,
  PLUGIN_STATUS_INSTALL_ERROR,
  PLUGIN_STATUS_INSTALLING,
  PLUGIN_STATUS_RUNNING,
  PLUGIN_STATUS_STOPPED
} from '@/constants/plugin';
import PluginStatus from '@/components/plugin/PluginStatus.vue';
import PluginStatusMultiNode from '@/components/plugin/PluginStatusMultiNode.vue';
import PluginPid from '@/components/plugin/PluginPid.vue';
import {translate} from '@/utils/i18n';
import {sendEvent} from '@/admin/umeng';
import {ACTION_ADD, ACTION_DELETE, ACTION_START, ACTION_STOP, ACTION_VIEW} from '@/constants';

type Plugin = CPlugin;

// i18n
const t = translate;

const {
  post,
} = useRequest();

const usePluginList = () => {
  // router
  const router = useRouter();

  // store
  const ns = 'plugin';
  const store = useStore<RootStoreState>();
  const {commit} = store;
  const {
    plugin: state,
  } = store.state;

  // use list
  const {
    actionFunctions,
  } = useList<Plugin>(ns, store);

  // action functions
  const {
    deleteByIdConfirm,
  } = actionFunctions;

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
          label: t('views.plugins.navActions.install.label'),
          tooltip: t('views.plugins.navActions.install.tooltip'),
          icon: ['fa', 'download'],
          type: 'success',
          onClick: () => {
            commit(`${ns}/showDialog`, 'install');

            sendEvent('click_plugin_list_install');
          }
        }
      ]
    },
    {
      name: 'settings',
      children: [
        {
          id: 'settings-btn',
          className: 'settings-btn',
          buttonType: 'label',
          label: t('views.plugins.navActions.settings.label'),
          tooltip: t('views.plugins.navActions.settings.tooltip'),
          icon: ['fa', 'cog'],
          type: 'primary',
          onClick: () => {
            commit(`${ns}/showDialog`, 'settings');

            sendEvent('click_plugin_list_settings');
          }
        }
      ]
    }
  ]);

  // table columns
  const tableColumns = computed<TableColumns<Plugin>>(() => [
    {
      key: 'name', // name
      label: t('views.plugins.table.columns.name'),
      icon: ['fa', 'font'],
      width: '250',
      value: (row: Plugin) => h(NavLink, {
        path: `/plugins/${row._id}`,
        label: row.name || row.full_name || row._id,
      }),
      hasSort: true,
      hasFilter: true,
      allowFilterSearch: true,
    },
    {
      key: 'status',
      label: t('views.plugins.table.columns.status'),
      icon: ['fa', 'check-square'],
      width: '120',
      value: (row: Plugin) => {
        if (row.deploy_mode === PLUGIN_DEPLOY_MODE_MASTER || row.status?.length === 1) {
          const status = row.status?.[0];
          return h(PluginStatus, {...status} as PluginStatusProps);
        } else if (row.deploy_mode === PLUGIN_DEPLOY_MODE_ALL) {
          return h(PluginStatusMultiNode, {status: row.status} as PluginStatusMultiNodeProps);
        }
      },
    },
    {
      key: 'pid',
      label: t('views.plugins.table.columns.processId'),
      icon: ['fa', 'microchip'],
      width: '120',
      value: (row: Plugin) => {
        return h(PluginPid, {status: row.status} as PluginPidProps);
      },
    },
    {
      key: 'description',
      label: t('views.plugins.table.columns.description'),
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
      buttons: (row: Plugin) => {
        let buttons: TableColumnButton[];

        buttons = [
          {
            className: 'start-btn',
            type: 'success',
            icon: ['fa', 'play'],
            tooltip: t('common.actions.start'),
            onClick: async (row) => {
              sendEvent('click_plugin_list_actions_start');

              await ElMessageBox.confirm(
                t('common.messageBox.confirm.start'),
                t('common.actions.start'),
                {
                  type: 'warning',
                  confirmButtonClass: 'start-plugin-confirm-btn',
                  cancelButtonClass: 'start-plugin-cancel-btn',
                },
              );

              sendEvent('click_plugin_list_actions_start_confirm');

              await post(`/plugins/${row._id}/start`);
              await ElMessage.success(t('common.message.success.start'));
              await store.dispatch(`${ns}/getList`);
            },
            disabled: (row: Plugin) => {
              if (row.status?.length === 1) {
                return [
                  PLUGIN_STATUS_INSTALLING,
                  PLUGIN_STATUS_RUNNING,
                ].includes(row.status[0].status);
              } else if (row.status) {
                for (const s of row.status) {
                  if ([
                    PLUGIN_STATUS_INSTALL_ERROR,
                    PLUGIN_STATUS_STOPPED,
                    PLUGIN_STATUS_ERROR,
                  ].includes(s.status)) {
                    return false;
                  }
                }
                return true;
              } else {
                return true;
              }
            },
            action: ACTION_START,
          },
          {
            className: 'stop-btn',
            type: 'info',
            size: 'small',
            icon: ['fa', 'stop'],
            tooltip: t('common.actions.stop'),
            onClick: async (row) => {
              sendEvent('click_plugin_list_actions_stop');

              await ElMessageBox.confirm(
                t('common.messageBox.confirm.stop'),
                t('common.actions.stop'),
                {
                  type: 'warning',
                  confirmButtonClass: 'stop-plugin-confirm-btn',
                  cancelButtonClass: 'stop-plugin-cancel-btn',
                },
              );

              sendEvent('click_plugin_list_actions_stop_confirm');

              await ElMessage.info(t('common.message.info.stop'));
              await post(`/plugins/${row._id}/stop`);
              await store.dispatch(`${ns}/getList`);
            }, disabled: (row: Plugin) => {
              if (row.status?.length === 1) {
                return [
                  PLUGIN_STATUS_INSTALL_ERROR,
                  PLUGIN_STATUS_STOPPED,
                  PLUGIN_STATUS_ERROR,
                ].includes(row.status[0].status);
              } else if (row.status) {
                for (const s of row.status) {
                  if ([
                    PLUGIN_STATUS_INSTALLING,
                    PLUGIN_STATUS_RUNNING,
                  ].includes(s.status)) {
                    return false;
                  }
                }
                return true;
              } else {
                return true;
              }
            },
            action: ACTION_STOP,
          },
        ];

        // default
        buttons = buttons.concat([
          {
            className: 'view-btn',
            type: 'primary',
            icon: ['fa', 'search'],
            tooltip: t('common.actions.view'),
            onClick: (row) => {
              router.push(`/plugins/${row._id}`);

              sendEvent('click_plugin_list_actions_view');
            },
            action: ACTION_VIEW,
          },
          {
            className: 'delete-btn',
            type: 'danger',
            size: 'small',
            icon: ['fa', 'trash-alt'],
            tooltip: t('common.actions.delete'),
            disabled: (row: Plugin) => !!row.active,
            onClick: deleteByIdConfirm,
            action: ACTION_DELETE,
          },
        ]);
        return buttons;
      },
      disableTransfer: true,
    }
  ]);

  // options
  const opts = getDefaultUseListOptions<Plugin>(navActions, tableColumns);

  // init
  setupListComponent(ns, store, []);

  onBeforeMount(async () => {
    // get base url
    await store.dispatch(`${ns}/getSettings`);
  });

  const saveBaseUrl = async (value: string) => {
    await store.dispatch(`${ns}/saveBaseUrl`, value);
  };

  const onBaseUrlChange = async (value: string) => {
    await saveBaseUrl(value);
  };

  return {
    ...useList<Plugin>(ns, store, opts),
    saveBaseUrl,
    onBaseUrlChange,
  };
};

export default usePluginList;
