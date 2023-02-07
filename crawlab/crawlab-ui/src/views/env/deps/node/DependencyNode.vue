<template>
  <cl-dependency-lang
    lang="node"
    :path-func="name => `https://npmjs.com/package/${name}`"
    :icon="['fab', 'node']"
  />
</template>

<script lang="ts">
import {defineComponent} from 'vue';

export default defineComponent({
  name: 'DependencyPython',
});
</script>

