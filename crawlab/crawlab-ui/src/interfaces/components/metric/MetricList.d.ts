interface MetricListProps {
  activeMetricSnapshotKey?: string;
  metricSnapshots?: MetricSnapshot[];
  metrics?: NavItem[];
  metricDataFunc?: MetricListDataFunc;
  metricTitleFunc?: MetricListTitleFunc;
  dateRange?: RangeItem;
  duration?: string;
  durationOptions?: SelectOption[];
  defaultCheckedAll?: boolean;
}
