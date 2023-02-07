<template>
  <DemoLayout active-name="basic">
    <el-tab-pane name="basic" label="Basic">
      <DemoNavSidebarLayout>
        <template #sidebar>
          <NavSidebar
            type="tree"
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
    </el-tab-pane>
    <el-tab-pane name="checkbox" label="Checkbox">
      <DemoNavSidebarLayout>
        <template #sidebar>
          <NavSidebar
            type="tree"
            :active-key="activeKey"
            :items="items"
            show-checkbox
            @select="onSelect"
            @check="onCheck"
          />
        </template>
        <template #content>
        </template>
      </DemoNavSidebarLayout>
    </el-tab-pane>
    <el-tab-pane name="checkbox-default-checked" label="Checkbox with default checked">
      <DemoNavSidebarLayout>
        <template #sidebar>
          <NavSidebar
            type="tree"
            :active-key="activeKey"
            :items="items"
            show-checkbox
            :default-checked-keys="defaultCheckedKeys"
            @select="onSelect"
            @check="onCheck"
          />
        </template>
        <template #content>
        </template>
      </DemoNavSidebarLayout>
    </el-tab-pane>
  </DemoLayout>
</template>

<script lang="ts">
import {defineComponent, ref} from 'vue';
import NavSidebar from '@/components/nav/NavSidebar.vue';
import {ElMessage} from 'element-plus';
import Tag from '@/components/tag/Tag.vue';
import DemoNavSidebarLayout from '@/demo/views/nav/DemoNavSidebarLayout.vue';
import DemoLayout from '@/demo/layouts/DemoLayout.vue';

export default defineComponent({
  name: 'DemoNavSidebarTree',
  components: {
    DemoLayout,
    DemoNavSidebarLayout,
    Tag,
    NavSidebar,
  },
  setup() {
    const activeKey = ref<string>('1');

    const items = ref<NavItem[]>([
      {id: '1', title: 'Item 1'},
      {id: '2', title: 'Item 2'},
      {
        id: '3',
        title: 'Item 3',
        children: [
          {
            id: '3.1',
            title: 'Item 3.1',
          },
          {
            id: '3.2',
            title: 'Item 3.2',
          }
        ]
      },
    ]);

    const activeItem = ref<NavItem>(items.value[0]);

    const defaultCheckedKeys = ref<string[]>([
      '2',
      '3.1',
    ]);

    const onSelect = (item: NavItem) => {
      ElMessage.info(`Clicked item: ${item.title}`);
      activeKey.value = item.id;
      activeItem.value = item;
    };

    const onCheck = (item: NavItem, checked: boolean, items: NavItem[]) => {
      ElMessage.info(`Checked items: ${items?.map(item => item.title).join(', ')}`);
    };

    return {
      items,
      activeKey,
      activeItem,
      defaultCheckedKeys,
      onSelect,
      onCheck,
    };
  }
});
</script>

<style lang="scss" scoped>

</style>
