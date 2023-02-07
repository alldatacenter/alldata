<template>
  <div v-loading="loading" ref="tableWrapperRef" :class="['table', embedded ? 'embedded': ''].join(' ')">
    <!-- Table Header -->
    <div class="table-header">
      <el-pagination
        v-if="[TABLE_PAGINATION_POSITION_ALL, TABLE_PAGINATION_POSITION_TOP].includes(paginationPosition)"
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        class="pagination"
        :layout="paginationLayout"
        @current-change="onCurrentChange"
        @size-change="onSizeChange"
      />
    </div>
    <!-- ./Table Header -->

    <!-- Table Body -->
    <el-table
      v-if="selectedColumns.length > 0"
      ref="tableRef"
      :data="tableData"
      :fit="false"
      :row-key="rowKey"
      :height="height"
      :max-height="maxHeight"
      border
      @selection-change="onSelectionChange"
    >
      <el-table-column
        v-if="selectable"
        align="center"
        reserve-selection
        type="selection"
        width="40"
        fixed="left"
        :selectable="selectableFunction"
      />
      <el-table-column
        v-for="c in selectedColumns"
        :key="c.key"
        :column-key="c.key"
        :align="c.align"
        :fixed="c.fixed ? c.fixed : false"
        :label="c.label"
        :width="c.width"
        :min-width="c.minWidth || c.width"
        :sortable="c.sortable"
        :index="c.index"
        :resizable="c.resizable === undefined ? true : c.resizable"
        :class-name="c.className || c.key"
      >
        <template #header="scope">
          <cl-table-header :column="c" :index="scope.$index" @change="onHeaderChange"/>
        </template>
        <template #default="scope">
          <cl-table-cell :column="c" :row="scope.row" :row-index="scope.$index"/>
        </template>
      </el-table-column>
    </el-table>
    <!-- ./Table Body-->

    <!-- Table Footer-->
    <div v-if="!hideFooter" class="table-footer">
      <cl-table-actions
        :selection="internalSelection"
        :visible-buttons="visibleButtons"
        @delete="onDelete"
        @edit="onEdit"
        @export="onExport"
        @customize-columns="onShowColumnsTransfer"
      >
        <template #prefix>
          <slot name="actions-prefix"></slot>
        </template>
        <template #suffix>
          <slot name="actions-suffix"></slot>
        </template>
      </cl-table-actions>
      <el-pagination
        v-if="[TABLE_PAGINATION_POSITION_ALL, TABLE_PAGINATION_POSITION_BOTTOM].includes(paginationPosition)"
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        class="pagination"
        :layout="paginationLayout"
        @current-change="onCurrentChange"
        @size-change="onSizeChange"
      />
    </div>
    <!-- ./Table Footer-->

    <!-- Table Columns Transfer -->
    <cl-table-columns-transfer
      :columns="columns"
      :selected-column-keys="internalSelectedColumnKeys"
      :visible="columnsTransferVisible"
      @confirm="onColumnsChange"
      @close="onHideColumnsTransfer"
    />
    <!-- ./Table Columns Transfer -->
  </div>
</template>

<script lang="ts">
import {defineComponent, inject, PropType, ref, SetupContext} from 'vue';
import useColumn from '@/components/table/column';
import useHeader from '@/components/table/header';
import useData from '@/components/table/data';
import useAction from '@/components/table/action';
import usePagination from '@/components/table/pagination';
import {
  TABLE_PAGINATION_POSITION_ALL,
  TABLE_PAGINATION_POSITION_BOTTOM,
  TABLE_PAGINATION_POSITION_TOP
} from '@/constants/table';

