<template>
  <div class="notification-detail-tab-overview">
    <!-- Notification Form (handled by store) -->
    <cl-notification-form/>
    <!-- ./Notification Form -->
  </div>
</template>
<script lang="ts">
import {defineComponent} from 'vue';

export default defineComponent({
  name: 'NotificationDetailTabOverview',
  setup() {
    return {};
  },
});
</script>
<style lang="scss" scoped>
.notification-detail-tab-overview {
  margin: 20px;
}
</style>
