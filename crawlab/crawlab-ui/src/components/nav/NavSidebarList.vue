<template>
  <el-menu
    ref="navMenu"
    class="nav-menu"
    :default-active="activeKey"
    @select="onSelect"
  >
    <el-menu-item
      v-for="item in items"
      :key="item.id"
      :index="item.id"
    >
      <span class="title">{{ item.title }}</span>
    </el-menu-item>
  </el-menu>
</template>

<script lang="ts">
import {defineComponent, PropType} from 'vue';
import {emptyArrayFunc} from '@/utils/func';

export default defineComponent({
  name: 'NavSidebarList',
  props: {
    activeKey: {
      type: String,
    },
    items: {
      type: Array as PropType<NavItem[]>,
      default: emptyArrayFunc,
    }
  },
  emits: [
    'select',
  ],
  setup(props: NavSidebarListProps, {emit}) {
    const onSelect = (id: string) => {
      emit('select', props.items?.findIndex(item => item.id === id));
    };

    return {
      onSelect,
    };
  }
});
</script>

<style lang="scss" scoped>
.nav-menu {
  list-style: none;
  padding: 0;
  margin: 0;
  border: none;
  max-height: calc(100% - var(--cl-nav-sidebar-search-height));
  overflow-y: auto;
  color: var(--cl-nav-sidebar-color);

  &.empty {
    height: var(--cl-nav-sidebar-item-height);
    display: flex;
    align-items: center;
    padding-left: 24px;
    font-size: 14px;
  }

  .nav-menu-item {
    position: relative;
    height: var(--cl-nav-sidebar-item-height);
    line-height: var(--cl-nav-sidebar-item-height);
    padding-left: 24px !important;

    &:hover {
      .actions {
        display: inherit;
      }
    }

    .title {
      font-size: 14px;
      //margin-bottom: 3px;
    }

    .subtitle {
      font-size: 12px;
    }

    .actions {
      display: none;
      position: absolute;
      top: 0;
      right: 10px;

      .icon {
        color: var(--cl-nav-sidebar-item-action-color);
        margin-left: 3px;

        &:hover {
          color: var(--cl-primary-color);
        }
      }
    }
  }
}
</style>
