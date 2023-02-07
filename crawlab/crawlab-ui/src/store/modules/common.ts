import {plainClone} from '@/utils/object';
import useRequest from '@/services/request';

const {
  get,
} = useRequest();

export default {
  namespaced: true,
  state: {
    lang: localStorage.getItem('lang') || 'en',
    systemInfo: undefined,
  },
  mutations: {
    setLang: (state: CommonStoreState, lang: Lang) => {
      state.lang = lang;
    },
    setSystemInfo: (state: CommonStoreState, info: SystemInfo) => {
      state.systemInfo = plainClone(info);
    },
  },
  actions: {
    getSystemInfo: async ({commit}: StoreActionContext) => {
      const res = await get('/system-info');
      commit('setSystemInfo', res.data);
    }
  },
} as CommonStoreModule;
