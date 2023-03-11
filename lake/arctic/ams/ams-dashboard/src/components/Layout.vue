<template>
  <div class="layout">
    <!-- sidebar -->
    <side-bar />
    <div class="right-content">
      <!-- topbar -->
      <top-bar v-if="showTopBar" />
      <!-- content -->
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import SideBar from '@/components/Sidebar.vue'
import TopBar from '@/components/Topbar.vue'

export default defineComponent({
  name: 'Layout',
  components: {
    SideBar,
    TopBar
  },
  props: {
    menus: {
      type: Array,
      default: () => []
    },
    showTopBar: {
      type: Boolean,
      default: true
    }
  }
})
</script>

<style lang="less" scoped>
.layout {
  display: flex;
  width: 100%;
  height: 100%;
  min-width: 1200px;
  .right-content {
    display: flex;
    flex: 1;
    flex-direction: column;
    transition: width 0.3s;
    overflow: auto;
    .content {
      // padding: 16px 24px;
      height: calc(100% - 48px);
      overflow: auto;
    }
  }
}
</style>
