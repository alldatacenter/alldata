from __future__ import annotations

from soda.execution.query.query import Query


class UserDefinedFailedRowsQuery(Query):
    def __init__(
        self,
        data_source_scan: DataSourceScan,
        metric: FailedRowsQueryMetric,
        partition: Partition,
        location: Location | None = None,
        samples_limit: int | None = None,
    ):
        super().__init__(
            data_source_scan=data_source_scan,
            unqualified_query_name=f"failed_rows[{metric.name}]",
            location=location,
            samples_limit=samples_limit,
            sql=metric.query,
            partition=partition,
        )
        self.metric = metric

    def execute(self):
        self.store()
        if self.sample_ref:
            self.metric.set_value(self.sample_ref.total_row_count)
            if self.sample_ref.is_persisted():
                self.metric.failed_rows_sample_ref = self.sample_ref
