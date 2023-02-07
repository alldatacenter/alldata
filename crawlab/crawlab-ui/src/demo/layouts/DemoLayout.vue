<template>
  <div class="demo-layout">
    <el-tabs :active-name="activeName" @tab-click="(tab) => $emit('tab-click', tab)">
      <slot/>
    </el-tabs>
  </div>
</template>

<script lang="ts">
import {defineComponent} from 'vue';

export default defineComponent({
  name: 'DemoLayout',
  props: {
    activeName: {
      type: String,
    }
  },
  emits: [
    'tab-click',
  ],
  setup(props: DemoLayoutProps) {
    return {};
  }
});
</script>

<style lang="scss" scoped>
.demo-layout {
  height: 100%;
  background-color: #ffffff;
}
</style>

<style scoped>
.demo-layout >>> .el-tabs {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.demo-layout >>> .el-tabs .el-tabs__header {
  margin: 0;
  flex: 0 0;
}

.demo-layout >>> .el-tabs .el-tabs__content {
  flex: 1 0;
}

.demo-layout >>> .el-tabs .el-tabs__content .el-tab-pane {
  height: 100%;
}
</style>
