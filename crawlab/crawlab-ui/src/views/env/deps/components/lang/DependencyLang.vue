<template>
  <cl-list-layout
    v-loading="loading"
    class="dependency-list"
    :table-columns="tableColumns"
    :table-data="tableData"
    :table-total="tableTotal"
    :table-pagination="tablePagination"
    :action-functions="actionFunctions"
    :visible-buttons="['export', 'customize-columns']"
    table-pagination-layout="total, prev, pager, next"
    :table-actions-prefix="tableActionsPrefix"
    @select="onSelect"
  >
    <template #nav-actions-extra>
      <div class="top-bar">
        <div class="top-bar-left">
          <el-input
            class="search-query"
            v-model="searchQuery"
            :placeholder="t('views.env.deps.common.actions.searchDependencies')"
            :prefix-icon="Search"
            clearable
            @keyup.enter="onSearch"
            @clear="onSearchClear"
          />
          <cl-label-button
            class="search-btn"
            :icon="['fa', 'search']"
            :placeholder="t('common.search')"
            :disabled="!installed ? !searchQuery : false"
            @click="onSearch"
          />
          <el-radio-group
            class="view-mode"
            v-model="viewMode"
            @change="onInstalledChange"
          >
            <el-radio-button label="installed">
              <font-awesome-icon :icon="['fa', 'check']" style="margin-right: 5px"/>
              {{ t('views.env.deps.common.status.installed') }}
            </el-radio-button>
            <el-radio-button label="installable">
              <font-awesome-icon :icon="icon" style="margin-right: 5px"/>
              {{ t('views.env.deps.common.status.installable') }}
            </el-radio-button>
          </el-radio-group>
          <cl-button
            class-name="tasks-btn"
            :type="runningTaskTotal === 0 ? 'primary' : 'warning'"
            @click="() => onDialogOpen('tasks')"
          >
            <font-awesome-icon
              :icon="runningTaskTotal === 0 ? ['fa', 'tasks'] : ['fa', 'spinner']"
              :spin="runningTaskTotal > 0"
              style="margin-right: 5px"
            />
            {{
              runningTaskTotal === 0 ? t('views.env.deps.task.tasks') : `${t('views.env.deps.task.tasks')} (${runningTaskTotal})`
            }}
          </cl-button>
          <cl-fa-icon-button
            class-name="update-btn"
            type="primary"
            :tooltip="updateTooltip"
            :icon="updateInstalledLoading ? ['fa', 'spinner'] : ['fa', 'sync']"
            :spin="updateInstalledLoading"
            :disabled="updateInstalledLoading"
            @click="onUpdate"
          />
        </div>
        <el-pagination
          :current-page="tablePagination.page"
          :page-size="tablePagination.pageSize"
          :total="tableTotal"
          class="pagination"
          layout="total, prev, pager, next"
          @current-change="(page) => tablePagination.page = page"
        />
      </div>
    </template>
    <template #extra>
      <cl-install-form
        :visible="dialogVisible.install"
        :nodes="allNodes"
        :names="installForm.names"
        @confirm="onInstall"
        @close="() => onDialogClose('install')"
      />
      <cl-uninstall-form
        :visible="dialogVisible.uninstall"
        :nodes="uninstallForm.nodes"
        :names="uninstallForm.names"
        @confirm="onUninstall"
        @close="() => onDialogClose('uninstall')"
      />
      <cl-dialog
        :title="t('views.env.deps.task.tasks')"
        :visible="dialogVisible.tasks"
        width="1024px"
        @confirm="() => onDialogClose('tasks')"
        @close="() => onDialogClose('tasks')"
      >
        <cl-dependency-task-list
          v-if="dialogVisible.tasks"
          :type="lang"
        />
      </cl-dialog>
    </template>
  </cl-list-layout>
</template>

<script lang="ts">
import {computed, defineComponent, h, onBeforeUnmount, onMounted, PropType, ref} from 'vue';
import {ElMessage} from 'element-plus';
import {Search} from '@element-plus/icons';
import {useStore} from 'vuex';
import {translate} from '@/utils';
import useRequest from '@/services/request';
import NavLink from '@/components/nav/NavLink.vue';
import Tag from '@/components/tag/Tag.vue';
import NodeType from '@/components/node/NodeType.vue';

