import {inject, Ref, ref, SetupContext} from 'vue';
import {ElMessageBox} from 'element-plus';
import {voidAsyncFunc} from '@/utils/func';
import {translate} from '@/utils/i18n';
import {sendEvent} from '@/admin/umeng';

const t = translate;

const useAction = (props: TableProps, ctx: SetupContext, table: Ref, actionFunctions?: ListLayoutActionFunctions) => {
  const {emit} = ctx;

  // store context
  const storeContext = inject<ListStoreContext<BaseModel>>('store-context');
  const ns = storeContext?.namespace;
  const store = storeContext?.store;

  // table selection
  const selection = ref<TableData>([]);
  const onSelectionChange = (value: TableData) => {
    selection.value = value;
    emit('selection-change', value);

    sendEvent('click_table_action_selection_change');
  };

  // action functions
  const getList = actionFunctions?.getList || voidAsyncFunc;
  const deleteList = actionFunctions?.deleteList || voidAsyncFunc;

  const onAdd = () => {
    emit('add');

    sendEvent('click_table_action_on_add');
  };

  const onEdit = async () => {
    emit('edit', selection.value);
    if (storeContext) {
      store?.commit(`${ns}/showDialog`, 'edit');
      store?.commit(`${ns}/setIsSelectiveForm`, true);
      store?.commit(`${ns}/setFormList`, selection.value);
    }

    sendEvent('click_table_action_on_edit');
  };

  const onDelete = async () => {
    const res = await ElMessageBox.confirm(
      t('common.messageBox.confirm.delete'),
      t('components.table.actions.deleteSelected'),
      {
        type: 'warning',
        confirmButtonText: t('common.actions.delete'),
        confirmButtonClass: 'el-button--danger',
      });
    if (!res) return;
    const ids = selection.value.map(d => d._id as string);
    await deleteList(ids);
    table.value?.store?.clearSelection();
    await getList();
    emit('delete', selection.value);

    sendEvent('click_table_action_on_delete');
  };

  const onExport = () => {
    emit('export');

    sendEvent('click_table_action_on_export');
  };

  const clearSelection = () => {
    table.value?.store?.clearSelection();
  };

  return {
    // public variables and methods
    selection,
    onSelectionChange,
    onAdd,
    onEdit,
    onDelete,
    onExport,
    clearSelection,
  };
};

export default useAction;
