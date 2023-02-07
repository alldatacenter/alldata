<template>
  <div class="nav-menu" :class="[
    showCheckbox ? 'show-checkbox' : '',
  ].join(' ')">
    <el-tree
      ref="treeRef"
      :data="items"
      node-key="id"
      :props="{
        label: 'title',
        children: 'children',
        class: getClass,
      }"
      :show-checkbox="showCheckbox"
      :default-checked-keys="defaultCheckedKeys"
      :default-expand-all="defaultExpandAll"
      @node-click="onNodeClick"
      @check-change="onCheckChange"
    >
      <template #default="{data}">
        <span class="title">
          {{ data.title }}
        </span>
      </template>
    </el-tree>
  </div>
</template>

<script lang="ts">
import {defineComponent, PropType, ref} from 'vue';
import {emptyArrayFunc} from '@/utils/func';

export default defineComponent({
  name: 'NavSidebarTree',
  props: {
    activeKey: {
      type: String,
    },
    items: {
      type: Array as PropType<NavItem[]>,
      default: emptyArrayFunc,
    },
    showCheckbox: {
      type: Boolean,
      default: false,
    },
    defaultCheckedKeys: {
      type: Array as PropType<string[]>,
      default: emptyArrayFunc,
    },
    defaultExpandedKeys: {
      type: Array as PropType<string[]>,
      default: emptyArrayFunc,
    },
    defaultExpandAll: {
      type: Boolean,
      default: false,
    },
  },
  emits: [
    'select',
    'check',
  ],
  setup(props: NavSidebarTreeProps, {emit}) {
    const treeRef = ref();

    const onNodeClick = (item: NavItem) => {
      emit('select', item);
    };

    const onCheckChange = (item: NavItem, checked: boolean) => {
      emit('check', item, checked, treeRef.value?.getCheckedNodes());
    };

    const getClass = (item: NavItem): string | undefined => {
      if (item.id === props.activeKey) {
        return 'active';
      } else {
        return;
      }
    };

    return {
      treeRef,
      onNodeClick,
      onCheckChange,
      getClass,
    };
  },
});
</script>

<style lang="scss" scoped>
.nav-menu {
  height: 100%;
  overflow-y: auto;
}
</style>
<style scoped>
.nav-menu >>> .el-tree {
  /*overflow-y: auto;*/
}

.nav-menu >>> .el-tree-node {
  font-size: 14px;
  cursor: pointer;
}

.nav-menu >>> .el-tree-node > .el-tree-node__content {
  height: 48px;
}

.nav-menu >>> .el-tree-node > .el-tree-node__content:hover {
  background-color: #ecf5ff !important;
}

.nav-menu:not(.show-checkbox) >>> .el-tree-node.active > .el-tree-node__content,
.nav-menu:not(.show-checkbox) >>> .el-tree-node > .el-tree-node__content:hover {
  color: #409eff;
}

.nav-menu >>> .el-tree-node:focus > .el-tree-node__content,
.nav-menu >>> .el-tree-node:hover > .el-tree-node__content {
  background: inherit;
}
</style>
