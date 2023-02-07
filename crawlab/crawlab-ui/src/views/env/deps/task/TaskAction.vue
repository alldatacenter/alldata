<template>
  <cl-tag
      :type="type"
      :label="actionName"
      :icon="icon"
  />
</template>

<script lang="ts">
import {computed, defineComponent} from 'vue';

export default defineComponent({
  name: 'TaskAction',
  props: {
    action: {
      type: String,
    },
  },
  setup(props, {emit}) {
    const type = computed(() => {
      if (props.action === 'install') {
        return 'primary';
      } else if (props.action === 'uninstall') {
        return 'danger';
      } else if (props.action === 'upgrade') {
        return 'primary';
      } else {
        return 'info';
      }
    });

    const icon = computed(() => {
      if (props.action === 'install') {
        return ['fa', 'download'];
      } else if (props.action === 'uninstall') {
        return ['fa', 'trash-alt'];
      } else if (props.action === 'upgrade') {
        return ['fa', 'angle-up'];
      } else {
        return ['fa', 'question'];
      }
    });

    const actionName = computed(() => {
      if (!props.action) return 'unknown';
      const arr = props.action.split('');
      arr[0] = arr[0].toUpperCase();
      return arr.join('');
    });

    return {
      type,
      icon,
      actionName,
    };
  },
});
</script>

<style scoped>

</style>
