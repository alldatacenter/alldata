<template>
  <cl-nav-action-item class="nav-action-fa-icon" is-label>
    <el-tooltip :content="tooltip" :disabled="!tooltip">
      <font-awesome-icon :icon="icon" class="title"/>
    </el-tooltip>
  </cl-nav-action-item>
</template>

<script lang="ts">
import {defineComponent, PropType} from 'vue';

export default defineComponent({
  name: 'NavActionFaIcon',
  props: {
    tooltip: {
      type: String,
    },
    icon: {
      type: [String, Array] as PropType<Icon>,
    },
  },
  setup(props, {emit}) {
    return {};
  },
});
</script>

<style scoped>
.nav-action-fa-icon {
  height: 32px;
}
</style>
