from __future__ import annotations

from soda.execution.query.query import Query


class SampleQuery(Query):
    def __init__(self, data_source_scan: DataSourceScan, metric: Metric, sample_type: str, sql: str):
        """
        :param sample_type: Eg 'failed_rows'
        """
        super().__init__(
            data_source_scan=data_source_scan,
            partition=metric.partition,
            column=metric.column,
            unqualified_query_name=f"{sample_type}[{metric.name}]",
            sql=sql,
            samples_limit=metric.samples_limit,
        )
        self.metric = metric

    def execute(self):
        self.store()
        self.metric.failed_rows_sample_ref = self.sample_ref
