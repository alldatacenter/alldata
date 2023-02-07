import {
  getDefaultStoreActions,
  getDefaultStoreGetters,
  getDefaultStoreMutations,
  getDefaultStoreState
} from '@/utils/store';

const state = {
  ...getDefaultStoreState<Git>('git'),
} as GitStoreState;

const getters = {
  ...getDefaultStoreGetters<Git>(),
} as GitStoreGetters;

const mutations = {
  ...getDefaultStoreMutations<Git>(),
} as GitStoreMutations;

const actions = {
  ...getDefaultStoreActions<Git>('/gits'),
} as GitStoreActions;

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions,
} as GitStoreModule;
