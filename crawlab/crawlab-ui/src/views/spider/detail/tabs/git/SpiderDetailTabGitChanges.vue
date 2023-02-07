<template>
  <div class="git-changes">
    <cl-table
      ref="tableRef"
      :data="tableData"
      :columns="tableColumns"
      height="100%"
      selectable
      hide-footer
      @selection-change="onSelectionChange"
    />
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, h, onBeforeMount, ref, watch} from 'vue';
import {useStore} from 'vuex';
import GitFileStatus from '@/components/git/GitFileStatus.vue';
import Tag from '@/components/tag/Tag.vue';
import {useI18n} from 'vue-i18n';
import useSpiderDetail from '../../useSpiderDetail';

export default defineComponent({
  name: 'SpiderDetailTabGitChanges',
  setup() {
    // i18n
    const {t} = useI18n();

    // store
    const ns = 'spider';
    const store = useStore();
    const {
      spider: state,
    } = store.state as RootStoreState;

    const {
      activeId,
    } = useSpiderDetail();

    const getStatusTagProps = (status?: string): TagProps => {
      const label = status;
      switch (status) {
        case '?':
          return {type: 'danger', tooltip: t('components.git.changes.status.untracked'), label};
        case 'M':
          return {type: 'primary', tooltip: t('components.git.changes.status.modified'), label};
        case 'A':
          return {type: 'success', tooltip: t('components.git.changes.status.added'), label};
        case 'D':
          return {type: 'info', tooltip: t('components.git.changes.status.deleted'), label};
        case 'R':
          return {type: 'primary', tooltip: t('components.git.changes.status.renamed'), label};
        case 'C':
          return {type: 'primary', tooltip: t('components.git.changes.status.copied'), label};
        case 'U':
          return {type: 'danger', tooltip: t('components.git.changes.status.updatedButUnmerged'), label};
        default:
          return {label};
      }
    };

    // table ref
    const tableRef = ref<typeof Table>();

    // table data
    const tableData = computed<TableData<GitChange>>(() => state.gitData.changes || []);

    // table columns
    const tableColumns = computed<TableColumns<GitChange>>(() => {
      return [
        {
          key: 'changed_file',
          label: t('components.git.changes.table.columns.changedFile'),
          width: '1000',
          icon: ['far', 'file-code'],
          value: (row: GitChange) => {
            return h(GitFileStatus, {fileStatus: row});
          },
        },
        {
          key: 'status',
          label: t('components.git.changes.table.columns.status'),
          width: '100',
          icon: ['fa', 'edit'],
          fixed: 'right',
          value: (row: GitChange) => {
            return h(Tag, getStatusTagProps(row.worktree));
          },
        },
      ] as TableColumns<GitChange>;
    });

    const onSelectionChange = (rows: TableData<GitChange>) => {
      store.commit(`${ns}/setGitChangeSelection`, rows);
    };

    watch(() => tableData.value, () => tableRef.value?.clearSelection());

    onBeforeMount(async () => {
      store.dispatch(`${ns}/getGit`, {id: activeId.value});
    });

    return {
      tableRef,
      tableData,
      tableColumns,
      onSelectionChange,
    };
  },
});
</script>

<style scoped lang="scss">
.git-changes {
  height: 100%;

  .table {
    height: 100%;
  }
}
</style>

<style scoped>
.git-changes >>> .el-table {
  border-top: none;
  border-left: none;
  border-right: none;
}
</style>
