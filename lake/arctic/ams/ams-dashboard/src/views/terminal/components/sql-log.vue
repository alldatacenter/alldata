
<template>
  <div class="sql-log">
    <div v-html="outputLog" style="white-space: pre-wrap;font-size: 12px;"></div>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'

const outputLog = ref<string>('')

defineExpose({
  initData(log: string) {
    outputLog.value = log
  }
})

</script>
<style lang="less" scoped>
.sql-log {
  padding: 16px 16px 0;
  overflow: auto;
  height: 100%;
}
</style>
