import {
  getDefaultStoreActions,
  getDefaultStoreGetters,
  getDefaultStoreMutations,
  getDefaultStoreState
} from '@/utils/store';
import useRequest from '@/services/request';
import {TAB_NAME_OVERVIEW, TAB_NAME_TASKS} from '@/constants/tab';
import {TASK_MODE_RANDOM} from '@/constants/task';
import {translate} from '@/utils/i18n';

// i18n
const t = translate;

const {
  post,
} = useRequest();

const state = {
  ...getDefaultStoreState<Schedule>('schedule'),
  newFormFn: () => {
    return {
      enabled: true,
      mode: TASK_MODE_RANDOM,
    };
  },
  tabs: [
    {id: TAB_NAME_OVERVIEW, title: t('common.tabs.overview')},
    {id: TAB_NAME_TASKS, title: t('common.tabs.tasks')},
  ],
} as ScheduleStoreState;

const getters = {
  ...getDefaultStoreGetters<Schedule>(),
} as ScheduleStoreGetters;

const mutations = {
  ...getDefaultStoreMutations<Schedule>(),
} as ScheduleStoreMutations;

const actions = {
  ...getDefaultStoreActions<Schedule>('/schedules'),
  enable: async (ctx: StoreActionContext, id: string) => {
    return await post(`/schedules/${id}/enable`);
  },
  disable: async (ctx: StoreActionContext, id: string) => {
    return await post(`/schedules/${id}/disable`);
  },
} as ScheduleStoreActions;

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions,
} as ScheduleStoreModule;
