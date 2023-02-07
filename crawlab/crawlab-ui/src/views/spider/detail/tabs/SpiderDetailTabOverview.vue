<template>
  <div class="spider-detail-tab-overview">
    <!-- Spider Form (handled by store) -->
    <cl-spider-form/>
    <!-- ./Spider Form -->
  </div>
</template>
<script lang="ts">
import {defineComponent} from 'vue';

export default defineComponent({
  name: 'SpiderDetailTabOverview',
  setup() {
    return {};
  },
});
</script>
<style lang="scss" scoped>
.spider-detail-tab-overview {
  margin: 20px;
}
</style>
