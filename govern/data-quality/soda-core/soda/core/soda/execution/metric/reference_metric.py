from __future__ import annotations

from soda.execution.metric.query_metric import QueryMetric
from soda.sampler.sample_ref import SampleRef


class ReferenceMetric(QueryMetric):
    def __init__(
        self,
        data_source_scan: DataSourceScan,
        check: ReferenceCheck,
        partition: Partition,
        single_source_column: Column,
    ):
        super().__init__(
            data_source_scan=data_source_scan,
            partition=partition,
            column=single_source_column,
            name="reference",
            check=check,
            identity_parts=[
                check.check_cfg.source_column_names,
                check.check_cfg.target_table_name,
                check.check_cfg.target_column_names,
            ],
        )
        self.check = check
        self.invalid_references_sample_ref: Optional[SampleRef] = None

    def __str__(self):
        return f'"{self.name}"'

    def ensure_query(self):
        from soda.execution.query.reference_query import ReferenceQuery

        reference_query = ReferenceQuery(
            data_source_scan=self.data_source_scan,
            metric=self,
            samples_limit=self.samples_limit,
            partition=self.partition,
        )
        self.data_source_scan.queries.append(reference_query)
        self.queries.append(reference_query)
