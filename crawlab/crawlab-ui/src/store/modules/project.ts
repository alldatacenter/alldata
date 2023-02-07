import {
  getDefaultStoreActions,
  getDefaultStoreGetters,
  getDefaultStoreMutations,
  getDefaultStoreState
} from '@/utils/store';
import {TAB_NAME_OVERVIEW, TAB_NAME_SPIDERS} from '@/constants/tab';
import {translate} from '@/utils/i18n';

// i18n
const t = translate;

const state = {
  ...getDefaultStoreState<Project>('project'),
  newFormFn: () => {
    return {
      tags: [],
    } as Project;
  },
  tabs: [
    {id: TAB_NAME_OVERVIEW, title: t('common.tabs.overview')},
    {id: TAB_NAME_SPIDERS, title: t('common.tabs.spiders')},
  ],
} as ProjectStoreState;

const getters = {
  ...getDefaultStoreGetters<Project>(),
} as ProjectStoreGetters;

const mutations = {
  ...getDefaultStoreMutations<Project>(),
  setAllProjectSelectOptions: (state: ProjectStoreState, options: SelectOption[]) => {
    state.allProjectSelectOptions = options;
  },
  setAllProjectTags: (state: ProjectStoreState, tags: string[]) => {
    state.allProjectTags = tags;
  },
} as ProjectStoreMutations;

const actions = {
  ...getDefaultStoreActions<Project>('/projects'),
  getAllProjectSelectOptions: async (state: ProjectStoreState) => {
    // TODO: implement
  },
  getAllProjectTags: async (state: ProjectStoreState) => {
    // TODO: implement
  },
} as ProjectStoreActions;

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions,
} as ProjectStoreModule;
