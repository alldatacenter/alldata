<template>
  <div class="plugin-pid">
    <cl-tag
      v-for="s in statusWithPid"
      :key="JSON.stringify(s)"
      :type="getType(s)"
      :label="s.pid"
      :tooltip="s.node?.name"
    />
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, PropType} from 'vue';
import {emptyArrayFunc} from '@/utils/func';

export default defineComponent({
  name: 'PluginPid',
  props: {
    status: {
      type: Array as PropType<PluginStatus[]>,
      required: false,
      default: emptyArrayFunc,
    },
  },
  setup(props: PluginPidProps, {emit}) {
    const getType = (s: PluginStatus): BasicType => {
      return s.node?.is_master ? 'primary' : 'warning';
    };

    const statusWithPid = computed<PluginStatus[]>(() => {
      const {status} = props;
      if (!status) return [];
      return status.filter(s => !!s.pid);
    });

    return {
      getType,
      statusWithPid,
    };
  },
});
</script>

<style scoped lang="scss">

</style>
