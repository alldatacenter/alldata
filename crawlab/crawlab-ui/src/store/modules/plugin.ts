import {
  getDefaultStoreActions,
  getDefaultStoreGetters,
  getDefaultStoreMutations,
  getDefaultStoreState
} from '@/utils/store';
import useRequest from '@/services/request';
import {SETTING_PLUGIN} from '@/constants/setting';
import {PLUGIN_INSTALL_TYPE_PUBLIC} from '@/constants/plugin';
import {cloneArray, plainClone} from '@/utils/object';

type Plugin = CPlugin;

const {
  get,
  post,
  getList,
} = useRequest();

const state = {
  ...getDefaultStoreState<Plugin>('plugin'),
  newFormFn: () => {
    return {
      install_type: PLUGIN_INSTALL_TYPE_PUBLIC,
      auto_start: true,
    };
  },
  settings: {
    key: 'plugin',
    value: {
      baseUrl: '',
    },
  },
  publicPlugins: [],
  activePublicPlugin: undefined,
  activePublicPluginInfo: undefined,
  installType: PLUGIN_INSTALL_TYPE_PUBLIC,
} as PluginStoreState;

const getters = {
  ...getDefaultStoreGetters<Plugin>(),
} as PluginStoreGetters;

const mutations = {
  ...getDefaultStoreMutations<Plugin>(),
  setSettings: (state: PluginStoreState, settings: Setting) => {
    state.settings = plainClone(settings);
  },
  setSettingsByKey: (state: PluginStoreState, key: string, value: string) => {
    state.settings.value[key] = value;
  },
  setPublicPlugins: (state: PluginStoreState, plugins: PublicPlugin[]) => {
    state.publicPlugins = cloneArray(plugins);
  },
  resetPublicPlugins: (state: PluginStoreState) => {
    state.publicPlugins = [];
  },
  setActivePublicPlugin: (state: PluginStoreState, plugin: PublicPlugin) => {
    state.activePublicPlugin = plainClone(plugin);
  },
  resetActivePublicPlugin: (state: PluginStoreState) => {
    state.activePublicPlugin = undefined;
  },
  setActivePublicPluginInfo: (state: PluginStoreState, pluginInfo: PublicPluginInfo) => {
    state.activePublicPluginInfo = plainClone(pluginInfo);
  },
  resetActivePublicPluginInfo: (state: PluginStoreState) => {
    state.activePublicPluginInfo = undefined;
  },
  setInstallType: (state: PluginStoreState, installType: string) => {
    state.installType = installType;
  },
  resetInstallType: (state: PluginStoreState) => {
    state.installType = PLUGIN_INSTALL_TYPE_PUBLIC;
  },
} as PluginStoreMutations;

const actions = {
  ...getDefaultStoreActions<Plugin>('/plugins'),
  getList: async ({state, commit}: StoreActionContext<PluginStoreState>) => {
    const payload = {
      ...state.tablePagination,
      conditions: JSON.stringify(state.tableListFilter),
      sort: JSON.stringify(state.tableListSort),
      status: true,
    };
    const res = await getList(`/plugins`, payload);
    commit('setTableData', {data: res.data || [], total: res.total});
    return res;
  },
  getAllList: async ({state, commit}: StoreActionContext<PluginStoreState>) => {
    const payload = {
      conditions: JSON.stringify(state.tableListFilter),
      sort: JSON.stringify(state.tableListSort),
      status: true,
      all: true,
    };
    const res = await getList(`/plugins`, payload);
    commit('setAllList', res.data || []);
    return res;
  },
  getSettings: async ({commit}: StoreActionContext<PluginStoreState>) => {
    const res = await get(`/settings/${SETTING_PLUGIN}`);
    if (!res?.data) return;
    commit('setSettings', res.data);
  },
  saveSettings: async ({state}: StoreActionContext<PluginStoreState>) => {
    await post(`/settings/${SETTING_PLUGIN}`, state.settings);
  },
  getPublicPluginList: async ({commit}: StoreActionContext<PluginStoreState>) => {
    const res = await get(`/plugins/public`);
    if (!res?.data) return;
    commit('setPublicPlugins', res.data);
  },
  getPublicPluginInfo: async ({commit}: StoreActionContext<PluginStoreState>, fullName: string) => {
    const res = await get(`/plugins/public/info`, {full_name: fullName});
    if (!res?.data) return;
    commit('setActivePublicPluginInfo', res.data);
  },
} as PluginStoreActions;

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions,
} as PluginStoreModule;
