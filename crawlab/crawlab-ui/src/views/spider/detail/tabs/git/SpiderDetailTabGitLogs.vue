<template>
  <div class="git-logs">
    <cl-table
      :data="tableData"
      :columns="tableColumns"
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
import Tag from '@/components/tag/Tag.vue';
import {GIT_REF_TYPE_BRANCH} from '@/constants/git';
import {TABLE_ACTION_CUSTOMIZE_COLUMNS} from '@/constants/table';
import {useI18n} from 'vue-i18n';
import useSpiderDetail from '../../useSpiderDetail';

export default defineComponent({
  name: 'SpiderDetailTabGitLogs',
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

    // table pagination
    const tablePagination = ref<TablePagination>({
      page: 1,
      size: 10,
    });

    const onPaginationChange = (pagination: TablePagination) => {
      tablePagination.value = {...pagination};
    };

    // all table data
    const allTableData = computed<TableData<GitRef>>(() => state.gitData?.logs || []);

    // table data
    const tableData = computed<TableData<GitLog>>(() => {
      const {page, size} = tablePagination.value;
      return allTableData.value.filter((_, i) => (i >= (page - 1) * size) && (i < page * size));
    });

    // table columns
    const tableColumns = computed<TableColumns<GitLog>>(() => {
      return [
        {
          key: 'ref',
          label: t('components.git.logs.table.columns.reference'),
          width: '120',
          icon: ['fa', 'tags'],
          value: (row: GitLog) => {
            return row.refs?.map(r => h(Tag, {
              label: r.name,
              icon: r.type === GIT_REF_TYPE_BRANCH ? ['fa', 'code-branch'] : ['fa', 'tag'],
              effect: r.type === GIT_REF_TYPE_BRANCH ? 'dark' : 'light',
              type: r.type === GIT_REF_TYPE_BRANCH ? 'primary' : 'success',
              tooltip: `${r.type}: ${r.name}`
            } as TagProps));
          },
        },
        {
          key: 'msg',
          label: t('components.git.logs.table.columns.commitMessage'),
          width: '650',
          icon: ['fa', 'comment-alt'],
        },
        {
          key: 'author',
          label: t('components.git.logs.table.columns.author'),
          width: '250',
          icon: ['fa', 'user'],
          value: (row: GitLog) => {
            return `${row.author_name}${row.author_email ? (' (' + row.author_email + ')') : ''}`;
          },
        },
        {
          key: 'timestamp',
          label: t('components.git.logs.table.columns.timestamp'),
          width: '200',
          icon: ['fa', 'clock'],
          fixed: 'right',
          value: (row: GitLog) => {
            return h(Time, {time: row.timestamp, ago: false, format: 'YYYY-MM-DD hh:mm:ss A'});
          }
        }
      ] as TableColumns<GitLog>;
    });

    onBeforeMount(async () => {
      store.dispatch(`${ns}/getGit`, {id: activeId.value});
    });

    return {
      tablePagination,
      onPaginationChange,
      allTableData,
      tableData,
      tableColumns,
      TABLE_ACTION_CUSTOMIZE_COLUMNS,
    };
  },
});
</script>

<style scoped lang="scss">
.git-logs {
  height: 100%;

  .table {
    height: 100%;
  }
}
</style>

<style scoped>
.git-logs >>> .el-table {
  border-top: none;
  border-left: none;
  border-right: none;
}
</style>
