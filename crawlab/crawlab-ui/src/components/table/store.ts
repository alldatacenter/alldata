import {computed, Ref, SetupContext} from 'vue';
import {Table} from 'element-plus/lib/components/table/src/table/defaults';

const useStore = (props: TableProps, ctx: SetupContext, table: Ref<Table<any> | undefined>) => {
  const setColumns = (states: TableStoreStates, columns: TableColumnCtx[]) => {
    states._columns.value = columns;
  };

  const store = computed<TableStore | undefined>(() => {
    const store = (table.value?.store as unknown) as TableStore;
    if (!store) return;
    store.mutations.setColumns = setColumns;
    return store;
  });

  return {
    // public variables and methods
    store,
  };
};

export default useStore;
