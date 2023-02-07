<template>
  <div class="plugin-detail-tab-overview">
    <cl-plugin-form/>
  </div>
</template>
<script lang="ts">
import {defineComponent} from 'vue';

export default defineComponent({
  name: 'PluginDetailTabOverview',
  setup() {
    return {};
  },
});
</script>
<style lang="scss" scoped>
.plugin-detail-tab-overview {
  margin: 20px;
}
</style>
