<template>
  <div class="git-remote">
    <cl-git-form
      :branch-select-options="gitBranchSelectOptions"
      @change="onChange"
    />
  </div>
</template>

<script lang="ts">
import {computed, defineComponent} from 'vue';
import useSpiderDetail from '@/views/spider/detail/useSpiderDetail';
import {useStore} from 'vuex';

export default defineComponent({
  name: 'SpiderDetailTabGitRemote',
  setup() {
    // store
    const ns = 'spider';
    const store = useStore();

    const {
      activeId,
    } = useSpiderDetail();

    const gitBranchSelectOptions = computed<SelectOption[]>(() => store.getters[`${ns}/gitBranchSelectOptions`]);

    const onChange = async () => {
      console.debug('onChange');
      // await store.dispatch(`${ns}/getGitRemoteRefs`, {id: activeId.value});
    };

    return {
      ...useSpiderDetail(),
      gitBranchSelectOptions,
      onChange,
    };
  },
});
</script>

<style scoped lang="scss">
.git-remote {
  margin: 20px;
}
</style>
