<template>
  <div class="project-detail-tab-spiders">
    <cl-spider-list no-actions embedded/>
  </div>
</template>
<script lang="ts">
import {computed, defineComponent, onBeforeMount, onBeforeUnmount} from 'vue';
import {useRoute} from 'vue-router';
import {useStore} from 'vuex';
import {FILTER_OP_EQUAL} from '@/constants/filter';

export default defineComponent({
  name: 'ProjectDetailTabSpiders',
  setup() {
    // route
    const route = useRoute();

    // store
    const ns = 'project';
    const store = useStore();

    // id
    const id = computed<string>(() => route.params.id as string);

    onBeforeMount(() => {
      store.commit(`spider/setTableListFilter`, [{
        key: 'project_id',
        op: FILTER_OP_EQUAL,
        value: id.value,
      } as FilterConditionData]);
    });

    onBeforeUnmount(() => {
      store.commit(`spider/resetTableListFilter`);
      store.commit(`spider/resetTableData`);
    });

    return {};
  },
});
</script>
<style lang="scss" scoped>
.project-detail-tab-overview {
  margin: 20px;
}
</style>
