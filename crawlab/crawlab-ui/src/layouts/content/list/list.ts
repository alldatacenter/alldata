import {computed, onBeforeUnmount, provide, readonly, watch} from 'vue';
import {Store} from 'vuex';
import {ElMessage, ElMessageBox} from 'element-plus';
import {FILTER_OP_CONTAINS, FILTER_OP_IN, FILTER_OP_NOT_SET} from '@/constants/filter';
import {translate} from '@/utils/i18n';
import {sendEvent} from '@/admin/umeng';

// i18n
const t = translate;

export const getFilterConditions = (column: TableColumn, filter: TableHeaderDialogFilterData) => {
  // allow filter search/items
  const {allowFilterSearch, allowFilterItems} = column;

  // conditions
  const conditions = [] as FilterConditionData[];

  // filter conditions
  if (filter.conditions) {
    filter.conditions
      .filter(d => d.op !== FILTER_OP_NOT_SET)
      .forEach(d => {
        conditions.push(d);
      });
  }

  if (allowFilterItems) {
    // allow filter items (only relevant to items)
    if (filter.items && filter.items.length > 0) {
      conditions.push({
        op: FILTER_OP_IN,
        value: filter.items,
      });
    }
  } else if (allowFilterSearch) {
    // not allow filter items and allow filter search (only relevant to search string)
    if (filter.searchString) {
      conditions.push({
        op: FILTER_OP_CONTAINS,
        value: filter.searchString,
      });
    }
  }

  return conditions;
};

const useList = <T = any>(ns: ListStoreNamespace, store: Store<RootStoreState>, opts?: UseListOptions<T>): ListLayoutComponentData => {
  // store state
  const state = store.state[ns] as BaseStoreState;

  // table
  const tableData = computed<TableData<T>>(() => state.tableData as TableData<T>);
  const tableTotal = computed<number>(() => state.tableTotal);
  const tablePagination = computed<TablePagination>(() => state.tablePagination);
  const tableListFilter = computed<FilterConditionData[]>(() => state.tableListFilter);
  const tableListSort = computed<SortData[]>(() => state.tableListSort);

  // action functions
  const actionFunctions = readonly<ListLayoutActionFunctions>({
    setPagination: (pagination: TablePagination) => store.commit(`${ns}/setTablePagination`, pagination),
    getList: () => store.dispatch(`${ns}/getList`),
    getAll: () => store.dispatch(`${ns}/getAllList`),
    deleteList: (ids: string[]) => store.dispatch(`${ns}/deleteList`, ids),
    deleteByIdConfirm: async (row: BaseModel) => {
      sendEvent('click_list_layout_actions_delete', {ns});
      await ElMessageBox.confirm(t('common.messageBox.confirm.delete'), t('common.actions.delete'), {
        type: 'warning',
        confirmButtonClass: 'el-button--danger delete-confirm-btn'
      });
      sendEvent('click_list_layout_actions_delete_confirm', {ns});
      await store.dispatch(`${ns}/deleteById`, row._id);
      await ElMessage.success(t('common.message.success.delete'));
      await store.dispatch(`${ns}/getList`);
    },
    onHeaderChange: async (column, sort, filter) => {
      const {key} = column;

      // filter
      if (!filter) {
        // no filter
        store.commit(`${ns}/resetTableListFilterByKey`, key);
      } else {
        // has filter
        const conditions = getFilterConditions(column, filter);
        store.commit(`${ns}/setTableListFilterByKey`, {key, conditions});
      }

      // sort
      if (!sort) {
        // no sort
        store.commit(`${ns}/resetTableListSortByKey`, key);
      } else {
        // has sort
        store.commit(`${ns}/setTableListSortByKey`, {key, sort});
      }

      // get list
      await store.dispatch(`${ns}/getList`);
    },
  });

  // active dialog key
  const activeDialogKey = computed<DialogKey | undefined>(() => state.activeDialogKey);

  // get list when pagination changes
  watch(() => tablePagination.value, actionFunctions.getList);

  // get new form
  const getNewForm = state.newFormFn;

  // get new form list
  const getNewFormList = () => {
    const list = [];
    for (let i = 0; i < 5; i++) {
      list.push(getNewForm());
    }
    return list;
  };

  // reset form when active dialog key is changed
  watch(() => state.activeDialogKey, () => {
    if (state.activeDialogKey) {
      // open dialog
      switch (activeDialogKey.value) {
        case 'create':
          store.commit(`${ns}/setForm`, getNewForm());
          store.commit(`${ns}/setFormList`, getNewFormList());
          break;
      }
    } else {
      // close dialog
      store.commit(`${ns}/resetForm`);
      store.commit(`${ns}/resetFormList`);
    }
  });

  // store context
  provide<ListStoreContext<T>>('store-context', {
    namespace: ns,
    store,
    state,
  });

  onBeforeUnmount(() => {
    store.commit(`${ns}/resetTableData`);
    store.commit(`${ns}/resetTablePagination`);
    store.commit(`${ns}/resetTableListFilter`);
  });

  return {
    ...opts,
    tableData,
    tableTotal,
    tablePagination,
    tableListFilter,
    tableListSort,
    actionFunctions,
    activeDialogKey,
  };
};

export default useList;
