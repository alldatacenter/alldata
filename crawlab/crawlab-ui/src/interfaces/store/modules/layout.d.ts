import {GetterTree, Module, MutationTree} from 'vuex';

declare global {
  interface LayoutStoreModule extends Module<LayoutStoreState, RootStoreState> {
    getters: LayoutStoreGetters;
    mutations: LayoutStoreMutations;
  }

  interface LayoutStoreState {
    // sidebar
    sidebarCollapsed: boolean;
    menuItems: MenuItem[];

    // tabs view
    maxTabId: number;
    tabs: Tab[];
    activeTabId?: number;
    draggingTab?: Tab;
    targetTab?: Tab;
    isTabsDragging: boolean;

    // nav
    navVisibleFn: (path: string) => boolean;

    // detail
    detailTabVisibleFn: (ns: StoreNamespace, item: NavItem) => boolean;

    // action
    actionVisibleFn: (target: string, action: string) => boolean;
  }

  interface LayoutStoreGetters extends GetterTree<LayoutStoreState, RootStoreState> {
    tabs: StoreGetter<LayoutStoreState, Tab[]>;
    activeTab: StoreGetter<LayoutStoreState, Tab | undefined>;
    sidebarMenuItems: StoreGetter<LayoutStoreState, MenuItem[]>;
    normalizedMenuItems: StoreGetter<LayoutStoreState, MenuItem[]>;
  }

  interface LayoutStoreMutations extends MutationTree<LayoutStoreState> {
    setMenuItems: StoreMutation<LayoutStoreState, MenuItem[]>;
    setSideBarCollapsed: StoreMutation<LayoutStoreState, boolean>;
    setTabs: StoreMutation<LayoutStoreState, Tab[]>;
    setActiveTabId: StoreMutation<LayoutStoreState, number>;
    addTab: StoreMutation<LayoutStoreState, Tab>;
    updateTab: StoreMutation<LayoutStoreState, Tab>;
    removeTab: StoreMutation<LayoutStoreState, Tab>;
    removeAllTabs: StoreMutation<LayoutStoreState>;
    setDraggingTab: StoreMutation<LayoutStoreState, Tab>;
    resetDraggingTab: StoreMutation<LayoutStoreState>;
    setTargetTab: StoreMutation<LayoutStoreState, Tab>;
    resetTargetTab: StoreMutation<LayoutStoreState>;
    setIsTabsDragging: StoreMutation<LayoutStoreState, boolean>;
    setNavVisibleFn: StoreMutation<LayoutStoreState, (path: string) => boolean>;
    setDetailTabVisibleFn: StoreMutation<LayoutStoreState, (ns: StoreNamespace, tab: NavItem) => boolean>;
    setActionVisibleFn: StoreMutation<LayoutStoreState, (target: string, action: string) => boolean>;
  }
}
