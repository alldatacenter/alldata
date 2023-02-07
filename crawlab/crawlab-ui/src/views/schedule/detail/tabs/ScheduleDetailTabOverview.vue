<template>
  <div class="schedule-detail-tab-overview">
    <cl-schedule-form/>
  </div>
</template>
<script lang="ts">
import {defineComponent} from 'vue';

export default defineComponent({
  name: 'ProjectDetailTabOverview',
  setup() {
    return {};
  },
});
</script>
<style lang="scss" scoped>
.schedule-detail-tab-overview {
  margin: 20px;
}
</style>
