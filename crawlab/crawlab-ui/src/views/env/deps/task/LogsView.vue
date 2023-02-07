<template>
  <div class="logs-view" v-html="content">
  </div>
</template>

<script lang="ts">
import {computed, defineComponent} from 'vue';

export default defineComponent({
  name: 'LogsView',
  props: {
    logs: {
      type: Array,
      default: () => {
        return [];
      },
    }
  },
  setup(props, {emit}) {
    const content = computed(() => {
      return props.logs.join('\n').replace(/\n/g, '<br>');
    });

    return {
      content,
    };
  },
});
</script>

<style scoped>
.logs-view {
  border: 1px solid rgb(244, 244, 245);
  padding: 10px;
  overflow: auto;
  min-height: 480px;
  max-height: 560px;
}
</style>
