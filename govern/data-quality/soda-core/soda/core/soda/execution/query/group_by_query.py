from soda.execution.query.query import Query


class GroupByQuery(Query):
    def __init__(
        self,
        data_source_scan: "DataSourceScan",
        metric: "GroupByMetric",
        partition: "Partition",
        location: "Location",
        group_limit: int = 1000,
    ):
        super().__init__(
            data_source_scan=data_source_scan,
            unqualified_query_name=f"group_by[{metric.name}]",
            location=location,
            sql=metric.query,
            partition=partition,
        )
        self.group_limit = group_limit
        self.metric = metric

    def execute(self):
        self.fetchall()

        results = []
        columns = [column[0] for column in self.description]
        for row in self.rows:
            results.append(dict(zip(columns, row)))
        self.metric.set_value(results)
