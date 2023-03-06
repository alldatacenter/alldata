from __future__ import annotations

from soda.execution.metric.numeric_query_metric import NumericQueryMetric
from soda.execution.query.aggregation_query import AggregationQuery
from soda.execution.query.duplicates_query import DuplicatesQuery
from soda.execution.query.query import Query
from soda.execution.query.schema_query import TableColumnsQuery
from soda.sodacl.partition_cfg import PartitionCfg


class Partition:
    def __init__(self, table: Table, partition_name: str):
        from soda.execution.metric.column_metrics import ColumnMetrics
        from soda.execution.metric.metric import Metric
        from soda.execution.table import Table
        from soda.sodacl.partition_cfg import PartitionCfg

        self.logs = table.data_source_scan.scan._logs
        self.table: Table = table
        self.partition_name = partition_name
        self.partition_cfg: PartitionCfg = None
        self.sql_partition_filter: str | None = None
        self.data_source_scan = table.data_source_scan
        self.metrics: list[Metric] = []
        self.column_metrics: dict[str, ColumnMetrics] = {}

        self.schema_query: TableColumnsQuery | None = None
        self.aggregation_queries: list[AggregationQuery] = []
        self.duplicate_queries: list[DuplicatesQuery] = []

    def set_partition_cfg(self, partition_cfg: PartitionCfg):
        self.partition_cfg = partition_cfg
        self.sql_partition_filter = partition_cfg.sql_partition_filter

    def add_check_cfg(self, check_cfg: CheckCfg):
        if self.partition_cfg is None:
            self.partition_cfg = PartitionCfg(self.partition_name)
        self.partition_cfg.add_check_cfg(check_cfg)

    def get_or_create_column_metrics(self, column_name: str):
        from soda.execution.metric.column_metrics import ColumnMetrics

        column_metrics = self.column_metrics.get(column_name)
        if not column_metrics:
            column_metrics = ColumnMetrics(self, column_name)
            self.column_metrics[column_name] = column_metrics
        return column_metrics

    def ensure_query_for_metric(self, metric: Metric):
        if isinstance(metric, NumericQueryMetric):
            if metric.name == "duplicate_count":
                duplicates_query = DuplicatesQuery(self, metric)
                self.duplicate_queries.append(duplicates_query)
                metric.queries.append(duplicates_query)
            else:
                sql_aggregation_expression = metric.get_sql_aggregation_expression()
                if sql_aggregation_expression:
                    max_aggregation_fields = self.data_source_scan.data_source.get_max_aggregation_fields()
                    if (
                        len(self.aggregation_queries) == 0
                        or len(self.aggregation_queries[-1].metrics) >= max_aggregation_fields
                    ):
                        aggregation_query_index = len(self.aggregation_queries)
                        aggregation_query = AggregationQuery(self, aggregation_query_index)
                        self.aggregation_queries.append(aggregation_query)
                    else:
                        aggregation_query = self.aggregation_queries[-1]
                    aggregation_query.add_metric(sql_aggregation_expression, metric)
                    metric.queries.append(aggregation_query)
                else:
                    self.logs.error(f"Unsupported metric {metric.name}")
        else:
            self.logs.error(f"Unsupported metric {metric.name} ({type(metric).__name__})")

    def collect_queries(self) -> list[Query]:
        queries: list[Query] = []
        if self.schema_query:
            queries.append(self.schema_query)
        queries.extend(self.aggregation_queries)
        queries.extend(self.duplicate_queries)
        return queries

    @classmethod
    def get_partition_name(cls, partition):
        return partition.partition_name if isinstance(partition, Partition) else None

    @classmethod
    def get_table_name(cls, partition):
        return partition.table.table_name if isinstance(partition, Partition) else None
