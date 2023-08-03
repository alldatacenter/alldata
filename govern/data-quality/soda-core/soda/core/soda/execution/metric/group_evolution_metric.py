from soda.execution.metric.query_metric import QueryMetric
from soda.execution.query.group_evolution_query import GroupEvolutionQuery


class GroupEvolutionMetric(QueryMetric):
    def __init__(self, data_source_scan: "DataSourceScan", partition: "Partition", check: "Check", query: str):
        super().__init__(
            data_source_scan=data_source_scan,
            partition=partition,
            column=None,
            name="group evolution",
            check=check,
            identity_parts=[],
        )
        self.query = query

    def ensure_query(self):
        check = next(iter(self.checks), None)
        location = check.check_cfg.location if check else None
        self.data_source_scan.queries.append(
            GroupEvolutionQuery(
                data_source_scan=self.data_source_scan,
                metric=self,
                location=location,
                partition=self.partition,
            )
        )

    def get_cloud_dict(self):
        from soda.execution.column import Column
        from soda.execution.partition import Partition

        return {
            "identity": self.identity,
            "metricName": self.name,
            "dataSourceName": self.data_source_scan.data_source.data_source_name,
            "tableName": Partition.get_table_name(self.partition),
            "partitionName": Partition.get_partition_name(self.partition),
            "columnName": Column.get_partition_name(self.column),
            "value": self.value,
        }

    def get_dict(self):
        from soda.execution.column import Column
        from soda.execution.partition import Partition

        return {
            "identity": self.identity,
            "metricName": self.name,
            "dataSourceName": self.data_source_scan.data_source.data_source_name,
            "tableName": Partition.get_table_name(self.partition),
            "partitionName": Partition.get_partition_name(self.partition),
            "columnName": Column.get_partition_name(self.column),
            "value": self.value,
        }
