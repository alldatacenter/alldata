<template>
  <cl-nav-action-group>
    <cl-nav-action-fa-icon
      :icon="['fa', 'code-branch']"
      :tooltip="t('components.git.actions.title')"
    />
    <cl-nav-action-item>
      <cl-fa-icon-button
        :icon="['fa', 'download']"
        :tooltip="t('components.git.actions.tooltip.pull')"
        type="primary"
        :disabled="!gitForm.url || !gitForm.auth_type"
        @click="onClickPull"
      />
      <cl-fa-icon-button
        :icon="['fa', 'upload']"
        :tooltip="t('components.git.actions.tooltip.commit')"
        type="success"
        :disabled="!gitForm.url || !gitForm.auth_type"
        @click="onClickCommit"
      />
      <div v-if="gitCurrentBranch || gitCurrentBranchLoading" class="branch">
        <cl-tag
          v-if="gitCurrentBranchLoading"
          class-name="current-branch-loading"
          type="warning"
          :label="t('components.git.common.status.loading.label')"
          :tooltip="t('components.git.common.status.loading.tooltip')"
          :icon="['fa','spinner']"
          spinning
          size="large"
        />
        <cl-tag
          v-else
          class-name="current-branch"
          type="primary"
          :icon="['fa','code-branch']"
          size="large"
          :label="gitCurrentBranch"
          @click="onBranchClick"
        >
          <template #tooltip>
            <span>{{ t('components.git.common.currentBranch') }}:</span>
            <span
              style="color: #409eff; margin-left: 5px; font-weight: bolder"
            >
              {{ gitCurrentBranch }}
            </span>
          </template>
        </cl-tag>
      </div>
      <cl-switch
        v-model="gitForm.auto_pull"
        :active-text="t('components.git.form.autoPull')"
        :disabled="gitForm.url === '' || gitForm.auth_type === ''"
        @change="onAutoFillChange"
      />
    </cl-nav-action-item>
  </cl-nav-action-group>
</template>

<script lang="ts">
import {computed, defineComponent, ref, watch} from 'vue';
import {useStore} from 'vuex';
import {useI18n} from 'vue-i18n';
import useSpiderDetail from '@/views/spider/detail/useSpiderDetail';
import {ElMessage} from 'element-plus';

export default defineComponent({
  name: 'SpiderDetailActionsGit',
  setup() {
    // i18n
    const {t} = useI18n();

    // store
    const ns = 'spider';
    const store = useStore();
    const {
      spider: state,
      git: gitState,
    } = store.state as RootStoreState;

    const {
      gitCurrentBranch,
    } = useSpiderDetail();

    // git form
    const gitForm = computed<Git>(() => gitState.form);

    const internalBranch = ref<string>('');

    watch(() => gitCurrentBranch.value, () => internalBranch.value = gitCurrentBranch.value || '');

    const branches = computed<SelectOption[]>(() => {
      return state.gitData.branches?.map(branch => {
        return {
          label: branch.name,
          value: branch.name,
        };
      }) || [];
    });

    const isBranchClicked = ref<boolean>(false);

    const onBranchClick = () => {
      isBranchClicked.value = true;
    };

    const onBranchCancel = () => {
      isBranchClicked.value = false;
    };

    const onBranchCheckout = () => {
      isBranchClicked.value = false;
    };

    const onAutoFillChange = async () => {
      await store.dispatch(`git/updateById`, {id: gitForm.value._id, form: gitForm.value});
      await ElMessage.success(t('common.message.success.save'));
    };

    return {
      ...useSpiderDetail(),
      gitForm,
      gitCurrentBranch,
      internalBranch,
      branches,
      isBranchClicked,
      onBranchClick,
      onBranchCancel,
      onBranchCheckout,
      onAutoFillChange,
      t,
    };
  },
});
</script>

<style lang="scss" scoped>
.branch {
  display: flex;
  align-items: center;
  margin-right: 10px;

  .branch-select {
    .el-select {
      width: 120px;
    }
  }
}
</style>
<style scoped>
.branch >>> .branch-select .action {
  margin-left: 5px;
  margin-right: 10px;
}

.branch >>> .branch-select .action .button {
  width: 28px;
  height: 28px;
}

.branch >>> .branch-label:hover {
  opacity: 0.8;
}
</style>
