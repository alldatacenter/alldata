<template>
  <div class="git-references">
    <cl-table
      :key="gitRefType"
      :columns="tableColumns"
      :data="tableData"
      :page="tablePagination.page"
      :page-size="tablePagination.size"
      :total="allTableData.length"
      :visible-buttons="[TABLE_ACTION_CUSTOMIZE_COLUMNS]"
      @pagination-change="onPaginationChange"
    />
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, h, onBeforeMount, ref} from 'vue';
import {useStore} from 'vuex';
import Time from '@/components/time/Time.vue';
import {GIT_REF_TYPE_BRANCH} from '@/constants/git';
import {TABLE_ACTION_CUSTOMIZE_COLUMNS} from '@/constants/table';
import {useI18n} from 'vue-i18n';
import useSpiderDetail from '@/views/spider/detail/useSpiderDetail';

export default defineComponent({
  name: 'SpiderDetailTabGitReferences',
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

    // git logs map
    const gitLogsMap = computed<Map<string, GitLog>>(() => store.getters[`${ns}/gitLogsMap`] as Map<string, GitLog>);

    // git ref type
    const gitRefType = computed<string>(() => state.gitRefType);

    // table pagination
    const tablePagination = ref<TablePagination>({
      page: 1,
      size: 10,
    });

    const onPaginationChange = (pagination: TablePagination) => {
      tablePagination.value = {...pagination};
    };

    // all table data
    const allTableData = computed<TableData<GitRef>>(() => state.gitRemoteRefs
      .filter(r => r.type === gitRefType.value));

    // table data
    const tableData = computed<TableData<GitRef>>(() => {
      const {page, size} = tablePagination.value;
      return allTableData.value.filter((_, i) => (i >= (page - 1) * size) && (i < page * size));
    });

    // table columns
    const tableColumns = computed<TableColumns<GitLog>>(() => {
      return [
        {
          key: 'name',
          label: gitRefType.value === GIT_REF_TYPE_BRANCH ?
            t('components.git.references.type.branch') :
            t('components.git.references.type.tag'),
          width: '1000',
          icon: gitRefType.value === GIT_REF_TYPE_BRANCH ? ['fa', 'code-branch'] : ['fa', 'tag']
        },
        {
          key: 'timestamp',
          label: t('components.git.references.table.columns.timestamp'),
          width: '200',
          icon: ['fa', 'clock'],
          fixed: 'right',
          value: (row: GitLog) => {
            if (!row.hash) return;
            const l = gitLogsMap.value.get(row.hash);
            if (!l?.timestamp) return;
            return h(Time, {time: l.timestamp, ago: false, format: 'YYYY-MM-DD hh:mm:ss A'});
          }
        }
      ] as TableColumns<GitLog>;
    });

    onBeforeMount(async () => {
      store.dispatch(`${ns}/getGitRemoteRefs`, {id: activeId.value});
    });

    return {
      allTableData,
      tableData,
      tableColumns,
      tablePagination,
      onPaginationChange,
      gitRefType,
      TABLE_ACTION_CUSTOMIZE_COLUMNS,
    };
  },
});
</script>

<style scoped lang="scss">
.git-references {
  height: 100%;

  .table {
    //height: 100%;
  }
}
</style>

<style scoped>
.git-references >>> .el-table {
  border-top: none;
  border-left: none;
  border-right: none;
}

.git-references >>> .el-table:before,
.git-references >>> .el-table .el-table__inner-wrapper:before {
}
</style>
