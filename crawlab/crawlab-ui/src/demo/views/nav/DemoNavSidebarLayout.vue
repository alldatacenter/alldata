<template>
  <div class="demo-nav-sidebar-layout">
    <slot name="sidebar"/>
    <div class="content">
      <slot name="content"/>
    </div>
  </div>
</template>

<script lang="ts">
import {defineComponent} from 'vue';

export default defineComponent({
  name: 'DemoNavSidebarLayout',
  setup() {
    return {};
  }
});
</script>

<style lang="scss" scoped>
.demo-nav-sidebar-layout {
  display: flex;
  height: 100%;

  .content {
    padding: 20px;
    background-color: #ffffff;
    flex: 1 0;
  }
}
</style>
