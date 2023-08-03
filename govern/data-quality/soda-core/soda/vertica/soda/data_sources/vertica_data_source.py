from __future__ import annotations

import vertica_python
from soda.common.exceptions import DataSourceConnectionError
from soda.common.logs import Logs
from soda.execution.data_source import DataSource
from soda.execution.data_type import DataType


class VerticaDataSource(DataSource):
    TYPE: str = "vertica"

    SCHEMA_CHECK_TYPES_MAPPING: dict = {
        "Varchar": ["varchar"],
        "Char": ["char"],
        "Long Varchar": ["text"],
        "Boolean": ["boolean", "bool"],
        "Binary": ["binary", "bin"],
        "Varbinary": ["varbinary", "varbin"],
        "Long Varbinary": ["long varbinary", "long varbin"],
        "Date": ["date"],
        "Time": ["time"],
        "Timestamp": ["timestamp"],
        "TimeTz": ["time with tz", "time"],
        "TimestampTz": ["timestamp with tz", "timestamp"],
        "Float": ["float", "float8", "real", "double precision", "double"],
        "Integer": ["integer", "int", "bigint", "int8", "smallint", "tinyint"],
        "Numeric": ["decimal", "numeric", "number", "money"],
    }

    SQL_TYPE_FOR_CREATE_TABLE_MAP: dict = {
        DataType.TEXT: "VARCHAR(255)",
        DataType.INTEGER: "INT",
        DataType.DECIMAL: "DECIMAL",
        DataType.DATE: "DATE",
        DataType.TIME: "TIME",
        DataType.TIMESTAMP: "TIMESTAMP",
        DataType.TIMESTAMP_TZ: "TIMESTAMP WITH TIMEZONE",
        DataType.BOOLEAN: "BOOLEAN",
    }

    SQL_TYPE_FOR_SCHEMA_CHECK_MAP: dict = {
        DataType.TEXT: "Varchar",
        DataType.INTEGER: "Integer",
        DataType.DECIMAL: "Numeric",
        DataType.DATE: "Date",
        DataType.TIME: "Time",
        DataType.TIMESTAMP: "Timestamp",
        DataType.TIMESTAMP_TZ: "TimestampTz",
        DataType.BOOLEAN: "Boolean",
    }

    NUMERIC_TYPES_FOR_PROFILING: list = [
        "float",
        "integer",
        "numeric",
        "int",
    ]

    TEXT_TYPES_FOR_PROFILING: list = ["varchar", "char"]

    def __init__(self, logs: Logs, data_source_name: str, data_source_properties: dict):
        super().__init__(logs, data_source_name, data_source_properties)

        self.host = data_source_properties.get("host", "localhost")
        self.port = data_source_properties.get("port", "5433")
        self.username = data_source_properties.get("username", "dbadmin")
        self.password = data_source_properties.get("password", "password")
        self.database = data_source_properties.get("database", "vmart")  # TODO: find default DB name
        self.schema = data_source_properties.get("schema", "public")

    def connect(self):
        try:
            self.connection = vertica_python.connect(
                user=self.username, password=self.password, host=self.host, port=self.port, database=self.database
            )
            return self.connection
        except Exception as e:
            raise DataSourceConnectionError(self.TYPE, e)

    def safe_connection_data(self):
        return [self.type, self.host, self.port, self.username, self.database]

    @staticmethod
    def column_metadata_columns() -> list:
        return ["column_name", "data_type", "is_nullable"]

    def sql_get_table_names_with_count(
        self, include_tables: list[str] | None = None, exclude_tables: list[str] | None = None
    ) -> str:
        table_filter_expression = self.sql_table_include_exclude_filter(
            "anchor_table_name", "schema_name", include_tables, exclude_tables
        )

        where_clause = f"AND {table_filter_expression}" if table_filter_expression else ""

        sql = f"""
        with

        num_rows as (
            select
                schema_name,
                anchor_table_name as table_name,
                sum(total_row_count) as rows
            from v_monitor.storage_containers sc
                join v_catalog.projections p
                    on sc.projection_id = p.projection_id
                        and p.is_super_projection = true
            where True
                {where_clause}
            group by schema_name  anchor_table_name, sc.projection_id
        )

        select
            table_name,
            max(rows) as row_count
        from num_rows
        group by table_name
        order by rows desc
        ;
        """

        return sql

    @staticmethod
    def column_metadata_catalog_column() -> str:
        pass

    def validate_configuration(self, logs: Logs) -> None:
        pass

    def sql_information_schema_tables(self) -> str:
        return "v_catalog.tables"

    def sql_information_schema_columns(self) -> str:
        return "v_catalog.columns"

    def sql_get_table_columns(
        self,
        table_name: str,
        included_columns: list[str] | None = None,
        excluded_columns: list[str] | None = None,
    ) -> str:
        table_name_default_case = self.default_casify_table_name(table_name)

        unquoted_table_name_default_case = (
            table_name_default_case[1:-1] if self.is_quoted(table_name_default_case) else table_name_default_case
        )

        filter_clauses = [f"table_name = '{unquoted_table_name_default_case}'"]

        if self.schema:
            filter_clauses.append(f"table_schema = '{self.schema}'")

        if included_columns:
            include_clauses = []
            for col in included_columns:
                include_clauses.append(f"column_name like '{col}'")
            include_causes_or = " or ".join(include_clauses)
            filter_clauses.append(f"({include_causes_or})")

        if excluded_columns:
            for col in excluded_columns:
                filter_clauses.append(f"column_name not like '{col}'")

        where_filter = " \n  and ".join(filter_clauses)

        sql = (
            f"select\n"
            f"  columns.column_name\n"
            f"  , types.type_name as data_type\n"
            f"  , columns.is_nullable\n"
            f"from columns\n"
            f"  inner join types\n"
            f"      on columns.data_type_id = types.type_id\n"
            f"where {where_filter}\n"
            f"\norder by {self.get_ordinal_position_name()}\n"
        )

        return sql

    def default_casify_system_name(self, identifier: str) -> str:
        return identifier

    def default_casify_table_name(self, identifier: str) -> str:
        return identifier

    def default_casify_column_name(self, identifier: str) -> str:
        return identifier

    def default_casify_type_name(self, identifier: str) -> str:
        return identifier.lower()

    def get_metric_sql_aggregation_expression(self, metric_name: str, metric_args: list[object] | None, expr: str):
        if metric_name in [
            "stddev",
            "stddev_pop",
            "stddev_samp",
            "variance",
            "var_pop",
            "var_samp",
        ]:
            return f"{metric_name.upper()}({expr})"

        if metric_name == "percentile":
            return f"APPROXIMATE_PERCENTILE ({expr} USING PARAMETERS percentile = {metric_args[1]})"

        return super().get_metric_sql_aggregation_expression(metric_name, metric_args, expr)

    def regex_replace_flags(self) -> str:
        return ""

    @staticmethod
    def column_metadata_table_name() -> str:
        return "table_name"

    @staticmethod
    def column_metadata_schema_name() -> str:
        return "table_schema"

    @staticmethod
    def column_metadata_column_name() -> str:
        return "column_name"

    @staticmethod
    def column_metadata_datatype_name() -> str:
        return "data_type"

    def get_ordinal_position_name(self) -> str:
        return "ordinal_position"

    def catalog_column_filter(self) -> str:
        return ""
