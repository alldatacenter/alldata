<template>
  <el-container class="basic-layout">
    <cl-sidebar/>
    <el-container :class="sidebarCollapsed ? 'collapsed' : ''" class="container">
      <cl-header/>
      <cl-tabs-view/>
      <div class="container-body">
        <router-view/>
      </div>
    </el-container>
  </el-container>
</template>

<script lang="ts">
import {computed, defineComponent} from 'vue';
import {useStore} from 'vuex';

export default defineComponent({
  name: 'NormalLayout',
  setup() {
    const store = useStore();
    const {layout} = store.state as RootStoreState;

    const sidebarCollapsed = computed<boolean>(() => layout.sidebarCollapsed);

    return {
      sidebarCollapsed,
    };
  }
});
</script>

<style lang="scss" scoped>
.basic-layout {
  height: 100vh;

  .container {
    position: fixed;
    top: 0;
    left: var(--cl-sidebar-width);
    display: block;
    width: calc(100vw - var(--cl-sidebar-width));
    height: 100vh;
    transition: left var(--cl-sidebar-collapse-transition-duration);
    z-index: 2;

    &.collapsed {
      left: var(--cl-sidebar-width-collapsed);
      width: calc(100vw - var(--cl-sidebar-width-collapsed));
    }

    .container-body {
      background-color: var(--cl-container-bg);
      height: calc(100vh - var(--cl-header-height) - var(--cl-tabs-view-height));
      overflow: auto;
    }
  }
}
</style>
