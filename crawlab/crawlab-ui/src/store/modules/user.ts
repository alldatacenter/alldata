import {
  getDefaultStoreActions,
  getDefaultStoreGetters,
  getDefaultStoreMutations,
  getDefaultStoreState
} from '@/utils/store';
import useRequest from '@/services/request';
import {ROLE_NORMAL} from '@/constants/user';
import {LOCAL_STORAGE_KEY_ME} from '@/constants/localStorage';

const {
  get,
  post,
} = useRequest();

const state = {
  ...getDefaultStoreState<User>('user'),
  newFormFn: () => {
    return {
      role: ROLE_NORMAL,
    };
  },
  me: undefined,
} as UserStoreState;

const getters = {
  ...getDefaultStoreGetters<User>(),
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  me: (state, getters, rootState, rootGetters) => {
    if (state.me) return state.me;
    const meJson = localStorage.getItem(LOCAL_STORAGE_KEY_ME);
    if (!meJson) return;
    try {
      return JSON.parse(meJson) as User;
    } catch (e) {
      return;
    }
  },
} as UserStoreGetters;

const mutations = {
  ...getDefaultStoreMutations<User>(),
  setMe: (state: UserStoreState, user: User) => {
    state.me = user;
    localStorage.setItem(LOCAL_STORAGE_KEY_ME, JSON.stringify(user));
  },
  resetMe: (state: UserStoreState) => {
    state.me = undefined;
    localStorage.removeItem(LOCAL_STORAGE_KEY_ME);
  },
} as UserStoreMutations;

const actions = {
  ...getDefaultStoreActions<User>('/users'),
  changePassword: async (ctx: StoreActionContext, {id, password}: { id: string; password: string }) => {
    return await post(`/users/${id}/change-password`, {password});
  },
  getMe: async ({commit}: StoreActionContext) => {
    const res = await get(`/users/me`);
    commit('setMe', res.data);
  },
  postMe: async (ctx: StoreActionContext, me: User) => {
    await post(`/users/me`, me);
  },
} as UserStoreActions;

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions,
} as UserStoreModule;
