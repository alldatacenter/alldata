<template>
  <DemoNavSidebarLayout>
    <template #sidebar>
      <NavSidebar
        type="list"
        :active-key="activeKey"
        :items="items"
        @select="onSelect"
      />
    </template>
    <template #content>
      <div>
        Active Item:
      </div>
      <Tag :label="activeItem?.title"/>
    </template>
  </DemoNavSidebarLayout>
</template>

<script lang="ts">
import {defineComponent, ref} from 'vue';
import NavSidebar from '@/components/nav/NavSidebar.vue';
import {ElMessage} from 'element-plus';
import DemoNavSidebarLayout from '@/demo/views/nav/DemoNavSidebarLayout.vue';
import Tag from '@/components/tag/Tag.vue';

export default defineComponent({
  name: 'DemoNavSidebarList',
  components: {
    DemoNavSidebarLayout,
    NavSidebar,
    Tag,
  },
  setup() {
    const activeKey = ref<string>('1');

    const items = ref<NavItem[]>([
      {id: '1', title: 'Item 1'},
      {id: '2', title: 'Item 2'},
      {id: '3', title: 'Item 3'},
    ]);

    const activeItem = ref<NavItem>(items.value[0]);

    const onSelect = (item: NavItem) => {
      ElMessage.info(`Clicked item: ${item.title}`);
      activeKey.value = item.id;
      activeItem.value = item;
    };

    return {
      activeKey,
      items,
      onSelect,
      activeItem,
    };
  }
});
</script>

<style lang="scss" scoped>

</style>