const t = translate;

const endpointS = '/env/deps/settings';
const endpointT = '/env/deps/tasks';

const {
  get,
  getList: getList_,
  post,
} = useRequest();

const getDefaultForm = () => {
  return {
    type: 'mail',
    enabled: true,
  };
};

interface DependencyLangProps {
  lang: string;
  pathFunc: (name: string) => string;
  icon: string[];
}

export default defineComponent({
  name: 'DependencyLang',
  props: {
    lang: {
      type: String,
      required: true,
    },
    pathFunc: {
      type: Function as PropType<(name: string) => string>,
      required: true,
    },
    icon: {
      type: Array as PropType<string[]>,
      required: true,
    },
  },
  setup(props: DependencyLangProps) {
    const endpoint = computed<string>(() => `/env/deps/lang/${props.lang}`);

    const store = useStore();

    const viewMode = ref('installed');

    const installed = computed(() => viewMode.value === 'installed');

    const allNodeListSelectOptions = computed(() => store.getters[`node/allListSelectOptions`]);

    const allNodeDict = computed(() => store.getters[`node/allDict`]);

    const allNodes = computed<CNode[]>(() => store.state.node.allList);

    const runningTaskList = ref<TableData<EnvDepsTask>>([]);
    const runningTaskTotal = ref(0);

    const getRunningTaskList = async () => {
      const res = await getList_(`${endpointT}`, {
        all: true,
        conditions: [
          {
            key: 'type',
            op: 'eq',
            value: props.lang,
          },
          {
            key: 'status',
            op: 'eq',
            value: 'running',
          },
        ]
      });
      const {data, total} = res;
      runningTaskList.value = data || [];
      runningTaskTotal.value = total || 0;
    };

    let runningTaskHandle: any;

    onMounted(() => {
      getRunningTaskList();
      runningTaskHandle = setInterval(getRunningTaskList, 5000);
    });

    onBeforeUnmount(() => {
      clearInterval(runningTaskHandle);
    });

    const setting = ref({});

    const getSetting = async () => {
      const res = await get(`${endpointS}`, {
        conditions: [{
          key: 'key',
          op: 'eq',
          value: props.lang,
        }],
      });
      const {data} = res;
      if (data && data.length > 0) {
        setting.value = data[0];
      }
    };

    onMounted(getSetting);

    const updateTooltip = computed(() => {
      return t('common.actions.update');
    });

    const installForm = ref<EnvDepsInstallPayload>({
      names: [],
    });

    const uninstallForm = ref<EnvDepsUninstallPayload>({
      nodes: [],
      names: [],
    });

    const isInstallable = (dep: EnvDepsDependency) => {
      if (dep.upgradable) return true;
      let node_ids = [];
      if (installed.value) {
        node_ids = dep.node_ids || [];
      } else if (dep.result) {
        node_ids = dep.result.node_ids || [];
      } else {
        return false;
      }
      return node_ids.length < allNodes.value.length;
    };

    const isUninstallable = (dep: EnvDepsDependency) => {
      let node_ids = [];
      if (installed.value) {
        node_ids = dep.node_ids || [];
      } else if (dep.result) {
        node_ids = dep.result.node_ids || [];
      } else {
        return false;
      }
      return node_ids.length > 0;
    };

    const getNodes = (dep: EnvDepsDependency) => {
      let node_ids: string[] = [];
      if (installed.value) {
        node_ids = dep.node_ids || [];
      } else if (dep.result) {
        node_ids = dep.result.node_ids || [];
      } else {
        return [];
      }
      return node_ids.map(id => allNodeDict.value.get(id));
    };

    const tableColumns = computed<TableColumns<EnvDepsDependency>>(() => {
      return [
        {
          key: 'name',
          label: t('views.env.deps.dependency.form.name'),
          icon: ['fa', 'font'],
          width: '200',
          value: (row: EnvDepsDependency) => h(NavLink, {
            label: row.name,
            path: props.pathFunc(row.name as string),
            external: true,
          }),
        },
        {
          key: 'latest_version',
          label: t('views.env.deps.dependency.form.latestVersion'),
          icon: ['fa', 'tag'],
          width: '200',
        },
        {
          key: 'versions',
          label: t('views.env.deps.dependency.form.installedVersion'),
          icon: ['fa', 'tag'],
          width: '200',
          value: (row: EnvDepsDependency) => {
            const res = [];
            let versions = [];
            if (installed.value) {
              if (!row.versions) return;
              versions = row.versions;
            } else {
              if (!row.result || !row.result.versions) return;
              versions = row.result.versions;
            }
            res.push(h('span', {style: 'margin-right: 5px'}, versions.join(', ')));
            if (row.upgradable) {
              res.push(h(Tag, {
                type: 'primary',
                effect: 'light',
                size: 'mini',
                label: t('views.env.deps.common.status.upgradable'),
                icon: ['fa', 'arrow-up'],
              }));
            }
            return res;
          },
        },
        {
          key: 'node_ids',
          label: t('views.env.deps.dependency.form.installedNodes'),
          icon: ['fa', 'server'],
          width: '580',
          value: (row: EnvDepsDependency) => {
            const result = (installed.value ? row : row.result) || {};
            const node_ids = result.node_ids || [];
            return allNodes.value
              .filter(n => node_ids.includes(n._id))
              .map(n => {
                return h(NodeType, {
                  isMaster: n.is_master,
                  label: n.name,
                });
              });
          },
        },
        {
          key: 'actions',
          label: t('components.table.columns.actions'),
          fixed: 'right',
          width: '200',
          buttons: (row: EnvDepsDependency) => [
            {
              type: 'primary',
              icon: ['fa', 'download'],
              tooltip: row.upgradable ? t('views.env.deps.common.actions.installAndUpgrade') : t('common.actions.install'),
              disabled: (row) => !isInstallable(row),
              onClick: async (row) => {
                installForm.value.names = [row.name];
                dialogVisible.value.install = true;
              },
            },
            {
              type: 'danger',
              icon: ['fa', 'trash-alt'],
              tooltip: t('common.actions.uninstall'),
              disabled: (row) => !isUninstallable(row),
              onClick: async (row) => {
                uninstallForm.value.nodes = getNodes(row);
                uninstallForm.value.names = [row.name];
                dialogVisible.value.uninstall = true;
              },
            },
          ],
          disableTransfer: true,
        },
      ];
    });

    const tableData = ref<TableData<EnvDepsDependency>>([]);

    const tablePagination = ref({
      page: 1,
      size: 10,
    });

    const tableTotal = ref(0);

    const tableActionsPrefix = ref([
      {
        buttonType: 'fa-icon',
        label: t('common.actions.install'),
        tooltip: t('common.actions.install'),
        icon: ['fa', 'download'],
        type: 'primary',
        disabled: () => !installForm.value.names?.length,
        onClick: () => {
          dialogVisible.value.install = true;
        },
      },
      {
        buttonType: 'fa-icon',
        label: t('common.actions.uninstall'),
        tooltip: t('common.actions.uninstall'),
        icon: ['fa', 'trash-alt'],
        type: 'danger',
        disabled: () => !installed.value || !uninstallForm.value.names?.length,
        onClick: () => {
          dialogVisible.value.uninstall = true;
        },
      }
    ]);

    const loading = ref(false);

    const updateInstalledLoading = ref(false);

    const getList = async () => {
      loading.value = true;
      try {
        if (!searchQuery.value && !installed.value) {
          tableData.value = [];
          tableTotal.value = 0;
          return;
        }
        const params = {
          ...tablePagination.value,
          query: searchQuery.value,
          installed: installed.value,
        };
        const res = await getList_(`${endpoint.value}`, params);
        if (!res) {
          tableData.value = [];
          tableTotal.value = 0;
        }
        const {data, total} = res;
        tableData.value = data || [];
        tableTotal.value = total;
      } catch (e) {
        console.error(e);
      } finally {
        loading.value = false;
      }
    };

    const update = async () => {
      updateInstalledLoading.value = true;
      try {
        await post(`${endpoint.value}/update`);
      } finally {
        updateInstalledLoading.value = false;
        await getList();
      }
    };

    const actionFunctions = ref({
      getList,
      setPagination: (pagination: TablePagination) => {
        tablePagination.value = {...pagination};
      },
    });

    const searchQuery = ref();

    const form = ref(getDefaultForm());

    const dialogVisible = ref<{ [key: string]: boolean }>({
      install: false,
      uninstall: false,
      tasks: false,
    });

    const resetForms = () => {
      installForm.value = {
        names: [],
      };
      uninstallForm.value = {
        nodes: [],
        names: [],
      };
    };

    const onDialogOpen = (key: string) => {
      dialogVisible.value[key] = true;
    };

    const onDialogClose = (key: string) => {
      dialogVisible.value[key] = false;
      resetForms();
    };

    const onSearch = async () => {
      await actionFunctions.value.getList();
    };

    const onSearchClear = async () => {
      await actionFunctions.value.getList();
    };

    const onUpdate = async () => {
      await update();
    };

    const onInstalledChange = async () => {
      await actionFunctions.value.getList();
    };

    const onFilterChange = async () => {
      await actionFunctions.value.getList();
    };

    const onSelect = (rows: TableData<EnvDepsDependency>) => {
      installForm.value.names = rows.map(d => d.name || '');
      uninstallForm.value.names = rows.map(d => d.name || '');
    };

    const onInstall = async ({mode, upgrade, nodeIds}: { mode: string; upgrade: boolean; nodeIds: string[] }) => {
      const data = {
        mode,
        upgrade,
        names: installForm.value.names,
        node_id: [] as string[],
      };
      if (data.mode === 'all') {
        data['node_id'] = nodeIds;
      }
      await post(`${endpoint.value}/install`, data);
      await ElMessage.success(t('common.message.success.install'));
      await getRunningTaskList();
      onDialogClose('install');
    };

    const onUninstall = async ({mode, nodeIds}: { mode: string; nodeIds: string[] }) => {
      const data = {
        names: uninstallForm.value.names,
        mode,
        node_id: [] as string[],
      };
      if (data.mode === 'all') {
        data['node_id'] = nodeIds;
      }
      await post(`${endpoint.value}/uninstall`, data);
      await ElMessage.success(t('common.message.success.uninstall'));
      await getRunningTaskList();
      onDialogClose('uninstall');
    };

    onMounted(() => store.dispatch(`node/getAllList`));

    return {
      tableColumns,
      tableData,
      tableTotal,
      tablePagination,
      tableActionsPrefix,
      actionFunctions,
      dialogVisible,
      searchQuery,
      form,
      viewMode,
      installed,
      loading,
      updateInstalledLoading,
      allNodeListSelectOptions,
      allNodes,
      onDialogOpen,
      onDialogClose,
      onSearch,
      onSearchClear,
      onUpdate,
      onInstalledChange,
      onFilterChange,
      onSelect,
      installForm,
      uninstallForm,
      onInstall,
      onUninstall,
      setting,
      getSetting,
      updateTooltip,
      runningTaskList,
      runningTaskTotal,
      getRunningTaskList,
      Search,
      t,
    };
  },
});
</script>

<style scoped>
.search-query {
  width: 300px;
  margin-right: 10px;
}

.top-bar {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
}

.top-bar > * {
  display: flex;
  align-items: center;
}

.top-bar >>> .search-btn {
  margin-right: 0;
}

.top-bar >>> .update-btn,
.top-bar >>> .view-mode,
.top-bar >>> .tasks-btn {
  margin-left: 10px;
  margin-right: 0;
}

.top-bar .pagination {
  /*width: 100%;*/
  text-align: right;
}

.dependency-list >>> .node-type {
  margin-right: 10px;
}
</style>
