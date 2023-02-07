<template>
  <cl-detail-layout store-namespace="spider">
    <template #actions>
      <cl-spider-detail-actions-common/>
      <cl-spider-detail-actions-files v-if="activeTabName === 'files'"/>
      <cl-spider-detail-actions-git v-if="activeTabName === 'git'"/>
      <cl-spider-detail-actions-data v-if="activeTabName === 'data'"/>
      <slot name="actions-suffix"/>
    </template>
  </cl-detail-layout>

  <!-- Dialogs (handled by store) -->
  <cl-upload-spider-files-dialog/>
  <cl-result-dedup-fields-dialog/>
  <!-- ./Dialogs -->
</template>
<script lang="ts">
import {defineComponent, onBeforeMount, onBeforeUnmount} from 'vue';
import {useStore} from 'vuex';
import useSpiderDetail from '@/views/spider/detail/useSpiderDetail';

export default defineComponent({
  name: 'SpiderDetail',
  setup() {
    const ns = 'spider';
    const nsGit = 'git';
    const store = useStore();

    const {
      saveFile,
      saveGit,
    } = useSpiderDetail();

    onBeforeMount(async () => {
      await Promise.all([
        store.dispatch(`project/getAllList`),
      ]);

      store.commit(`${ns}/setAfterSave`, [
        saveFile,
        saveGit,
      ]);
    });

    onBeforeUnmount(() => {
      store.commit(`${ns}/resetGitData`);
      store.commit(`${nsGit}/resetForm`);
    });

    return {
      ...useSpiderDetail(),
    };
  },
});
</script>
<style scoped lang="scss">
</style>
