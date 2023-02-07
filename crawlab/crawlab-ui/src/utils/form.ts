import {ref} from 'vue';

export const getDefaultFormComponentData = <T>() => {
  return {
    formRef: ref(),
    formList: ref<T[]>([]),
    formTableFieldRefsMap: ref<FormTableFieldRefsMap>(new Map()),
  } as FormComponentData<T>;
};
