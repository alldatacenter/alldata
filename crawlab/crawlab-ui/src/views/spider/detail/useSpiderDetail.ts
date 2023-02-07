import {watch, computed, ref} from 'vue';
import {useStore} from 'vuex';
import useSpiderService from '@/services/spider/spiderService';
import {useRoute, useRouter} from 'vue-router';
import useGitService from '@/services/git/gitService';
import {getTabName} from '@/utils/route';
import {ElMessage, ElMessageBox} from 'element-plus';
import {sendEvent} from '@/admin/umeng';
import {translate} from '@/utils/i18n';
import Form from '@/components/form/Form.vue';
import {GIT_REF_TYPE_BRANCH} from '@/constants/git';
import useDetail from '@/layouts/content/detail/useDetail';

// i18n
const t = translate;

const gitCheckoutFormRef = ref<typeof Form>();

const gitCheckoutForm = ref({
  type: GIT_REF_TYPE_BRANCH,
  name: '',
});

const gitDialogVisible = ref({
  checkout: false,
});

const gitLoading = ref({
  checkout: false,
  pull: false,
  commit: false,
});

const useSpiderDetail = () => {
  const ns = 'spider';
  const nsDc = 'dataCollection';
  const store = useStore();
  const {
    spider: state,
    git: gitState,
  } = store.state as RootStoreState;

  const router = useRouter();

  const route = useRoute();

  const id = computed(() => route.params.id as string);

  const activeNavItem = computed<FileNavItem | undefined>(() => state.activeNavItem);

  const fileContent = computed<string>(() => state.fileContent);

  const {
    saveFile: save,
  } = useSpiderService(store);

  const {
    create: createGitForm,
    updateById: updateGitFormById,
  } = useGitService(store);

  const saveFile = async () => {
    if (!id.value || !activeNavItem.value?.path) return;
    await save(id.value, activeNavItem.value?.path, fileContent.value);
  };

  const activeTabName = computed<string>(() => getTabName(router));

  const saveGit = async () => {
    if (!id.value || !gitState.form.url || activeTabName.value !== 'git') return;
    if (gitState.form._id) {
      await updateGitFormById(gitState.form._id, gitState.form);
    } else {
      const res = await createGitForm({
        _id: state.form._id,
        ...gitState.form,
      });
      await store.dispatch(`git/getById`, res.data?._id);
    }
    await store.dispatch(`${ns}/getGit`, {id: id.value});
    await store.dispatch(`${ns}/getGitRemoteRefs`, {id: id.value});
  };

  const gitActions = {
    onClickPull: async () => {
      await ElMessageBox.confirm(
        t('components.git.common.messageBox.confirm.pull'),
        t('components.git.common.actions.pull'),
        {
          type: 'warning',
        }
      );
      gitLoading.value.pull = true;
      await saveGit();
      try {
        const res = await store.dispatch(`${ns}/gitPull`, {id: id.value});
        if (res) {
          await ElMessage.success(t('components.git.common.message.success.pull'));
        }
        await store.dispatch(`${ns}/getGit`, {id: id.value});
      } finally {
        gitLoading.value.pull = false;
      }

      sendEvent('click_spider_detail_git_pull');
    },
    onClickCommit: async () => {
      const res = await ElMessageBox.prompt(
        t('components.git.common.messageBox.prompt.commit.label'),
        t('components.git.common.actions.commit'),
        {
          type: 'warning',
          inputPlaceholder: t('components.git.common.messageBox.prompt.commit.placeholder'),
        }
      );
      const commitMessage = res.value;
      gitLoading.value.commit = true;
      // await saveGit();
      try {
        const res = await store.dispatch(`${ns}/gitCommit`, {id: id.value, commit_message: commitMessage});
        store.commit(`${ns}/resetGitChangeSelection`);
        if (res) {
          await ElMessage.success(t('components.git.common.message.success.commit'));
        }
        await store.dispatch(`${ns}/getGit`, {id: id.value});
      } finally {
        gitLoading.value.commit = false;
      }

      sendEvent('click_spider_detail_git_commit');
    },
    onDialogCheckoutConfirm: async () => {
      await gitCheckoutFormRef.value?.validate();
      gitDialogVisible.value.checkout = false;
      gitLoading.value.checkout = true;
      try {
        await store.dispatch(`${ns}/gitCheckout`, {id: id.value, branch: gitCheckoutForm.value.name});
        await ElMessage.success(t('components.git.common.message.success.checkout'));
        await store.dispatch(`${ns}/getGit`, {id: id.value});
      } finally {
        gitLoading.value.checkout = false;
      }

      sendEvent('click_spider_detail_git_checkout_confirm');
    },
  };

  const gitCurrentBranch = computed<string | undefined>(() => state.gitData?.current_branch);

  const gitCurrentBranchLoading = computed<boolean>(() => state.gitCurrentBranchLoading);

  watch(() => state.form?.col_id, (val) => {
    if (val) {
      store.dispatch(`${nsDc}/getById`, val);
    }
  });

  return {
    ...useDetail('spider'),
    saveFile,
    saveGit,
    gitCheckoutFormRef,
    gitCheckoutForm,
    gitDialogVisible,
    gitLoading,
    ...gitActions,
    gitCurrentBranch,
    gitCurrentBranchLoading,
  };
};

export default useSpiderDetail;
