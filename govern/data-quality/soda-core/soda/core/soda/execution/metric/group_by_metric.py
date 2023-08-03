from soda.execution.metric.query_metric import QueryMetric
from soda.execution.query.group_by_query import GroupByQuery


class GroupByMetric(QueryMetric):
    def __init__(self, data_source_scan: "DataSourceScan", partition: "Partition", check: "Check", query: str):
        super().__init__(
            data_source_scan=data_source_scan,
            partition=partition,
            column=None,
            name="group by",
            check=check,
            identity_parts=[query],
        )
        self.query = query

    def ensure_query(self):
        check = next(iter(self.checks), None)
        location = check.check_cfg.location if check else None
        self.data_source_scan.queries.append(
            GroupByQuery(
                data_source_scan=self.data_source_scan,
                metric=self,
                location=location,
                partition=self.partition,
            )
        )
