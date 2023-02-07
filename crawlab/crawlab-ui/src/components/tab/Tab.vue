<template>
  <div
    :class="classes"
    :draggable="true"
    class="tab"
    @click="onClick"
  >
    <span :key="item?.icon || icon" class="icon">
      <cl-menu-item-icon v-if="!icon" :item="item" size="10px"/>
      <cl-icon v-else :icon="icon" size="10px"/>
    </span>
    <span v-if="showTitle" class="title">
      {{ t(title) }}
    </span>
    <span
      v-track="{
        code: 'click_tabs_view_close_tab',
        params: {
          path: () => tab.path,
        }
      }"
      v-if="showClose"
      class="close-btn"
      @click.stop="onClose"
    >
      <el-icon>
        <close/>
      </el-icon>
    </span>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, PropType} from 'vue';
import {useStore} from 'vuex';
import {getPrimaryPath} from '@/utils/path';
// import {useI18n} from 'vue-i18n';
import {useRouter} from 'vue-router';
import Icon from '@/components/icon/Icon.vue';
import {useI18n} from 'vue-i18n';
import {Close} from '@element-plus/icons';

export default defineComponent({
  name: 'Tab',
  components: {
    Close,
  },
  props: {
    tab: {
      type: Object as PropType<Tab>,
    },
    icon: {
      type: [String, Array] as PropType<Icon>
    },
    showTitle: {
      type: Boolean,
      default: true,
    },
    showClose: {
      type: Boolean,
      default: true,
    },
    disabled: {
      type: Boolean,
      default: false,
    }
  },
  emits: [
    'click',
  ],
  setup(props: TabProps, {emit}) {
    const router = useRouter();
    const storeNamespace = 'layout';
    const store = useStore();
    const {layout: state} = store.state as RootStoreState;

    const {t} = useI18n();

    const item = computed<MenuItem | undefined>(() => {
      const {tab} = props as TabProps;
      if (!tab) return;

      // normalized menu items
      const menuItems = store.getters['layout/normalizedMenuItems'] as MenuItem[];

      // iterate normalized menu items
      for (const _item of menuItems) {
        // primary path, or first-level path
        const primaryPath = getPrimaryPath(tab.path);

        // if primary path equals to item path, or tab path equals to item path, return it
        if (primaryPath === _item.path ||
          tab.path === _item.path) {
          return _item;
        }
      }

      return {
        path: tab.path,
        title: '',
      };
    });

    const title = computed(() => {
      // TODO: detailed title
      // return item.value?.title || tm('No Title');
      return item.value?.title;
    });

    const active = computed(() => {
      const {tab} = props as TabProps;
      const {activeTabId} = state;
      return tab?.id === activeTabId;
    });

    const dragging = computed<boolean>(() => {
      const {tab} = props as TabProps;
      return !!tab?.dragging;
    });

    const isTabsDragging = computed<boolean>(() => state.isTabsDragging);

    const classes = computed(() => {
      const cls = [];
      if (active.value) cls.push('active');
      if (dragging.value) cls.push('dragging');
      if (isTabsDragging.value) cls.push('is-tabs-dragging');
      return cls;
    });

    const onClick = () => {
      emit('click');
      const {tab} = props as TabProps;
      if (!tab) return;
      store.commit(`${storeNamespace}/setActiveTabId`, tab.id);
      router.push(tab.path);
    };

    const close = () => {
      // current tab
      const {tab} = props as TabProps;
      if (!tab) return;

      // tabs
      const {tabs} = state;

      // index of current tab (to be removed)
      const idx = tabs.findIndex(d => d.id === tab.id);

      // remove tab
      store.commit(`${storeNamespace}/removeTab`, tab);

      // after-remove actions
      if (active.value) {
        if (tabs.length === 0) {
          const newTab: Tab = {path: '/'};
          store.commit(`${storeNamespace}/addTab`, newTab);
          store.commit(`${storeNamespace}/setActiveTabId`, newTab.id);
          router.push(newTab.path);
        } else if (idx === 0) {
          router.push(tabs[0].path);
          store.commit(`${storeNamespace}/setActiveTabId`, tabs[0].id);
        } else {
          router.push(tabs[idx - 1].path);
          store.commit(`${storeNamespace}/setActiveTabId`, tabs[idx - 1].id);
        }
      }
    };

    const onClose = () => {
      setTimeout(close, 10);
    };

    return {
      item,
      title,
      active,
      dragging,
      isTabsDragging,
      classes,
      onClick,
      onClose,
      t,
    };
  },
});
</script>
<style lang="scss" scoped>
.tab {
  display: flex;
  align-items: center;
  padding: 3px 5px;
  //max-width: $tabsViewTabMaxWidth;
  border: 1px solid var(--cl-tabs-view-tab-border-color);
  cursor: pointer;
  background-color: var(--cl-tabs-view-bg);
  user-select: none;
  color: var(--cl-tabs-view-tab-color);

  &.disabled {
    cursor: not-allowed;
    background-color: var(--cl-disabled-bg-color);
    border-color: var(--cl-disabled-border-color);
    color: var(--cl-disabled-color);
  }

  &:focus:not(.disabled) {
    color: inherit;
  }

  &:hover:not(.disabled) {
    &.dragging {
      .title,
      .icon {
        color: inherit;
      }
    }

    .title,
    .icon {
      color: var(--cl-primary-color);
    }
  }

  &.active {
    color: var(--cl-primary-color);
    border-color: var(--cl-primary-color);
    background-color: var(--cl-primary-plain-color);
  }

  .close-btn,
  .icon {
    font-weight: 100;
    display: flex;
    align-items: center;
    font-size: 12px;
  }

  .title {
    display: flex;
    align-items: center;
    margin: 0 3px;
    font-size: 12px;
    height: var(--cl-tabs-view-tab-height);
    line-height: var(--cl-tabs-view-tab-height);
  }
}
</style>
