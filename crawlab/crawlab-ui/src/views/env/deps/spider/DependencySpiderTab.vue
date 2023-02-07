<template>
  <div class="dependency-spider-tab">
    <div class="top-bar">
      <cl-form
        :model="spiderData"
        inline
      >
        <cl-form-item :label="t('views.env.deps.spider.form.dependencyType')">
          <cl-tag
            :label="spiderDataDependencyTypeLabel"
            :type="spiderDataDependencyTypeType"
            :tooltip="spiderDataDependencyTypeTooltip"
            size="large"
          />
        </cl-form-item>
      </cl-form>
      <cl-button
        class-name="action-btn"
        :tooltip="t('common.actions.install')"
        :disabled="!spiderData.dependency_type"
        @click="onInstallByConfig"
      >
        <font-awesome-icon class="icon" :icon="['fa', 'download']"/>
        {{ t('common.actions.install') }}
      </cl-button>
    </div>
    <cl-table
      :data="tableData"
      :columns="tableColumns"
      hide-footer
    />
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
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, h, onMounted, ref} from 'vue';
import {useRoute} from 'vue-router';
import {useStore} from 'vuex';
import {ElMessage, ElMessageBox} from 'element-plus';
import useRequest from '@/services/request';
import {translate} from '@/utils';
import NavLink from '@/components/nav/NavLink.vue';
import NodeType from '@/components/node/NodeType.vue';
import Tag from '@/components/tag/Tag.vue';

const t = translate;

const {
  get,
  post,
} = useRequest();

const endpoint = '/env/deps';

