<template>
  <div class="user-detail-tab-overview">
    <cl-user-form/>
  </div>
</template>
<script lang="ts">
import {defineComponent} from 'vue';
import UserForm from '@/components/user/UserForm.vue';

export default defineComponent({
  name: 'ProjectDetailTabOverview',
  setup() {
    return {};
  },
});
</script>
<style lang="scss" scoped>
.user-detail-tab-overview {
  margin: 20px;
}
</style>
