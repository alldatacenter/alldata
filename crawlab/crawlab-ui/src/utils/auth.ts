import {LOCAL_STORAGE_KEY_TOKEN} from '@/constants/localStorage';
import {getStore} from '@/store';

export const getToken = () => {
  return localStorage.getItem(LOCAL_STORAGE_KEY_TOKEN);
};

export const setToken = (token: string) => {
  return localStorage.setItem(LOCAL_STORAGE_KEY_TOKEN, token);
};

export const isAllowedAction = (target: string, action: string): boolean => {
  const store = getStore();
  const actionVisibleFn = (store.state as RootStoreState).layout.actionVisibleFn;
  if (!actionVisibleFn) return true;
  return actionVisibleFn(target, action);
};
