<template>
  <div class="list-layout">
    <div class="content">
      <!-- Nav Actions -->
      <cl-nav-actions v-if="!noActions" ref="navActions" class="nav-actions">
        <cl-nav-action-group
          v-for="(grp, i) in navActions"
          :key="grp.name || i"
        >
          <cl-nav-action-item
            v-for="(item, j) in grp.children"
            :key="`${grp.name}-${item.id || j}`"
          >
            <template v-if="item.action === ACTION_FILTER_SEARCH">
              <cl-filter-input
                :placeholder="item.placeholder"
                @change="(value) => item?.onChange(value)"
              />
            </template>
            <template v-else-if="item.action === ACTION_FILTER_SELECT">
              <cl-filter-select
                :label="item.label"
                :placeholder="item.placeholder"
                :options="item.options"
                :options-remote="item.optionsRemote"
                @change="(value) => item?.onChange(value)"
              />
            </template>
            <template v-else>
              <cl-nav-action-button
                v-auth="ACTION_ADD"
                :id="item.id"
                :class-name="item.className"
                :button-type="item.buttonType"
                :disabled="item.disabled"
                :icon="item.icon"
                :label="item.label"
                :size="item.size"
                :tooltip="item.tooltip"
                :type="item.type"
                @click="item.onClick"
              />
            </template>
          </cl-nav-action-item>
        </cl-nav-action-group>
        <slot name="nav-actions-extra"></slot>
      </cl-nav-actions>
      <!-- ./Nav Actions -->

      <!-- Table -->
      <cl-table
        ref="tableRef"
        :key="tableColumnsHash"
        :columns="tableColumns"
        :data="tableData"
        :total="tableTotal"
        :page="tablePagination.page"
        :page-size="tablePagination.size"
        selectable
        :selectable-function="selectableFunction"
        :visible-buttons="visibleButtons"
        :pagination-layout="tablePaginationLayout"
        :loading="tableLoading"
        :embedded="embedded"
        @selection-change="onSelect"
        @delete="onDelete"
        @edit="onEdit"
        @pagination-change="onPaginationChange"
        @header-change="onHeaderChange"
      >
        <template #actions-prefix>
          <cl-nav-action-button
            v-for="(btn, $index) in tableActionsPrefix"
            :key="$index"
            :button-type="btn.buttonType"
            :disabled="getNavActionButtonDisabled(btn)"
            :icon="btn.icon"
            :label="btn.label"
            :size="btn.size"
            :tooltip="btn.tooltip"
            :type="btn.type"
            @click="btn.onClick"
          />
        </template>
        <template #actions-suffix>
          <cl-nav-action-button
            v-for="(btn, $index) in tableActionsSuffix"
            :key="$index"
            :button-type="btn.buttonType"
            :disabled="btn.disabled"
            :icon="btn.icon"
            :label="btn.label"
            :size="btn.size"
            :tooltip="btn.tooltip"
            :type="btn.type"
            @click="btn.onClick"
          />
        </template>
      </cl-table>
      <!-- ./Table -->
    </div>

    <slot name="extra"/>
  </div>
</template>

<script lang="ts">
import {
  computed,
  defineComponent,
  onBeforeMount,
  onMounted,
  PropType,
  provide,
  ref,
  SetupContext,
  toRefs,
  watch
} from 'vue';
import {emptyArrayFunc, emptyObjectFunc} from '@/utils/func';
import {getMd5} from '@/utils/hash';
import {ACTION_ADD, ACTION_FILTER_SEARCH, ACTION_FILTER_SELECT} from '@/constants/action';

export default defineComponent({
  name: 'ListLayout',
  props: {
    navActions: {
      type: Array as PropType<ListActionGroup[]>,
      default: () => {
        return [];
      }
    },
    tableColumns: {
      type: Array as PropType<TableColumns>,
      default: () => {
        return [];
      }
    },
    tableData: {
      type: Array as PropType<TableData>,
      default: () => {
        return [];
      }
    },
    tableTotal: {
      type: Number,
      default: 0,
    },
    tablePagination: {
      type: Object as PropType<TablePagination>,
      default: () => {
        return {
          page: 1,
          size: 10,
        };
      }
    },
    tableListFilter: {
      type: Array as PropType<FilterConditionData[]>,
      default: emptyArrayFunc,
    },
    tableListSort: {
      type: Array as PropType<SortData[]>,
      default: emptyArrayFunc,
    },
    tableActionsPrefix: {
      type: Array as PropType<ListActionButton[]>,
      default: () => {
        return [];
      }
    },
    tableActionsSuffix: {
      type: Array as PropType<ListActionButton[]>,
      default: () => {
        return [];
      }
    },
    tableFilter: {},
    actionFunctions: {
      type: Object as PropType<ListLayoutActionFunctions>,
      default: emptyObjectFunc,
    },
    noActions: {
      type: Boolean,
      default: false,
    },
    selectableFunction: {
      type: Function as PropType<TableSelectableFunction>,
      default: () => true,
    },
    visibleButtons: {
      type: Array as PropType<BuiltInTableActionButtonName[]>,
      required: false,
      default: () => {
        return [];
      }
    },
    tablePaginationLayout: {
      type: String,
      required: false,
    },
    tableLoading: {
      type: Boolean,
      required: false,
    },
    tablePaginationPosition: {
      type: String as PropType<TablePaginationPosition>,
      required: false,
    },
    embedded: {
      type: Boolean,
      required: false,
    },
  },
  emits: [
    'select',
    'edit',
    'delete',
  ],
  setup(props: ListLayoutProps, {emit}: SetupContext) {
    const tableRef = ref();

    const computedTableRef = computed(() => tableRef.value);

    const onSelect = (value: TableData) => {
      emit('select', value);
    };

    const onEdit = (value: TableData) => {
      emit('edit', value);
    };

    const onDelete = (value: TableData) => {
      emit('delete', value);
    };

    const onPaginationChange = (value: TablePagination) => {
      props.actionFunctions?.setPagination(value);
    };

    // get list when table pagination changes
    watch(() => props.tablePagination, props.actionFunctions?.getList);

    // get list before mount
    onMounted(() => {
      props.actionFunctions?.getList();
    });

    provide<ListLayoutActionFunctions>('action-functions', props.actionFunctions);

    const getNavActionButtonDisabled = (btn: ListActionButton) => {
      if (typeof btn.disabled === 'boolean') {
        return btn.disabled;
      } else if (typeof btn.disabled === 'function') {
        return btn.disabled(computedTableRef.value);
      } else {
        return false;
      }
    };

    const tableColumnsHash = computed<string>(() => {
      const {tableColumns} = props;
      return getMd5(JSON.stringify(tableColumns));
    });

    return {
      tableRef,
      tableColumnsHash,
      onSelect,
      onPaginationChange,
      onHeaderChange: props.actionFunctions?.onHeaderChange,
      onEdit,
      onDelete,
      getNavActionButtonDisabled,
      ACTION_ADD,
      ACTION_FILTER_SEARCH,
      ACTION_FILTER_SELECT,
    };
  },
});
</script>

<style lang="scss" scoped>
.list-layout {
  .nav-actions {
    background-color: var(--cl-container-white-bg);
    border-bottom: none;

    .nav-action-group {
      .nav-action-item {
        &:not(:last-child) {
          margin-right: 10px;
        }
      }
    }
  }

  .content {
    background-color: var(--cl-container-white-bg);
  }
}
</style>
<style scoped>
.list-layout >>> .tag {
  margin-right: 10px;
}

</style>
