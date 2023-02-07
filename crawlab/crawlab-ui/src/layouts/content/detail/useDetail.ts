import {useRoute, useRouter} from 'vue-router';
import {useStore} from 'vuex';
import {computed, watch, provide, ref} from 'vue';
import {plainClone} from '@/utils/object';
import {getRoutePathByDepth, getTabName} from '@/utils/route';
import {ElMessage} from 'element-plus';
import {sendEvent} from '@/admin/umeng';
import {translate} from '@/utils/i18n';

// i18n
const t = translate;

const useDetail = <T = BaseModel>(ns: ListStoreNamespace) => {
  const router = useRouter();
  const route = useRoute();

  // store state
  const store = useStore();
  const state = store.state[ns] as BaseStoreState;
  const {
    form,
  } = state;

  const navSidebar = ref<NavSidebar | null>(null);

  const navActions = ref<NavActions | null>(null);

  const showActionsToggleTooltip = ref<boolean>(false);

  const navItems = computed<NavItem<T>[]>(() => state.allList.map((d: BaseModel) => {
    return {
      id: d._id,
      title: d.name,
    } as NavItem;
  }));

  const activeId = computed<string>(() => {
    const {id} = route.params;
    return id as string || form._id || '';
  });

  const activeTabName = computed<string>(() => getTabName(router));

  const sidebarCollapsed = computed<boolean>(() => state.sidebarCollapsed);

  const actionsCollapsed = computed<boolean>(() => state.actionsCollapsed);

  const tabs = computed(() => {
    return plainClone(store.getters[`${ns}/tabs`]) as NavItem[];
  });

  const contentContainerStyle = computed(() => {
    return {
      height: `calc(100% - var(--cl-nav-tabs-height) - 1px${navActions.value ? ' - ' + navActions.value.getHeight() : ''})`,
    };
  });

  const primaryRoutePath = computed<string>(() => getRoutePathByDepth(route.path));

  const afterSave = computed<Function[]>(() => state.afterSave);

  const getForm = async () => {
    if (!activeId.value) return;
    return await store.dispatch(`${ns}/getById`, activeId.value);
  };

  const onNavSidebarSelect = async (item: NavItem) => {
    if (!item) {
      console.error(new Error('item is empty'));
      return;
    }
    await router.push(`${primaryRoutePath.value}/${item.id}/${activeTabName.value}`);
    await getForm();

    sendEvent('click_detail_layout_nav_sidebar_select');
  };

  const onNavSidebarToggle = (value: boolean) => {
    if (value) {
      store.commit(`${ns}/collapseSidebar`);
    } else {
      store.commit(`${ns}/expandSidebar`);
    }

    sendEvent('click_detail_layout_nav_sidebar_toggle', {
      collapse: value
    });
  };

  const onActionsToggle = () => {
    showActionsToggleTooltip.value = false;
    if (actionsCollapsed.value) {
      store.commit(`${ns}/expandActions`);
    } else {
      store.commit(`${ns}/collapseActions`);
    }

    sendEvent('click_detail_layout_actions_toggle', {
      collapse: !actionsCollapsed.value,
    });
  };

  const onNavTabsSelect = (tabName: string) => {
    router.push(`${primaryRoutePath.value}/${activeId.value}/${tabName}`);

    sendEvent('click_detail_layout_nav_tabs_select', {
      tabName,
    });
  };

  const onNavTabsToggle = () => {
    if (!sidebarCollapsed.value) {
      store.commit(`${ns}/collapseSidebar`);
    } else {
      store.commit(`${ns}/expandSidebar`);
    }

    sendEvent('click_detail_layout_nav_tabs_toggle', {
      collapse: !sidebarCollapsed.value
    });
  };

  const onBack = () => {
    router.push(`${primaryRoutePath.value}`);

    sendEvent('click_detail_layout_on_back');
  };

  const onSave = async () => {
    if (!activeId.value) {
      console.error('Active id is empty');
      return;
    }
    await store.dispatch(`${ns}/updateById`, {id: activeId.value, form: state.form});
    await ElMessage.success(t('common.message.success.save'));
    await Promise.all([
      store.dispatch(`${ns}/getAllList`),
      store.dispatch(`${ns}/getById`, activeId.value),
    ]);

    // after save
    afterSave.value.map(fn => fn());

    sendEvent('click_detail_layout_on_save');
  };

  // get form when active id changes
  watch(() => activeId.value, getForm);

  // store context
  provide<DetailStoreContext<T>>('store-context', {
    namespace: ns,
    store,
    state,
  });

  return {
    navItems,
    activeId,
    navSidebar,
    navActions,
    showActionsToggleTooltip,
    tabs,
    activeTabName,
    sidebarCollapsed,
    actionsCollapsed,
    contentContainerStyle,
    getForm,
    onNavSidebarSelect,
    onNavSidebarToggle,
    onActionsToggle,
    onNavTabsSelect,
    onNavTabsToggle,
    onBack,
    onSave,
  };
};

export default useDetail;
