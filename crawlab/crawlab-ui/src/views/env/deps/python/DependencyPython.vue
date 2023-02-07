<template>
  <cl-dependency-lang
    lang="python"
    :path-func="name => `https://pypi.org/project/${name}`"
    :icon="['fab', 'python']"
  />
</template>

<script lang="ts">
import {defineComponent} from 'vue';

export default defineComponent({
  name: 'DependencyPython',
});
</script>

