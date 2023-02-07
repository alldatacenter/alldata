<template>
  <cl-list-layout store-namespace="tag">
  </cl-list-layout>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import DetailLayout from '@/layouts/content/detail/DetailLayout.vue';

export default defineComponent({
  name: 'TagDetail',
  setup(props, {emit}) {
    return {};
  },
});
</script>

<style lang="scss" scoped>

</style>
