<template>
  <cl-detail-layout store-namespace="notification"/>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import useNotificationDetail from '@/views/notification/detail/useNotificationDetail';

export default defineComponent({
  name: 'NotificationDetail',
  setup() {
    return {
      ...useNotificationDetail(),
    };
  },
});
</script>

<style scoped>

</style>
