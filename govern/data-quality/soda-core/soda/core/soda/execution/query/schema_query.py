from soda.execution.query.query import Query


class TableColumnsQuery(Query):
    def __init__(self, partition: "Partition", schema_metric: "SchemaMetric"):
        super().__init__(
            data_source_scan=partition.data_source_scan,
            table=partition.table,
            partition=partition,
            unqualified_query_name=f"schema[{partition.table.table_name}]",
        )
        self.metric = schema_metric

    def execute(self):
        self._initialize_column_rows()
        self._propagate_column_rows_to_metric_value()

    def _initialize_column_rows(self):
        """
        Initializes member self.rows as a list (or tuple) of rows where each row representing a column description.
        A column description is a list (or tuple) of column name on index 0 and column data type (str) on index 1
        Eg [["col_name_one", "data_type_of_col_name_one"], ...]
        """
        data_source = self.data_source_scan.data_source
        self.sql = data_source.sql_get_table_columns(self.table.table_name)
        self.sql = self.data_source_scan.scan.jinja_resolve(self.sql)

        self.fetchall()

    def _propagate_column_rows_to_metric_value(self):
        """
        Propagates self.rows to the metric value being a dict with name and type as keys
        """
        if len(self.rows) > 0:
            measured_schema = [{"name": row[0], "type": row[1]} for row in self.rows]
            self.metric.set_value(measured_schema)
