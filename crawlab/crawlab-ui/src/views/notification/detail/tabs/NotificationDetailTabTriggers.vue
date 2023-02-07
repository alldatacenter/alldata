<template>
  <div class="notification-detail-tab-triggers">
    <cl-transfer
      v-if="triggersList && triggersEnabled"
      :titles="triggersTitles"
      :data="triggersList"
      :value="triggersEnabled"
      @change="onTriggersChange"
    />
  </div>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import useNotificationDetail from '@/views/notification/detail/useNotificationDetail';

export default defineComponent({
  name: 'NotificationDetailTabTriggers',
  setup() {
    return {
      ...useNotificationDetail(),
    };
  },
});
</script>

<style lang="scss" scoped>

.notification-detail-tab-triggers {
  height: 100%;
  min-height: 100%;

  .transfer {
    height: 100%;
    min-height: 100%;
  }
}
</style>

<style scoped>
.notification-detail-tab-triggers >>> .transfer .el-card,
.notification-detail-tab-triggers >>> .transfer .el-card .check-list {
  height: 100%;
}
</style>
