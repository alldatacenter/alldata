type DataCollectionStoreModule = BaseModule<DataCollectionStoreState, DataCollectionStoreGetters, DataCollectionStoreMutations, DataCollectionStoreActions>;

interface DataCollectionStoreState extends BaseStoreState<DataCollection> {
  resultTableData: TableData<Result>;
  resultTableTotal: number;
  resultTablePagination: TablePagination;
  resultTableFilter: FilterConditionData[];
  resultDialogVisible: boolean;
  resultDialogContent: any;
  resultDialogType?: DataFieldType;
  resultDialogKey: string;
  dedupFieldsDialogVisible: boolean;
}

interface DataCollectionStoreGetters extends BaseStoreGetters<DataCollectionStoreState> {
  resultFields: StoreGetter<DataCollectionStoreState, DataField[]>;
}

interface DataCollectionStoreMutations extends BaseStoreMutations<DataCollection> {
  setResultTableData: StoreMutation<DataCollectionStoreState, TableDataWithTotal<Result>>;
  resetResultTableData: StoreMutation<DataCollectionStoreState>;
  setResultTablePagination: StoreMutation<DataCollectionStoreState, TablePagination>;
  resetResultTablePagination: StoreMutation<DataCollectionStoreState>;
  setResultTableFilter: StoreMutation<DataCollectionStoreState, FilterConditionData[]>;
  resetResultTableFilter: StoreMutation<DataCollectionStoreState>;
  setResultDialogVisible: StoreMutation<DataCollectionStoreState, boolean>;
  setResultDialogContent: StoreMutation<DataCollectionStoreState, any>;
  resetResultDialogContent: StoreMutation<DataCollectionStoreState>;
  setResultDialogType: StoreMutation<DataCollectionStoreState, DataFieldType>;
  resetResultDialogType: StoreMutation<DataCollectionStoreState>;
  setResultDialogKey: StoreMutation<DataCollectionStoreState, string>;
  resetResultDialogKey: StoreMutation<DataCollectionStoreState>;
  setDedupFieldsDialogVisible: StoreMutation<DataCollectionStoreState, boolean>;
}

interface DataCollectionStoreActions extends BaseStoreActions<DataCollection> {
  getResultData: StoreAction<DataCollectionStoreState, { string; ListRequestParams }>;
}
