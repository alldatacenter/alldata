import {computed, provide} from 'vue';
import {Store} from 'vuex';
import useFormTable from '@/components/form/formTable';
import {EMPTY_OBJECT_ID} from '@/utils/mongo';
import {translate} from '@/utils/i18n';
import {sendEvent} from '@/admin/umeng';

// i18n
const t = translate;

export const useForm = (ns: ListStoreNamespace, store: Store<RootStoreState>, services: Services<BaseModel>, data: FormComponentData<BaseModel>) => {
  const {
    formRef,
    formTableFieldRefsMap,
  } = data;

  // state
  const state = store.state[ns];

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

  // form
  const form = computed<BaseModel>(() => state.form);

  // form list
  const formList = computed<BaseModel[]>(() => state.formList);

  // active dialog key
  const activeDialogKey = computed<DialogKey | undefined>(() => state.activeDialogKey);

  // is selective form
  const isSelectiveForm = computed<boolean>(() => state.isSelectiveForm);

  // selected form fields
  const selectedFormFields = computed<string[]>(() => state.selectedFormFields);

  // readonly form fields
  const readonlyFormFields = computed<string[]>(() => state.readonlyFormFields);

  // is batch form getters
  const isBatchForm = computed<boolean>(() => store.getters[`${ns}/isBatchForm`]);

  // form list ids getters
  const formListIds = computed<string[]>(() => store.getters[`${ns}/formListIds`]);

  const validateForm = async () => {
    if (isBatchForm.value && activeDialogKey.value === 'create') {
      let valid = true;
      for (const formRef of formTableFieldRefsMap.value.values()) {
        try {
          await formRef.value?.validate?.();
        } catch (e) {
          valid = false;
        }
      }
      return valid;
    } else {
      return await formRef.value?.validate();
    }
  };

  const resetForm = () => {
    if (activeDialogKey.value) {
      switch (activeDialogKey.value) {
        case 'create':
          store.commit(`${ns}/setForm`, getNewForm());
          store.commit(`${ns}/setFormList`, getNewFormList());
          break;
        case 'edit':
          formRef.value?.clearValidate();
          break;
      }
    } else {
      formRef.value?.resetFields();
      formTableFieldRefsMap.value = new Map();
    }
  };

  // whether form item is disabled
  const isFormItemDisabled = (prop: string) => {
    if (readonlyFormFields.value.includes(prop)) {
      return true;
    }
    if (!isSelectiveForm.value) return false;
    if (!prop) return false;
    return !selectedFormFields.value.includes(prop);
  };

  // whether the form is empty
  const isEmptyForm = (d: any): boolean => {
    return JSON.stringify(d) === JSON.stringify(getNewForm());
  };
  provide<(d: any) => boolean>('fn:isEmptyForm', isEmptyForm);

  // all list select options
  const allListSelectOptions = computed<SelectOption[]>(() => store.getters[`${ns}/allListSelectOptions`]);

  // all list select options with empty
  const allListSelectOptionsWithEmpty = computed<SelectOption[]>(() => allListSelectOptions.value.concat({
    label: t('common.status.unassigned'),
    value: EMPTY_OBJECT_ID,
  }));

  // all dict
  const allDict = computed<Map<string, BaseModel>>(() => store.getters[`${ns}/allDict`]);

  // all tags
  const allTags = computed<string[]>(() => store.getters[`${ns}/allTags`]);

  // services
  const {
    getList,
    create,
    updateById,
    createList,
    updateList,
  } = services;

  // dialog create edit
  const createEditDialogVisible = computed<boolean>(() => {
    const {activeDialogKey} = state;
    if (!activeDialogKey) return false;
    return ['create', 'edit'].includes(activeDialogKey);
  });

  // dialog create edit tab name
  const createEditDialogTabName = computed<CreateEditTabName>(() => state.createEditDialogTabName);

  // dialog confirm
  const confirmDisabled = computed<boolean>(() => {
    return isSelectiveForm.value &&
      selectedFormFields.value.length === 0;
  });
  const confirmLoading = computed<boolean>(() => state.confirmLoading);
  const setConfirmLoading = (value: boolean) => store.commit(`${ns}/setConfirmLoading`, value);
  const onConfirm = async () => {
    // validate
    try {
      const valid = await validateForm();
      if (!valid) return;
    } catch (ex) {
      console.error(ex);
      return;
    }
    if (!form.value) {
      console.error(new Error('form is undefined'));
      return;
    }

    // flag of request finished
    let isRequestFinished = false;

    // start loading
    setTimeout(() => {
      if (isRequestFinished) return;
      setConfirmLoading(true);
    }, 50);

    // request
    try {
      let res: HttpResponse;
      switch (activeDialogKey.value) {
        case 'create':
          if (isBatchForm.value) {
            const changedFormList = formList.value.filter(d => !isEmptyForm(d));
            res = await createList(changedFormList);
          } else {
            res = await create(form.value);
          }
          sendEvent('click_form_create_confirm', {
            ns,
            batch: isBatchForm.value,
          });
          break;
        case 'edit':
          if (isBatchForm.value) {
            res = await updateList(formListIds.value, form.value, selectedFormFields.value);
          } else {
            res = await updateById(form.value._id as string, form.value);
          }
          sendEvent('click_form_edit_confirm', {
            ns,
            batch: isBatchForm.value,
          });
          break;
        default:
          console.error(`activeDialogKey "${activeDialogKey.value}" is invalid`);
          return;
      }
      if (res.error) {
        console.error(res.error);
        return;
      }
    } finally {
      // flag request finished as true
      isRequestFinished = true;

      // stop loading
      setConfirmLoading(false);
    }

    // close
    store.commit(`${ns}/hideDialog`);

    // request list
    await getList();
  };

  // dialog close
  const onClose = () => {
    store.commit(`${ns}/hideDialog`);
  };

  // dialog tab change
  const onTabChange = (tabName: CreateEditTabName) => {
    // if (tabName === 'batch') {
    //   store.commit(`${ns}/setFormList`, getNewFormList());
    // }
    store.commit(`${ns}/setCreateEditDialogTabName`, tabName);
  };

  // use form table
  const formTable = useFormTable(ns, store, services, data);
  const {
    onAdd,
    onClone,
    onDelete,
    onFieldChange,
    onFieldRegister,
  } = formTable;

  // action functions
  const actionFunctions = {
    onClose,
    onConfirm,
    onTabChange,
    onAdd,
    onClone,
    onDelete,
    onFieldChange,
    onFieldRegister,
  } as CreateEditDialogActionFunctions;

  return {
    ...formTable,
    getNewForm,
    getNewFormList,
    form,
    formRef,
    isSelectiveForm,
    selectedFormFields,
    formList,
    isBatchForm,
    validateForm,
    resetForm,
    isFormItemDisabled,
    activeDialogKey,
    createEditDialogTabName,
    createEditDialogVisible,
    allListSelectOptions,
    allListSelectOptionsWithEmpty,
    allDict,
    allTags,
    confirmDisabled,
    confirmLoading,
    setConfirmLoading,
    actionFunctions,
  };
};

export default useForm;
