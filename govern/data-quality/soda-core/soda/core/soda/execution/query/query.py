from __future__ import annotations

from datetime import datetime, timedelta

from soda.common.exception_helper import get_exception_stacktrace
from soda.common.query_helper import parse_columns_from_query
from soda.common.undefined_instance import undefined
from soda.sampler.db_sample import DbSample
from soda.sampler.sample_context import SampleContext
from soda.sampler.sampler import Sampler


class Query:
    def __init__(
        self,
        data_source_scan: DataSourceScan,
        table: Table = None,
        partition: Partition = None,
        column: Column = None,
        unqualified_query_name: str = None,
        sql: str | None = None,
        sample_name: str = "failed_rows",
        location: Location | None = None,
        samples_limit: int | None = None,
    ):
        self.logs = data_source_scan.scan._logs
        self.data_source_scan = data_source_scan
        self.query_name: str = Query.build_query_name(
            data_source_scan, table, partition, column, unqualified_query_name
        )
        self.sample_name = sample_name
        self.table: Table | None = table
        self.partition: Partition | None = partition
        self.column: Column | None = column
        self.location: Location | None = location
        self.samples_limit: int | None = samples_limit

        # The SQL query that is used _fetchone or _fetchall or _store
        # This field can also be initialized in the execute method before any of _fetchone,
        # _fetchall or _store are called
        self.sql: str = data_source_scan.scan.jinja_resolve(sql)

        self.passing_sql: str | None = None
        self.failing_sql: str | None = None

        # Following fields are initialized in execute method
        self.description: tuple | None = None
        self.row: tuple | None = None
        self.rows: list[tuple] | None = None
        self.sample_ref: SampleRef | None = None
        self.exception: BaseException | None = None
        self.duration: timedelta | None = None

    def get_cloud_dicts(self) -> list(dict(str, any)):
        dicts = [self.get_dict()]

        if self.failing_sql:
            dicts.append(self.get_dict("failing_sql", self.failing_sql))

        if self.passing_sql:
            dicts.append(self.get_dict("passing_sql", self.passing_sql))

        return dicts

    def get_dict(self, name_suffix: str | None = None, sql: str | None = None) -> dict:
        from soda.execution.column import Column
        from soda.execution.partition import Partition

        name = self.query_name

        if name_suffix:
            name += f".{name_suffix}"
        return {
            "name": name,
            "dataSource": self.data_source_scan.data_source.data_source_name,
            "table": Partition.get_table_name(self.partition),
            "partition": Partition.get_partition_name(self.partition),
            "column": Column.get_partition_name(self.column),
            "sql": sql or self.sql,
            "exception": None if name_suffix or sql else get_exception_stacktrace(self.exception),
            "duration": None if name_suffix or sql else self.duration,
        }

    @staticmethod
    def build_query_name(data_source_scan, table, partition, column, unqualified_query_name):
        full_query_pieces = [data_source_scan.data_source.data_source_name]
        if partition is not None and partition.partition_name is not None:
            full_query_pieces.append(f"{partition.table.table_name}[{partition.partition_name}]")
        elif table is not None:
            full_query_pieces.append(f"{table.table_name}")
        if column is not None:
            full_query_pieces.append(f"{column.column_name}")
        full_query_pieces.append(unqualified_query_name)
        return ".".join(full_query_pieces)

    def execute(self):
        """
        Execute method implementations should
          - invoke either self.fetchone, self.fetchall or self.store
          - update the metrics with value and optionally other diagnostic information
        """
        # TODO: some of the subclasses couple setting metric with storing the sample - refactor that.
        self.fetchall()

    def fetchone(self):
        """
        DataSource query execution exceptions will be caught and result in the
        self.exception being populated.
        """
        self.__append_to_scan()
        start = datetime.now()
        data_source = self.data_source_scan.data_source
        try:
            cursor = data_source.connection.cursor()
            try:
                self.logs.debug(f"Query {self.query_name}:\n{self.sql}")
                cursor.execute(self.sql)
                self.row = cursor.fetchone()
                self.description = cursor.description
            finally:
                cursor.close()
        except BaseException as e:
            self.exception = e
            self.logs.error(
                message=f"Query execution error in {self.query_name}: {e}\n{self.sql}",
                exception=e,
                location=self.location,
            )
            data_source.query_failed(e)
        finally:
            self.duration = datetime.now() - start

    def fetchall(self):
        """
        DataSource query execution exceptions will be caught and result in the
        self.exception being populated.
        """
        self.__append_to_scan()
        start = datetime.now()
        data_source = self.data_source_scan.data_source
        try:
            cursor = data_source.connection.cursor()
            try:
                self.logs.debug(f"Query {self.query_name}:\n{self.sql}")
                cursor.execute(self.sql)
                self.rows = cursor.fetchall()
                self.description = cursor.description
            finally:
                cursor.close()
        except BaseException as e:
            self.exception = e
            self.logs.error(f"Query error: {self.query_name}: {e}\n{self.sql}", exception=e, location=self.location)
            data_source.query_failed(e)
        finally:
            self.duration = datetime.now() - start

    def store(self):
        """
        DataSource query execution exceptions will be caught and result in the
        self.exception being populated.
        """
        self.__append_to_scan()
        sampler: Sampler = self.data_source_scan.scan._configuration.sampler
        start = datetime.now()
        data_source = self.data_source_scan.data_source
        try:
            cursor = data_source.connection.cursor()
            try:
                # Check if query does not contain forbidden columns and only create sample if it does not.
                # Query still needs to execute in case this is a query that also sets a metric value. (e.g. reference check)
                allow_samples = True
                offending_columns = []

                if self.partition and self.partition.table:
                    query_columns = parse_columns_from_query(self.sql)

                    for column in query_columns:
                        if self.data_source_scan.data_source.is_column_excluded(
                            self.partition.table.table_name, column
                        ):
                            allow_samples = False
                            offending_columns.append(column)

                # A bit of a hacky workaround for queries that also set the metric in one go.
                # TODO: revisit after decoupling getting metric values and storing samples. This can be dangerous, it sets the metric value
                # only when metric value is not set, but this could cause weird regressions.
                set_metric = False
                if hasattr(self, "metric") and self.metric and self.metric.value == undefined:
                    set_metric = True

                if set_metric or allow_samples:
                    self.logs.debug(f"Query {self.query_name}:\n{self.sql}")
                    cursor.execute(str(self.sql))
                    self.description = cursor.description
                    db_sample = DbSample(cursor, self.data_source_scan.data_source)

                if set_metric:
                    self.metric.set_value(len(db_sample.get_rows()))

                if allow_samples:
                    # TODO Hacky way to get the check name, check name isn't there when dataset samples are taken
                    check_name = next(iter(self.metric.checks)).name if hasattr(self, "metric") else None
                    sample_context = SampleContext(
                        sample=db_sample,
                        sample_name=self.sample_name,
                        query=self.sql,
                        data_source=self.data_source_scan.data_source,
                        partition=self.partition,
                        column=self.column,
                        scan=self.data_source_scan.scan,
                        logs=self.data_source_scan.scan._logs,
                        samples_limit=self.samples_limit,
                        passing_sql=self.passing_sql,
                        check_name=check_name,
                    )

                    self.sample_ref = sampler.store_sample(sample_context)
                else:
                    self.logs.info(
                        f"Skipping samples from query '{self.query_name}'. Excluded column(s) present: {offending_columns}."
                    )
            finally:
                cursor.close()
        except BaseException as e:
            self.exception = e
            self.logs.error(f"Query error: {self.query_name}: {e}\n{self.sql}", exception=e, location=self.location)
            data_source.query_failed(e)
        finally:
            self.duration = datetime.now() - start

    def __append_to_scan(self):
        scan = self.data_source_scan.scan
        self.index = len(scan._queries)
        scan._queries.append(self)
