import {Module, GetterTree, MutationTree, ActionTree} from 'vuex';

declare global {
  interface CommonStoreModule extends Module<CommonStoreState, RootStoreState> {
    getters: CommonStoreGetters;
    mutations: CommonStoreMutations;
    actions: CommonStoreActions;
  }

  interface CommonStoreState {
    lang?: Lang;
    systemInfo?: SystemInfo;
  }

  type CommonStoreGetters = GetterTree<CommonStoreState, RootStoreState>;

  interface CommonStoreMutations extends MutationTree<CommonStoreState> {
    setLang: StoreMutation<CommonStoreState, Lang>;
    setSystemInfo: StoreMutation<CommonStoreState, SystemInfo>;
  }

  interface CommonStoreActions extends ActionTree<CommonStoreState, RootStoreState> {
    getSystemInfo: StoreAction<CommonStoreState>;
  }
}
