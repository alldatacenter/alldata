from __future__ import annotations

from soda.execution.query.query import Query


class GroupEvolutionQuery(Query):
    def __init__(
        self,
        data_source_scan: DataSourceScan,
        metric: GroupEvolutionMetric,
        partition: Partition,
        location: Location | None = None,
        samples_limit: int | None = None,
    ):
        super().__init__(
            data_source_scan=data_source_scan,
            unqualified_query_name=f"group_evolution[{metric.name}]",
            location=location,
            samples_limit=samples_limit,
            sql=metric.query,
            partition=partition,
        )
        self.metric = metric

    def execute(self):
        self.fetchall()
        # Only single value per group is supported and we skip nulls
        self.metric.set_value([r[0] for r in self.rows if r[0] is not None])
