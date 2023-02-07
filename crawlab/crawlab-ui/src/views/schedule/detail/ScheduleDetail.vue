<template>
  <cl-detail-layout store-namespace="schedule"/>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import useScheduleDetail from '@/views/schedule/detail/useScheduleDetail';

export default defineComponent({
  name: 'ScheduleDetail',
  setup() {
    return {
      ...useScheduleDetail(),
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
