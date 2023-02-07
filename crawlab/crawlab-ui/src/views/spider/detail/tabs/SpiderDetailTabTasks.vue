<template>
  <div class="spider-detail-tab-tasks">
    <cl-task-list no-actions embedded/>
  </div>
</template>
<script lang="ts">
import {computed, defineComponent, onBeforeMount, onBeforeUnmount, watch} from 'vue';
import {useRoute} from 'vue-router';
import {useStore} from 'vuex';
import {FILTER_OP_EQUAL} from '@/constants/filter';

export default defineComponent({
  name: 'SpiderDetailTabTasks',
  setup() {
    // route
    const route = useRoute();

    // store
    const store = useStore();

    // id
    const id = computed<string>(() => route.params.id as string);

    const setTableListFilter = () => {
      // set filter
      store.commit(`task/setTableListFilter`, [{
        key: 'spider_id',
        op: FILTER_OP_EQUAL,
        value: id.value,
      }]);
    };

    const getData = async () => {
      setTableListFilter();
      await store.dispatch('task/getList');
    };

    onBeforeMount(getData);

    watch(() => id.value, getData);

    onBeforeUnmount(() => {
      store.commit(`task/resetTableListFilter`);
      store.commit(`task/resetTableData`);
    });

    return {};
  },
});
</script>

<style scoped>
.spider-detail-tab-tasks >>> .el-table {
  border: none;
}
</style>
