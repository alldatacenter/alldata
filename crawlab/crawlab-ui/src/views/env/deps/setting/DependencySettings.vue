<template>
  <cl-list-layout
    :table-columns="tableColumns"
    :table-data="tableData"
    :table-total="tableTotal"
    :table-pagination="tablePagination"
    :action-functions="actionFunctions"
    :nav-actions="navActions"
    no-actions
    :visible-buttons="['export', 'customize-columns']"
  >
    <template #extra>
      <cl-dialog
        :visible="dialogVisible"
        width="800px"
        @confirm="onDialogConfirm"
        @close="onDialogClose"
      >
        <cl-dependency-setting-form
          :form="form"
          @change="onFormChange"
        />
      </cl-dialog>
    </template>
  </cl-list-layout>
</template>

<script lang="ts">
import {defineComponent, computed, ref, h} from 'vue';
import {ElMessage} from 'element-plus';
import useRequest from '@/services/request';
import {translate} from '@/utils';
import ClNavLink from '@/components/nav/NavLink.vue';

const t = translate;

const endpoint = '/env/deps/settings';

const {
  getList,
  post,
} = useRequest();

export default defineComponent({
  name: 'DependencySettings',
  setup(props, {emit}) {
    const form = ref<any>({});

    const dialogVisible = ref(false);

    const tableColumns = computed(() => [
      {
        key: 'name',
        label: t('views.env.deps.settings.form.name'),
        icon: ['fa', 'font'],
        width: '150',
        value: (row: any) => h(ClNavLink, {
          label: row.name,
          path: `/dependencies/${row.key}`,
        }),
      },
      // {
      //   key: 'enabled',
      //   label: 'Enabled',
      //   icon: ['fa', 'toggle-on'],
      //   width: '120',
      //   value: (row) => h(ClSwitch, {
      //     modelValue: row.enabled,
      //     onChange: async (value) => {
      //       if (!row._id) return;
      //       if (value) {
      //         await post(`${endpoint}/${row._id}/enable`);
      //       } else {
      //         await post(`${endpoint}/${row._id}/disable`);
      //       }
      //     },
      //   }),
      // },
      {
        key: 'description',
        label: t('views.env.deps.settings.form.description'),
        icon: ['fa', 'comment-alt'],
        width: '1000',
        value: (row: any) => t(row.description),
      },
      {
        key: 'actions',
        label: t('components.table.columns.actions'),
        fixed: 'right',
        width: '200',
        buttons: [
          {
            type: 'warning',
            icon: ['fa', 'cog'],
            tooltip: t('common.actions.manage'),
            onClick: (row: any) => {
              form.value = {...row};
              dialogVisible.value = true;
            },
          },
        ],
        disableTransfer: true,
      },
    ]);

    const tableData = ref([]);

    const tablePagination = ref({
      page: 1,
      size: 10,
    });

    const tableTotal = ref(0);

    const actionFunctions = ref({
      getList: async () => {
        const res = await getList(`${endpoint}`, {
          ...tablePagination.value,
        });
        if (!res) {
          tableData.value = [];
          tableTotal.value = 0;
        }
        const {data, total} = res;
        tableData.value = data as any;
        tableTotal.value = total;
      },
    });

    const onDialogClose = () => {
      form.value = {};
      dialogVisible.value = false;
    };

    const onDialogConfirm = async () => {
      if (!form.value._id) return;
      await post(`${endpoint}/${form.value._id}`, form.value);
      await ElMessage.success(t('common.message.success.save'));
      form.value = {};
      dialogVisible.value = false;
    };

    const onFormChange = (value: any) => {
      form.value = {...value};
    };

    return {
      tableColumns,
      tableData,
      tableTotal,
      tablePagination,
      actionFunctions,
      dialogVisible,
      form,
      onDialogClose,
      onDialogConfirm,
      onFormChange,
      t,
    };
  },
});
</script>

<style scoped>

</style>
