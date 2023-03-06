from __future__ import annotations

import logging
from datetime import datetime

import ibm_db_dbi
from soda.common.logs import Logs
from soda.execution.data_source import DataSource
from soda.execution.data_type import DataType

logger = logging.getLogger(__name__)


class Db2DataSource(DataSource):
    TYPE = "db2"

    SCHEMA_CHECK_TYPES_MAPPING: dict = {
        "TEXT": ["TEXT", "VARCHAR"],
        # "NUMBER": ["int"],
        # "FLOAT": ["double"],
        # "TIMESTAMP_NTZ": ["timestamp"],
        # "TIMESTAMP_TZ": ["timestamp"],
    }

    SQL_TYPE_FOR_CREATE_TABLE_MAP: dict = {
        DataType.TEXT: "VARCHAR(255)",
        DataType.INTEGER: "INT",
        DataType.DECIMAL: "FLOAT",
        DataType.DATE: "DATE",
        DataType.TIME: "TIME",
        DataType.TIMESTAMP: "TIMESTAMP",
        DataType.TIMESTAMP_TZ: "TIMESTAMP",
        DataType.BOOLEAN: "BOOLEAN",
    }

    SQL_TYPE_FOR_SCHEMA_CHECK_MAP: dict = {
        DataType.TEXT: "VARCHAR",
        DataType.INTEGER: "INTEGER",
        DataType.DECIMAL: "DOUBLE",
        DataType.DATE: "DATE",
        DataType.TIME: "DATE",
        DataType.TIMESTAMP: "TIMESTAMP",
        DataType.TIMESTAMP_TZ: "TIMESTAMP",
        DataType.BOOLEAN: "BOOLEAN",
    }

    NUMERIC_TYPES_FOR_PROFILING = [
        "INT",
        "DOUBLE",
        "SMALLINT",
        "INTEGER",
        "BIGINT",
        "DECIMAL",
        "NUMERIC",
        "REAL",
        "DECFLOAT",
    ]
    TEXT_TYPES_FOR_PROFILING = ["VARCHAR", "CHARACTER"]

    def __init__(self, logs: Logs, data_source_name: str, data_source_properties: dict):
        super().__init__(logs, data_source_name, data_source_properties)
        self.host = data_source_properties.get("host")
        self.port = data_source_properties.get("port")
        self.password = data_source_properties.get("password")
        self.username = data_source_properties.get("username")
        self.database = data_source_properties.get("database")
        self.schema = data_source_properties.get("schema")
        self.update_schema(self.schema)

    def connect(self):
        conn_str = (
            f"DATABASE={self.database};HOSTNAME={self.host};PORT={self.port};UID={self.username};PWD={self.password}"
        )
        self.connection = ibm_db_dbi.connect(conn_str)
        return self.connection

    def validate_configuration(self, logs: Logs) -> None:
        pass

    def sql_information_schema_tables(self) -> str:
        return "SYSCAT.TABLES"

    def sql_information_schema_columns(self) -> str:
        return "SYSCAT.COLUMNS"

    def default_casify_column_name(self, identifier: str) -> str:
        return identifier.upper()

    def default_casify_type_name(self, identifier: str) -> str:
        return identifier.upper()

    @staticmethod
    def column_metadata_columns() -> list:
        return ["COLNAME", "TYPENAME", "NULLS"]

    @staticmethod
    def column_metadata_table_name() -> str:
        return "TABNAME"

    @staticmethod
    def column_metadata_schema_name() -> str:
        return "TABSCHEMA"

    @staticmethod
    def column_metadata_column_name() -> str:
        return "COLNAME"

    @staticmethod
    def column_metadata_datatype_name() -> str:
        return "TYPENAME"

    def get_ordinal_position_name(self) -> str:
        return "COLNO"

    def catalog_column_filter(self) -> str:
        return ""

    def sql_get_table_columns(
        self,
        table_name: str,
        included_columns: list[str] | None = None,
        excluded_columns: list[str] | None = None,
    ) -> str:
        def is_quoted(table_name):
            return (table_name.startswith('"') and table_name.endswith('"')) or (
                table_name.startswith("`") and table_name.endswith("`")
            )

        table_name_lower = table_name.lower()
        unquoted_table_name_lower = table_name_lower[1:-1] if is_quoted(table_name_lower) else table_name_lower

        filter_clauses = [f"lower(TABNAME) = '{unquoted_table_name_lower}'"]

        if self.schema:
            filter_clauses.append(f"lower(TABSCHEMA) = '{self.schema.lower()}'")

        if included_columns:
            include_clauses = []
            for col in included_columns:
                include_clauses.append(f"lower(COLNAME) LIKE lower('{col}')")
            include_causes_or = " OR ".join(include_clauses)
            filter_clauses.append(f"({include_causes_or})")

        if excluded_columns:
            for col in excluded_columns:
                filter_clauses.append(f"lower(COLNAME) NOT LIKE lower('{col}')")

        where_filter = " \n  AND ".join(filter_clauses)

        sql = (
            f"SELECT {', '.join(self.column_metadata_columns())} \n"
            f"FROM {self.sql_information_schema_columns()} \n"
            f"WHERE {where_filter}\n"
            "ORDER BY COLNO"
        )
        return sql

    def regex_replace_flags(self) -> str:
        # https://www.ibm.com/docs/en/db2-for-zos/12?topic=functions-regexp-replace
        # 1 : start position, 0: unlimited occurrences, `i` case insensitive
        return ", 1, 0,  'i'"

    def cast_text_to_number(self, column_name, validity_format: str):
        """Cast string to number

        - first regex replace removes extra chars, keeps: "digits + - . ,"
        - second regex changes "," to "."
        - Nullif makes sure that if regexes return empty string then Null is returned instead
        """
        regex = self.escape_regex(r"'[^-0-9\.\,]'")
        return f"CAST(NULLIF(REGEXP_REPLACE(REGEXP_REPLACE({column_name}, {regex}, ''{self.regex_replace_flags()}), ',', '.'{self.regex_replace_flags()}), '') AS {self.SQL_TYPE_FOR_CREATE_TABLE_MAP[DataType.DECIMAL]})"

    def sql_find_table_names(
        self,
        filter: str | None = None,
        include_tables: list[str] = [],
        exclude_tables: list[str] = [],
        table_column_name: str = "table_name",
        schema_column_name: str = "table_schema",
    ) -> str:
        return super().sql_find_table_names(
            filter,
            include_tables,
            exclude_tables,
            table_column_name="TABNAME",
            schema_column_name="TABSCHEMA",
        )

    def literal_datetime(self, datetime: datetime):
        formatted = datetime.strftime("%Y-%m-%d-%H.%M.%S")
        return f"TIMESTAMP '{formatted}'"

    def default_casify_table_name(self, identifier: str) -> str:
        return identifier.upper()

    def expr_avg(self, expr):
        return f"AVG(FLOAT({expr}))"