export default defineComponent({
  name: 'DependencySpiderTab',
  setup() {
    const route = useRoute();

    const store = useStore();

    const allNodes = computed<CNode[]>(() => store.state.node.allList);

    onMounted(() => store.dispatch(`node/getAllList`));

    const isInstallable = (dep: EnvDepsDependency) => {
      const {result} = dep;
      if (!result) return true;
      if (result.upgradable || result.downgradable) return true;
      const node_ids = result.node_ids || [];
      return node_ids.length < allNodes.value.length;
    };

    const isUninstallable = (dep: EnvDepsDependency) => {
      const {result} = dep;
      if (!result) return true;
      const node_ids = result.node_ids || [];
      return node_ids.length > 0;
    };

    const tableColumns = computed<TableColumns<EnvDepsDependency>>(() => {
      return [
        {
          key: 'name',
          label: t('views.env.deps.spider.form.name'),
          icon: ['fa', 'font'],
          width: '200',
          value: (row: EnvDepsDependency) => h(NavLink, {
            label: row.name,
            path: `https://pypi.org/project/${row.name}`,
            external: true,
          }),
        },
        {
          key: 'version',
          label: t('views.env.deps.spider.form.requiredVersion'),
          icon: ['fa', 'tag'],
          width: '200',
        },
        // {
        //   key: 'latest_version',
        //   label: 'Latest Version',
        //   icon: ['fa', 'tag'],
        //   width: '200',
        // },
        {
          key: 'versions',
          label: t('views.env.deps.spider.form.installedVersion'),
          icon: ['fa', 'tag'],
          width: '200',
          value: (row: EnvDepsDependency) => {
            const res = [];
            if (!row.result || !row.result.versions) return;
            const {result} = row;
            if (!result) return;
            const {versions} = result;
            res.push(h('span', {style: 'margin-right: 5px'}, versions?.join(', ')));
            if (result.upgradable) {
              res.push(h(Tag, {
                type: 'primary',
                effect: 'light',
                size: 'mini',
                tooltip: t('views.env.deps.common.status.upgradable'),
                icon: ['fa', 'arrow-up'],
              }));
            } else if (result.downgradable) {
              res.push(h(Tag, {
                type: 'warning',
                effect: 'light',
                size: 'mini',
                tooltip: t('views.env.deps.common.status.downgradable'),
                icon: ['fa', 'arrow-down'],
              }));
            }
            return res;
          },
        },
        {
          key: 'node_ids',
          label: t('views.env.deps.spider.form.installedNodes'),
          icon: ['fa', 'server'],
          width: 'auto',
          value: (row: EnvDepsDependency) => {
            const result = row.result || {};
            let {node_ids} = result;
            if (!node_ids) return;
            return allNodes.value
              .filter(n => node_ids?.includes(n._id as string))
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
          buttons: (row: EnvDepsDependency) => {
            let {result} = row;
            if (!result) result = {};
            let tooltip;
            if (result.upgradable) {
              tooltip = t('views.env.deps.common.actions.installAndUpgrade');
            } else if (result.downgradable) {
              tooltip = t('views.env.deps.common.actions.installAndDowngrade');
            } else if (isInstallable(row)) {
              tooltip = t('common.actions.install');
            } else {
              tooltip = '';
            }
            return [
              {
                type: 'primary',
                icon: ['fa', 'download'],
                tooltip,
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
                onClick: async (row: EnvDepsDependency) => {
                  uninstallForm.value.names = [row.name as string];
                  dialogVisible.value.uninstall = true;
                },
              },
            ];
          },
          disableTransfer: true,
        },
      ];
    });

    const spiderData = ref({
      dependency_type: '',
      dependencies: [],
    });

    const tableData = computed(() => {
      if (!spiderData.value.dependencies) return [];
      return spiderData.value.dependencies;
    });

    const getSpiderData = async () => {
      const id = route.params.id;
      if (!id) return;
      const res = await get(`${endpoint}/spiders/${id}`);
      const {data} = res;
      spiderData.value = data;
    };

    onMounted(getSpiderData);

    const spiderDataDependencyTypeLabel = computed(() => {
      switch (spiderData.value.dependency_type) {
        case 'requirements.txt':
          return 'Python Pip';
        case 'package.json':
          return 'NPM';
        default:
          return t('views.env.deps.common.status.noDependencyType');
      }
    });

    const spiderDataDependencyTypeType = computed(() => {
      switch (spiderData.value.dependency_type) {
        case 'requirements.txt':
          return 'primary';
        case 'package.json':
          return 'primary';
        default:
          return 'info';
      }
    });

    const spiderDataDependencyTypeTooltip = computed(() => {
      switch (spiderData.value.dependency_type) {
        case 'requirements.txt':
        case 'package.json':
          return spiderData.value.dependency_type;
        default:
          return t('common.mode.other');
      }
    });

    const installForm = ref<EnvDepsInstallPayload>({
      names: [],
    });

    const uninstallForm = ref<EnvDepsUninstallPayload>({
      nodes: [],
      names: [],
    });

    const dialogVisible = ref<{ [key: string]: boolean }>({
      install: false,
      uninstall: false,
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

    const onDialogClose = (key: string) => {
      dialogVisible.value[key] = false;
      resetForms();
    };

    const onInstall = async ({mode, upgrade, nodeIds}: { mode: string; upgrade: boolean; nodeIds: string[] }) => {
      const id = route.params.id;
      if (!id) return;
      const data = {
        mode,
        upgrade,
        names: installForm.value.names,
        node_id: [] as string[],
      };
      if (data.mode === 'all') {
        data['node_id'] = nodeIds;
      }
      await post(`${endpoint}/spiders/${id}/install`, data);
      await ElMessage.success(t('common.messageBox.success.startInstall'));
      onDialogClose('install');
    };

    const onUninstall = async ({mode, nodeIds}: { mode: string; nodeIds: string[] }) => {
      const id = route.params.id;
      if (!id) return;
      const data = {
        names: uninstallForm.value.names,
        mode,
        node_id: [] as string[],
      };
      if (data.mode === 'all') {
        data['node_id'] = nodeIds;
      }
      await post(`${endpoint}/spiders/${id}/uninstall`, data);
      await ElMessage.success(t('common.messageBox.success.startUninstall'));
      onDialogClose('uninstall');
    };

    const onSelect = (rows: EnvDepsDependency[]) => {
      installForm.value.names = rows.map(d => d.name as string);
    };

    const onInstallByConfig = async () => {
      await ElMessageBox.confirm(t('common.messageBox.confirm.install'), t('common.actions.install'));
      const id = route.params.id;
      if (!id) return;
      const mode = 'all';
      const data = {
        mode,
        use_config: true,
        spider_id: id,
        node_id: [] as string[],
      };
      if (data.mode === 'all') {
        data['node_id'] = allNodes.value.map(d => d._id as string);
      }
      await post(`${endpoint}/spiders/${id}/install`, data);
      await ElMessage.success(t('common.messageBox.success.startInstall'));
    };

    return {
      tableColumns,
      tableData,
      spiderData,
      spiderDataDependencyTypeLabel,
      spiderDataDependencyTypeType,
      spiderDataDependencyTypeTooltip,
      allNodes,
      onSelect,
      onDialogClose,
      onInstall,
      onUninstall,
      dialogVisible,
      installForm,
      uninstallForm,
      onInstallByConfig,
      t,
    };
  },
});
</script>

<style scoped>
.dependency-spider-tab .top-bar {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #e6e6e6;
}

.dependency-spider-tab .top-bar >>> .el-form-item {
  margin-bottom: 0;
}

.dependency-spider-tab .top-bar >>> .action-btn {
  margin-left: 10px;
}

.dependency-spider-tab .top-bar >>> .icon {
  margin-right: 5px;
}

.dependency-spider-tab >>> .el-table {
  border-top: none;
  border-left: none;
  border-right: none;
}

.dependency-spider-tab >>> .el-table::before,
.dependency-spider-tab >>> .el-table::after,
.dependency-spider-tab >>> .el-table--border::before,
.dependency-spider-tab >>> .el-table--border::after {
  display: none;
}

.dependency-spider-tab >>> .el-table .el-table__inner-wrapper::before,
.dependency-spider-tab >>> .el-table .el-table__inner-wrapper::after {
  display: none;
}

.dependency-spider-tab >>> .el-table .el-table__inner-wrapper thead,
.dependency-spider-tab >>> .el-table .el-table__inner-wrapper tbody,
.dependency-spider-tab >>> .el-table .el-table__inner-wrapper tr,
.dependency-spider-tab >>> .el-table .el-table__inner-wrapper tr > th:first-child,
.dependency-spider-tab >>> .el-table .el-table__inner-wrapper tr > td:first-child {
  border-left: none;
}

</style>
