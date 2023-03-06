from soda.execution.metric.query_metric import QueryMetric


class SchemaMetric(QueryMetric):
    def __init__(
        self,
        data_source_scan: "DataSourceScan",
        partition: "Partition",
        check: "Check",
    ):
        super().__init__(
            data_source_scan=data_source_scan,
            partition=partition,
            column=None,
            name="schema",
            check=check,
            identity_parts=[],
        )

    def ensure_query(self):
        self.schema_query = self.data_source_scan.data_source.create_table_columns_query(self.partition, self)
        self.queries.append(self.schema_query)
        self.data_source_scan.queries.append(self.schema_query)

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
            "value": [{"columnName": c["name"], "sourceDataType": c["type"]} for c in self.value],
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
            "value": [{"columnName": c["name"], "sourceDataType": c["type"]} for c in self.value],
        }
