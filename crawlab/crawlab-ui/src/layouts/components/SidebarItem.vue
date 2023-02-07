<template>
  <!-- no sub menu items -->
  <el-menu-item
    v-if="!item.children"
    v-track="{
      code: 'click_sidebar_menu_item',
      params: {
        path: item.path,
      }
    }"
    :index="item.path"
    @click="onMenuItemClick(item)"
  >
    <cl-menu-item-icon :item="item" size="normal"/>
    <template #title>
      <span class="menu-item-title">{{ t(item.title) }}</span>
    </template>
  </el-menu-item>
  <!-- ./no sub menu items -->

  <!-- has sub menu items -->
  <el-sub-menu
    v-else
    :index="item.path"
  >
    <template #title>
      <cl-menu-item-icon :item="item" size="normal"/>
      <span class="menu-item-title">{{ t(item.title) }}</span>
    </template>
    <SidebarItem
      v-for="(subItem, $index) in item.children"
      :key="$index"
      :index="subItem.path"
      :item="subItem"
      @click="onMenuItemClick(subItem)"
    />
  </el-sub-menu>
  <!-- ./has sub menu items -->
</template>

<script lang="ts">
import {defineComponent, PropType} from 'vue';
import {emptyObjectFunc} from '@/utils/func';
import {useI18n} from 'vue-i18n';

export default defineComponent({
  name: 'SidebarItem',
  props: {
    item: {
      type: Object as PropType<MenuItem>,
      default: emptyObjectFunc,
    }
  },
  emits: [
    'click',
  ],
  setup(props: SidebarItemProps, {emit}) {
    const {t} = useI18n();

    const onMenuItemClick = (item: MenuItem) => {
      emit('click', item);
    };

    return {
      onMenuItemClick,
      t,
    };
  }
});
</script>

<style lang="scss" scoped>
.el-menu-item * {
  vertical-align: middle;
}

.el-menu-item,
.el-sub-menu {
  &.is-active {
    background-color: var(--cl-menu-hover) !important;
  }

  .menu-item-title {
    margin-left: 6px;
  }
}
</style>
