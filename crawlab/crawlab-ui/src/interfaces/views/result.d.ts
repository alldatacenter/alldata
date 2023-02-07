interface ResultListProps {
  id: string;
  dataSourceId?: string;
  noActions?: boolean;
  embedded?: boolean;
  visibleButtons?: BuiltInTableActionButtonName[];
  filter?: FilterConditionData[] | (() => FilterConditionData[]);
  displayAllFields: boolean;
}
