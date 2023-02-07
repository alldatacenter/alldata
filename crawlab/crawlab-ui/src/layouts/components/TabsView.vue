<template>
  <div class="tabs-view">
    <cl-draggable-list
      class="tab-list"
      :items="tabs"
      item-key="id"
      @d-end="onDragDrop"
    >
      <template v-slot="{item}">
        <cl-tab
          v-track="{
              code: 'click_tabs_view_click_tab',
              params: {path: item.path}
            }"
          :tab="item"
        />
      </template>
    </cl-draggable-list>

    <!-- Add cl-tab -->
    <cl-action-tab
      :icon="['fa', 'plus']"
      class="add-tab"
      @click="onAddTab"
    />
    <!-- ./Add cl-tab -->
  </div>
</template>
<script lang="ts">
import {computed, defineComponent, onMounted, watch} from 'vue';
import {useStore} from 'vuex';
import {useRoute, useRouter} from 'vue-router';
import {plainClone} from '@/utils/object';

export default defineComponent({
  name: 'TabsView',
  setup() {
    // store
    const storeNameSpace = 'layout';
    const store = useStore<RootStoreState>();

    // route
    const route = useRoute();

    // router
    const router = useRouter();

    // current path
    const currentPath = computed(() => route.path);

    // tabs
    const tabs = computed < Tab[] > (() => store.getters[`${storeNameSpace}/tabs`]);

    const addTab = (tab: Tab) =>
    {
      store.commit(`${storeNameSpace}/addTab`, tab);
    }
    ;

    const setActiveTab = (tab: Tab) =>
    {
      store.commit(`${storeNameSpace}/setActiveTabId`, tab.id);
    }
    ;

    const onAddTab = () => {
      addTab({path: '/'});
      const newTab = tabs.value[tabs.value.length - 1];
      setActiveTab(newTab);
      router.push(newTab.path);
    };

    const onDragDrop = (tabs: Tab[]) =>
    {
      store.commit(`${storeNameSpace}/setTabs`, tabs);
    }
    ;

    const updateTabs = (path: string) => {
      // active tab
      const activeTab = store.getters[`${storeNameSpace}/activeTab`] as Tab | undefined;

      // skip if active tab is undefined
      if (!activeTab) return;

      // clone
      const activeTabClone = plainClone(activeTab);

      // set path to active tab
      activeTabClone.path = path;

      // update path of active tab
      store.commit(`${storeNameSpace}/updateTab`, activeTabClone);
    };

    watch(currentPath, updateTabs);

    onMounted(() => {
      // add current page to tabs if no tab exists
      if (tabs.value.length === 0) {
        // add tab
        addTab({path: currentPath.value});

        // new tab
        const newTab = tabs.value[0];
        if (!newTab) return;

        // set active tab id
        setActiveTab(newTab);
      }

      // update tabs
      updateTabs(currentPath.value);
    });

    return {
      tabs,
      onAddTab,
      currentPath,
      onDragDrop,
    };
  }
});
</script>
<style lang="scss" scoped>
.tabs-view {
  padding: 10px 0;
  border-bottom: 1px solid var(--cl-tabs-view-border-color);
  background-color: var(--cl-tabs-view-bg);
  display: flex;
}
</style>
<style scoped>
.tabs-view >>> .draggable-item {
  margin: 0 5px;
}

.tabs-view >>> .draggable-item:first-child {
  margin-left: 10px;
}
</style>
