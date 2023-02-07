import {computed, h} from 'vue';
import {useStore} from 'vuex';
import useList from '@/layouts/content/list/list';
import NavLink from '@/components/nav/NavLink.vue';
import {useRouter} from 'vue-router';
import {translate} from '@/utils/i18n';
import {
  ACTION_ADD, ACTION_DELETE,
  ACTION_FILTER,
  ACTION_FILTER_SEARCH,
  FILTER_OP_CONTAINS
} from '@/constants';
import {onListFilterChangeByKey} from '@/utils';
import Switch from '@/components/switch/Switch.vue';

const useNotificationList = () => {
  // router
  const router = useRouter();

  // store
  const ns = 'notification';
  const store = useStore<RootStoreState>();
  const {commit} = store;

  // i18n
  const t = translate;

  // use list
  const {
    actionFunctions,
  } = useList<NotificationSetting>(ns, store);

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
          label: t('views.projects.navActions.new.label'),
          tooltip: t('views.projects.navActions.new.tooltip'),
          icon: ['fa', 'plus'],
          type: 'success',
          onClick: () => {
            commit(`${ns}/showDialog`, 'create');
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
          placeholder: t('views.projects.navActions.filter.search.placeholder'),
          onChange: onListFilterChangeByKey(store, ns, 'name', FILTER_OP_CONTAINS),
        },
      ]
    },
  ]);

  // table columns
  const tableColumns = computed<TableColumns<NotificationSetting>>(() => [
    {
      key: 'name',
      label: t('views.notification.settings.form.name'),
      icon: ['fa', 'font'],
      width: '240',
      value: (row: NotificationSetting) => h(NavLink, {
        label: row.name,
        path: `/notifications/${row._id}`,
      }),
    },
    {
      key: 'type',
      label: t('views.notification.settings.form.type'),
      icon: ['fa', 'list'],
      width: '120',
      value: (row: NotificationSetting) => t(`views.notification.settings.type.${row.type}`)
    },
    {
      key: 'enabled',
      label: t('views.notification.settings.form.enabled'),
      icon: ['fa', 'toggle-on'],
      width: '120',
      value: (row: NotificationSetting) => h(Switch, {
        modelValue: row.enabled,
        onChange: async (value) => {
          // if (!row._id) return;
          // if (value.enabled) {
          //   await post(`${endpoint}/${row._id}/disable`);
          // } else {
          //   await post(`${endpoint}/${row._id}/enable`);
          // }
        },
      }),
    },
    {
      key: 'description',
      label: t('views.notification.settings.form.description'),
      icon: ['fa', 'comment-alt'],
      width: 'auto',
    },
    {
      key: 'actions',
      label: t('components.table.columns.actions'),
      fixed: 'right',
      width: '200',
      buttons: [
        {
          type: 'primary',
          icon: ['fa', 'search'],
          tooltip: t('common.actions.view'),
          onClick: (row: NotificationSetting) => {
            router.push(`/notifications/${row._id}`);
          },
        },
        {
          type: 'danger',
          size: 'small',
          icon: ['fa', 'trash-alt'],
          tooltip: t('common.actions.delete'),
          onClick: deleteByIdConfirm,
          className: 'delete-btn',
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
  } as UseListOptions<NotificationSetting>;

  return {
    ...useList<NotificationSetting>(ns, store, opts),
  };
};

export default useNotificationList;
