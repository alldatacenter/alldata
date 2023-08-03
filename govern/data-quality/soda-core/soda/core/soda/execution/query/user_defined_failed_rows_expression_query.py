from __future__ import annotations

from soda.execution.query.query import Query


class UserDefinedFailedRowsExpressionQuery(Query):
    def __init__(
        self,
        data_source_scan: DataSourceScan,
        check_name: str,
        sql: str,
        partition: Partition,
        samples_limit: int | None = None,
        metric: Metric = None,
    ):
        super().__init__(
            data_source_scan=data_source_scan,
            sql=sql,
            unqualified_query_name=f"user_defined_failed_rows_expression_query[{check_name}]",
            partition=partition,
        )
        self.samples_limit = samples_limit
        self.metric = metric

    def execute(self):
        self.store()
