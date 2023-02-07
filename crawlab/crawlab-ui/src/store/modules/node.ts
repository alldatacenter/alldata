import {
  getDefaultStoreActions,
  getDefaultStoreGetters,
  getDefaultStoreMutations,
  getDefaultStoreState
} from '@/utils/store';
import {TAB_NAME_OVERVIEW, TAB_NAME_TASKS} from '@/constants/tab';

const state = {
  ...getDefaultStoreState<CNode>('node'),
  newFormFn: () => {
    return {
      tags: [],
      max_runners: 8,
      enabled: true,
    };
  },
  tabs: [
    {id: TAB_NAME_OVERVIEW, title: 'common.tabs.overview'},
    {id: TAB_NAME_TASKS, title: 'common.tabs.tasks'},
  ],
} as NodeStoreState;

const getters = {
  ...getDefaultStoreGetters<CNode>(),
} as NodeStoreGetters;

const mutations = {
  ...getDefaultStoreMutations<CNode>(),
} as NodeStoreMutations;

const actions = {
  ...getDefaultStoreActions<CNode>('/nodes'),
} as NodeStoreActions;

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions,
} as NodeStoreModule;
