import {Ref, VNode} from 'vue';
import {AnyObject, Store, StoreMutations, TableColumnCtx} from 'element-plus/lib/components/table/src/table/defaults';
import {
  TABLE_ACTION_CUSTOMIZE_COLUMNS,
  TABLE_ACTION_EXPORT, TABLE_PAGINATION_POSITION_ALL,
  TABLE_PAGINATION_POSITION_BOTTOM,
  TABLE_PAGINATION_POSITION_TOP,
} from '@/constants/table';
import {FilterMethods, Filters} from 'element-plus/lib/components/table/src/table-column/defaults';

export declare global {
  interface TableProps {
    data: TableData;
    columns: TableColumn[];
    selectedColumnKeys: string[];
    total: number;
    page: number;
    pageSize: number;
    rowKey: string;
    selectable: boolean;
    visibleButtons: BuiltInTableActionButtonName[];
    hideFooter: boolean;
    selectableFunction: TableSelectableFunction;
    paginationLayout: string;
    loading: boolean;
    paginationPosition: TablePaginationPosition;
    height?: string | number;
    maxHeight?: string | number;
    embedded?: boolean;
  }

  interface TableColumn<T = any> {
    key: string;
    label: string;
    icon?: string | string[];
    width?: number | string;
    minWidth?: number | string;
    index?: number;
    align?: string;
    sortable?: boolean;
    fixed?: string | boolean;
    rowKey?: string;
    buttons?: TableColumnButton[] | TableColumnButtonsFunction;
    value?: TableValueFunction<T> | any;
    disableTransfer?: boolean;
    defaultHidden?: boolean;
    hasSort?: boolean;
    hasFilter?: boolean;
    filterItems?: SelectOption[];
    allowFilterSearch?: boolean;
    allowFilterItems?: boolean;
    required?: boolean;
    className?: string;
  }

  type TableColumns<T = any> = TableColumn<T>[];

  interface TableAnyRowData {
    [key: string]: any;
  }

  type TableData<T = TableAnyRowData> = T[];

  interface TableDataWithTotal<T = TableAnyRowData> {
    data: TableData<T>;
    total: number;
  }

  interface TableColumnsMap<T = any> {
    [key: string]: TableColumn<T>;
  }

  interface TableColumnCtx<T = any> {
    id: string;
    realWidth: number;
    type: string;
    label: string;
    className: string;
    labelClassName: string;
    property: string;
    prop: string;
    width: string | number;
    minWidth: string | number;
    renderHeader: (data: CI<T>) => VNode;
    sortable: boolean | string;
    sortMethod: (a: T, b: T) => number;
    sortBy: string | ((row: T, index: number) => string) | string[];
    resizable: boolean;
    columnKey: string;
    rawColumnKey: string;
    align: string;
    headerAlign: string;
    showTooltipWhenOverflow: boolean;
    showOverflowTooltip: boolean;
    fixed: boolean | string;
    formatter: (row: T, column: TableColumnCtx<T>, cellValue: any, index: number) => VNode;
    selectable: (row: T, index: number) => boolean;
    reserveSelection: boolean;
    filterMethod: FilterMethods<T>;
    filteredValue: string[];
    filters: Filters;
    filterPlacement: string;
    filterMultiple: boolean;
    index: number | ((index: number) => number);
    sortOrders: ('ascending' | 'descending' | null)[];
    renderCell: (data: any) => void;
    colSpan: number;
    rowSpan: number;
    children: TableColumnCtx<T>[];
    level: number;
    filterable: boolean | FilterMethods<T> | Filters;
    order: string;
    isColumnGroup: boolean;
    columns: TableColumnCtx<T>[];
    getColumnIndex: () => number;
    no: number;
    filterOpened?: boolean;
  }

  interface TableColumnCtxMap {
    [key: string]: TableColumnCtx;
  }

  interface TableColumnButton {
    type?: string;
    size?: string;
    icon?: Icon | TableValueFunction;
    tooltip?: string | TableButtonTooltipFunction;
    isHtml?: boolean;
    disabled?: TableButtonDisabledFunction;
    onClick?: TableButtonOnClickFunction;
    id?: string;
    className?: string;
    action?: GenericAction;
  }

  type TableColumnButtonsFunction<T = any> = (row?: T) => TableColumnButton[];

  type TableValueFunction<T = any> = (row: T, rowIndex?: number, column?: TableColumn<T>) => VNode;
  type TableButtonOnClickFunction<T = any> = (row: T, rowIndex?: number, column?: TableColumn<T>) => void;
  type TableButtonTooltipFunction<T = any> = (row: T, rowIndex?: number, column?: TableColumn<T>) => string;
  type TableButtonDisabledFunction<T = any> = (row: T, rowIndex?: number, column?: TableColumn<T>) => boolean;
  type TableFilterItemsFunction<T = any> = (filter?: TableHeaderDialogFilterData, column?: TableColumn<T>) => SelectOption[];
  type TableSelectableFunction<T = any> = (row: T, rowIndex?: number) => boolean;

  interface TableStore extends Store {
    mutations: TableStoreMutations;
    commit: (mutation: string, payload: any) => void;
    updateColumns: () => void;
  }

  interface TableStoreMutations extends StoreMutations {
    setColumns: (states: TableStoreStates, columns: TableColumnCtx[]) => void;
  }

  interface TableStoreStates {
    _data: Ref<AnyObject[]>;
    _columns: Ref<TableColumnCtx[]>;
  }

  interface TablePagination {
    page: number;
    size: number;
  }

  type TableActionName =
    ActionName |
    TABLE_ACTION_EXPORT |
    TABLE_ACTION_CUSTOMIZE_COLUMNS;

  type TablePaginationPosition =
    TABLE_PAGINATION_POSITION_TOP |
    TABLE_PAGINATION_POSITION_BOTTOM |
    TABLE_PAGINATION_POSITION_ALL;
}
