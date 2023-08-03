from typing import List

from soda.execution.query.query import Query


class AggregationQuery(Query):
    def __init__(self, partition: "Partition", aggregation_query_index: int):
        super().__init__(
            data_source_scan=partition.data_source_scan,
            table=partition.table,
            partition=partition,
            unqualified_query_name=f"aggregation[{aggregation_query_index}]",
        )
        from soda.execution.metric.query_metric import QueryMetric

        self.select_expressions: List[str] = []
        self.metrics: List[QueryMetric] = []

    def add_metric(self, sql_expression: str, metric: "Metric"):
        self.select_expressions.append(sql_expression)
        self.metrics.append(metric)

    def execute(self):
        scan = self.data_source_scan.scan
        select_expression_sql = f",\n  ".join(self.select_expressions)
        self.sql = f"SELECT \n" f"  {select_expression_sql} \n" f"FROM {self.partition.table.qualified_table_name}"

        partition_filter = self.partition.sql_partition_filter
        if partition_filter:
            resolved_filter = scan.jinja_resolve(definition=partition_filter)
            self.sql += f"\nWHERE {resolved_filter}"
        self.sql = self.data_source_scan.scan.jinja_resolve(self.sql)
        self.fetchone()
        if self.row:
            for i in range(0, len(self.row)):
                metric = self.metrics[i]
                fetched_value = self.row[i]
                metric.set_value(fetched_value)

                sample_query = metric.create_failed_rows_sample_query()
                if sample_query:
                    metric.queries.append(sample_query)
                    sample_query.execute()
