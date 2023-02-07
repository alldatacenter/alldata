<template>
  <cl-context-menu :placement="placement" :visible="visible" @hide="$emit('hide')">
    <template #default>
      <cl-context-menu-list :items="items" @hide="$emit('hide')"/>
    </template>
    <template #reference>
      <slot></slot>
    </template>
  </cl-context-menu>
</template>

<script lang="ts">
import {computed, defineComponent} from 'vue';
import {contextMenuDefaultProps} from '@/components/context-menu/ContextMenu.vue';

export default defineComponent({
  name: 'FileEditorNavTabsShowMoreContextMenu',
  props: {
    tabs: {
      type: Array,
      default: () => {
        return [];
      },
    },
    ...contextMenuDefaultProps,
  },
  emits: [
    'tab-click',
  ],
  setup(props, {emit}) {
    const items = computed<ContextMenuItem[]>(() => {
      const {tabs} = props as FileEditorNavTabsShowMoreContextMenuProps;
      const contextMenuItems: ContextMenuItem[] = tabs.map(t => {
        return {
          title: t.path || '',
          icon: t.name || '',
          action: () => emit('tab-click', t),
        };
      });
      return contextMenuItems;
    });

    return {
      items,
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
