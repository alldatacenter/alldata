<template>
  <cl-nav-action-group class="task-detail-actions-logs">
    <cl-nav-action-fa-icon :icon="['fa', 'file-alt']"/>
    <cl-nav-action-item>
      <el-tooltip
          :content="t('components.task.logs.actions.autoUpdateLogs')"
      >
        <cl-switch
            v-model="internalAutoUpdate"
            @change="onAutoUpdateChange"
        />
      </el-tooltip>
    </cl-nav-action-item>
  </cl-nav-action-group>
</template>

<script lang="ts">
import {defineComponent, ref, watch} from 'vue';
import {useStore} from 'vuex';
import useTask from '@/components/task/task';
import {useI18n} from 'vue-i18n';

export default defineComponent({
  name: 'TaskDetailActionsLogs',
  setup() {
    // i18n
    const {t} = useI18n();

    // store
    const ns = 'task';
    const store = useStore();
    const {
      task: state,
    } = store.state as RootStoreState;

    // internal auto update
    const internalAutoUpdate = ref<boolean>(state.logAutoUpdate);

    // watch log auto update
    watch(() => state.logAutoUpdate, () => {
      setTimeout(() => {
        internalAutoUpdate.value = state.logAutoUpdate;
      }, 100);
    });

    // auto update change
    const onAutoUpdateChange = (value: boolean) => {
      if (value) {
        store.commit(`${ns}/enableLogAutoUpdate`);
      } else {
        store.commit(`${ns}/disableLogAutoUpdate`);
      }
    };

    return {
      ...useTask(store),
      internalAutoUpdate,
      onAutoUpdateChange,
      t,
    };
  },
});
</script>

<style scoped>
</style>
