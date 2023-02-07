import {computed, h} from 'vue';
import {TABLE_COLUMN_NAME_ACTIONS} from '@/constants/table';
import {useStore} from 'vuex';
import useList from '@/layouts/content/list/list';
import NavLink from '@/components/nav/NavLink.vue';
import {useRouter} from 'vue-router';
import {TAB_NAME_SPIDERS} from '@/constants/tab';
import {translate} from '@/utils/i18n';
import {sendEvent} from '@/admin/umeng';
import {
  ACTION_ADD,
  ACTION_DELETE,
  ACTION_FILTER,
  ACTION_FILTER_SEARCH,
  ACTION_VIEW,
  FILTER_OP_CONTAINS
} from '@/constants';
import {onListFilterChangeByKey} from '@/utils';

const useProjectList = () => {
  // router
  const router = useRouter();

  // store
  const ns = 'project';
  const store = useStore<RootStoreState>();
  const {commit} = store;

  // i18n
  const t = translate;

  // use list
  const {
    actionFunctions,
  } = useList<Project>(ns, store);

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
  const tableColumns = computed<TableColumns<Project>>(() => [
    {
      className: 'name',
      key: 'name',
      label: t('views.projects.table.columns.name'),
      icon: ['fa', 'font'],
      width: '150',
      value: (row: Project) => h(NavLink, {
        path: `/projects/${row._id}`,
        label: row.name,
      }),
      hasSort: true,
      hasFilter: true,
      allowFilterSearch: true,
    },
    {
      className: 'spiders',
      key: 'spiders',
      label: t('views.projects.table.columns.spiders'),
      icon: ['fa', 'spider'],
      value: (row: Project) => h(NavLink, {
        path: `/projects/${row._id}/${TAB_NAME_SPIDERS}`,
        label: row.spiders,
      }),
      width: '120',
    },
    // {
    //   key: 'tags',
    //   label: t('views.projects.table.columns.tags'),
    //   icon: ['fa', 'hashtag'],
    //   value: ({tags}: Project) => {
    //     return h(TagList, {tags});
    //   },
    //   width: '200',
    // },
    {
      key: 'description',
      label: t('views.projects.table.columns.description'),
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
            router.push(`/projects/${row._id}`);

            sendEvent('click_project_list_actions_view');
          },
          action: ACTION_VIEW,
        },
        // {
        //   type: 'warning',
        //   icon: ['fa', 'edit'],
        //   tooltip: t('common.actions.edit'),
        //   onClick: (row) => {
        //     store.commit(`${ns}/setForm`, row);
        //     store.commit(`${ns}/showDialog`, 'edit');
        //   },
        // },
        // {
        //   type: 'info',
        //   size: 'small',
        //   icon: ['fa', 'clone'],
        //   tooltip: t('common.actions.clone'),
        //   onClick: (row) => {
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
  } as UseListOptions<Project>;

  return {
    ...useList<Project>(ns, store, opts),
  };
};

export default useProjectList;
