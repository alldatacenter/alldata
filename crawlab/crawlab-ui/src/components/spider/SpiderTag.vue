<template>
  <cl-link-tag
      :label="spider.name"
      :path="path"
      type="primary"
  />
</template>

<script lang="ts">
import {computed, defineComponent, PropType} from 'vue';

export default defineComponent({
  name: 'SpiderTag',
  props: {
    spider: {
      type: Object as PropType<Spider>,
      required: true,
    },
  },
  setup(props: SpiderTagProps, {emit}) {
    const path = computed<string>(() => {
      const {spider} = props;
      const {_id} = spider;
      return `/spiders/${_id}`;
    });

    return {
      path,
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