export default defineComponent({
  name: 'Table',
  props: {
    data: {
      type: Array as PropType<TableData>,
      required: true,
      default: () => {
        return [];
      },
    },
    columns: {
      type: Array as PropType<TableColumn[]>,
      required: true,
      default: () => {
        return [];
      },
    },
    selectedColumnKeys: {
      type: Array as PropType<string[]>,
      required: false,
      default: () => {
        return [];
      },
    },
    total: {
      type: Number,
      default: 0,
    },
    page: {
      type: Number,
      default: 1,
    },
    pageSize: {
      type: Number,
      default: 10,
    },
    rowKey: {
      type: String,
      default: '_id',
    },
    selectable: {
      type: Boolean,
      default: false,
    },
    visibleButtons: {
      type: Array as PropType<BuiltInTableActionButtonName[]>,
      required: false,
      default: () => {
        return [];
      }
    },
    hideFooter: {
      type: Boolean,
      default: false,
    },
    selectableFunction: {
      type: Function as PropType<TableSelectableFunction>,
      default: () => true,
    },
    paginationLayout: {
      type: String,
      default: 'total, sizes, prev, pager, next',
    },
    loading: {
      type: Boolean,
      default: false,
    },
    paginationPosition: {
      type: String as PropType<TablePaginationPosition>,
      default: TABLE_PAGINATION_POSITION_BOTTOM,
    },
    height: {
      type: [String, Number],
    },
    maxHeight: {
      type: [String, Number],
    },
    embedded: {
      type: Boolean,
      default: false,
    },
  },
  emits: [
    'edit',
    'delete',
    'export',
    'header-change',
    'pagination-change',
    'selection-change',
  ],
  setup(props: TableProps, ctx: SetupContext) {
    const tableWrapperRef = ref();
    const tableRef = ref();

    const {
      tableData,
    } = useData(props, ctx);

    const {
      internalSelectedColumnKeys,
      columnsTransferVisible,
      selectedColumns,
      onShowColumnsTransfer,
      onHideColumnsTransfer,
      onColumnsChange,
    } = useColumn(props, ctx, tableRef, tableWrapperRef);

    const {
      onHeaderChange,
    } = useHeader(props, ctx);

    // inject action functions
    const actionFunctions = inject<ListLayoutActionFunctions>('action-functions');

    const {
      selection: internalSelection,
      onSelectionChange,
      onAdd,
      onEdit,
      onDelete,
      onExport,
      clearSelection,
    } = useAction(props, ctx, tableRef, actionFunctions as ListLayoutActionFunctions);

    const {
      onCurrentChange,
      onSizeChange,
    } = usePagination(props, ctx);

    return {
      tableWrapperRef,
      tableRef,
      tableData,
      internalSelectedColumnKeys,
      columnsTransferVisible,
      selectedColumns,
      onHeaderChange,
      onShowColumnsTransfer,
      onHideColumnsTransfer,
      onColumnsChange,
      onExport,
      internalSelection,
      onSelectionChange,
      onAdd,
      onEdit,
      onDelete,
      clearSelection,
      onCurrentChange,
      onSizeChange,
      TABLE_PAGINATION_POSITION_ALL,
      TABLE_PAGINATION_POSITION_BOTTOM,
      TABLE_PAGINATION_POSITION_TOP,
    };
  },
});
</script>

<style lang="scss" scoped>
.table {
  background-color: var(--cl-container-white-bg);

  .el-table {
    width: 100%;
  }

  .table-header {
    width: 100%;
    text-align: right;
  }

  .table-footer {
    display: flex;
    justify-content: space-between;
    padding: 10px;

    .pagination {
      text-align: right;
    }
  }
}
</style>
<style scoped>
.el-table >>> th > .cell {
  line-height: 1.5;
  word-break: normal;
}

.table.embedded >>> .el-table--border .el-table__inner-wrapper:after {
  height: 0;
}

.table.embedded >>> .el-table__border-left-patch,
.table.embedded >>> .el-table--border:before {
  width: 0;
}

.table.embedded >>> .el-table--border .el-table__inner-wrapper tr:first-child td:first-child,
.table.embedded >>> .el-table.is-scrolling-left.el-table--border tr:first-child td:first-child,
.table.embedded >>> .el-table--border .el-table__inner-wrapper tr:first-child th:first-child {
  border-left: none;
}
</style>
