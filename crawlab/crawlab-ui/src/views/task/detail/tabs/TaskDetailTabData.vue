<template>
  <div class="task-detail-tab-data">
    <cl-result-list
      :id="form?.spider?.col_id"
      :data-source-id="form?.spider?.data_source_id"
      :filter="filter"
      :display-all-fields="displayAllFields"
      no-actions
      embedded
    />
  </div>
</template>
<script lang="ts">
import {computed, defineComponent, watch} from 'vue';
import {useStore} from 'vuex';
import useTask from "@/components/task/task";
import useTaskDetail from "@/views/task/detail/useTaskDetail";
import {FILTER_OP_EQUAL} from "@/constants";

export default defineComponent({
  name: 'TaskDetailTabOverview',
  setup() {
    // store
    const nsDc = 'dataCollection';
    const store = useStore();
    const {
      task: state,
    } = store.state as RootStoreState;

    const {
      activeId,
    } = useTaskDetail();

    const filter = computed<FilterConditionData[]>(() => {
      return [
        {
          key: '_tid',
          op: FILTER_OP_EQUAL,
          value: activeId.value,
        },
      ];
    });

    const displayAllFields = computed<boolean>(() => state.dataDisplayAllFields);

    watch(() => state.form?.spider?.col_id, (val) => {
      if (val) {
        store.dispatch(`${nsDc}/getById`, val);
      }
    });

    return {
      ...useTask(store),
      filter,
      displayAllFields,
    };
  },
});
</script>
<style scoped>
.task-detail-tab-data >>> .el-table {
  border: none;
}
</style>
