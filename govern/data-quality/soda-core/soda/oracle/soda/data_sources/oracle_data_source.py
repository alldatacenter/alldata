from __future__ import annotations

import logging
from datetime import date, datetime
from textwrap import dedent

import oracledb
from soda.common.logs import Logs
from soda.execution.data_source import DataSource
from soda.execution.data_type import DataType

logger = logging.getLogger(__name__)


class OracleDataSource(DataSource):
    TYPE = "oracle"

    LIMIT_KEYWORD = "FETCH FIRST"

    SCHEMA_CHECK_TYPES_MAPPING: dict = {
        "VARCHAR2": ["varchar"],
        # "double precision": ["decimal"],
        # "timestamp without time zone": ["timestamp"],
        # "timestamp with time zone": ["timestamptz"],
    }

    # Supported data types used in create statements.
    # These are used in tests for creating test data and do not affect the actual library functionality.
    SQL_TYPE_FOR_CREATE_TABLE_MAP: dict = {
        DataType.TEXT: "VARCHAR(255)",
        DataType.INTEGER: "INT",
        DataType.DECIMAL: "FLOAT",
        DataType.DATE: "DATE",
        DataType.TIME: "TIME",
        DataType.TIMESTAMP: "TIMESTAMP",
        DataType.TIMESTAMP_TZ: "TIMESTAMP WITH TIME ZONE",
        DataType.BOOLEAN: "BOOLEAN",
    }

    # Supported data types as returned by the given data source when retrieving dataset schema. Used in schema checks.
    SQL_TYPE_FOR_SCHEMA_CHECK_MAP = {
        DataType.TEXT: "VARCHAR2",
        DataType.INTEGER: "NUMBER",
        DataType.DECIMAL: "FLOAT",
        DataType.DATE: "DATE",
        DataType.TIME: "TIMESTAMP",
        DataType.TIMESTAMP: "TIMESTAMP(6)",
        DataType.TIMESTAMP_TZ: "TIMESTAMP(6) WITH TIME ZONE",
        DataType.BOOLEAN: "BOOL",
    }

    # Indicate which numeric/test data types can be used for profiling checks.
    NUMERIC_TYPES_FOR_PROFILING = [
        "NUMERIC",
        "DECIMAL",
        "DEC",
        "INTEGER",
        "INT",
        "SMALLINT",
        "FLOAT",
        "" "DOUBLE PRECISION",
        "REAL",
    ]
    TEXT_TYPES_FOR_PROFILING = ["CHARACTER VARYING", "CHAR", "NCHAR", "VARCHAR", "CHAR VARYING", "VARCHAR2"]

    def __init__(self, logs: Logs, data_source_name: str, data_source_properties: dict):
        super().__init__(logs, data_source_name, data_source_properties)
        self.username = data_source_properties.get("username", "localhost")
        self.password = data_source_properties.get("password", "")
        self.connectstring = data_source_properties.get("connectstring")

    def connect(self):
        self.connection = oracledb.connect(user=self.username, password=self.password, dsn=self.connectstring)

    def sql_test_connection(self) -> str:
        return "SELECT 1 FROM DUAL"

    def sql_information_schema_tables(self) -> str:
        return "ALL_TABLES"

    def sql_information_schema_columns(self) -> str:
        return "ALL_TAB_COLS"

    @staticmethod
    def column_metadata_columns() -> list:
        """Columns to be used for retrieving column metadata."""
        return ["COLUMN_NAME", "DATA_TYPE", "NULLABLE"]

    @staticmethod
    def column_metadata_table_name() -> str:
        return "TABLE_NAME"

    @staticmethod
    def column_metadata_schema_name() -> str:
        return "OWNER"

    @staticmethod
    def column_metadata_column_name() -> str:
        return "COLUMN_NAME"

    @staticmethod
    def column_metadata_datatype_name() -> str:
        return "DATA_TYPE"

    def catalog_column_filter(self) -> str:
        return ""

    def validate_configuration(self, logs: Logs) -> None:
        pass

    def literal_date(self, date: date):
        date_string = date.strftime("%Y-%m-%d")
        return f"DATE'{date_string}'".strip()

    def literal_datetime(self, datetime: datetime):
        datetime_str = datetime.strftime("%Y-%m-%d %H:%M:%S %Z")
        return f"TIMESTAMP '{datetime_str}'".strip()

    def sql_find_table_names(
        self,
        filter: str | None = None,
        include_tables: list[str] = [],
        exclude_tables: list[str] = [],
        table_column_name: str = "TABLE_NAME",
        schema_column_name: str = "OWNER",
    ) -> str:
        return super().sql_find_table_names(
            filter,
            include_tables,
            exclude_tables,
            table_column_name="TABLE_NAME",
            schema_column_name="OWNER",
        )

    def sql_select_all(self, table_name: str, limit: int | None = None, filter: str | None = None) -> str:
        qualified_table_name = self.qualified_table_name(table_name)

        filter_sql = ""
        if filter:
            filter_sql = f" \n WHERE {filter}"

        limit_sql = ""
        if limit is not None:
            limit_sql = f" \n FETCH FIRST {limit} ROWS ONLY \n"

        columns_names = ", ".join(self.sql_select_all_column_names(table_name))

        sql = f"SELECT  {columns_names} FROM {qualified_table_name}{filter_sql} {limit_sql}"
        return sql

    def default_casify_table_name(self, identifier: str) -> str:
        return identifier.upper()

    def default_casify_column_name(self, identifier: str) -> str:
        return identifier.upper()

    def default_casify_sql_function(self) -> str:
        """Returns the sql function to use for default casify."""
        return "upper"

    def default_casify_type_name(self, identifier: str) -> str:
        """Formats type identifier to e.g. a default case for a given data source."""
        return identifier.upper()

    def expr_false_condition(self):
        return "1 = 0"

    def get_ordinal_position_name(self) -> str:
        return "COLUMN_ID"

    def cast_to_text(self, expr: str) -> str:
        return f"CAST({expr} AS VARCHAR2(30))"

    def profiling_sql_values_frequencies_query(
        self,
        data_type_category: str,
        table_name: str,
        column_name: str,
        limit_mins_maxs: int,
        limit_frequent_values: int,
    ) -> str:
        cast_to_text = self.cast_to_text

        value_frequencies_cte = self.profiling_sql_value_frequencies_cte(table_name, column_name)

        union = self.sql_union()

        frequent_values_cte = f"""frequent_values AS (
                            SELECT  {cast_to_text("'frequent_values'")} AS metric_, ROW_NUMBER() OVER(ORDER BY frequency_ DESC) AS index_, value_, frequency_
                            FROM value_frequencies
                            ORDER BY frequency_ desc
                             FETCH FIRST  {limit_frequent_values} ROWS ONLY
                        )"""

        if data_type_category == "text":
            return dedent(
                f"""
                    WITH
                        {value_frequencies_cte},
                        {frequent_values_cte}
                    SELECT *
                    FROM frequent_values
                    ORDER BY metric_ ASC, index_ ASC
                """
            )

        elif data_type_category == "numeric":
            mins_cte = f"""mins AS (
                            SELECT {cast_to_text("'mins'")} AS metric_, ROW_NUMBER() OVER(ORDER BY value_ ASC) AS index_, value_, frequency_
                            FROM value_frequencies
                            WHERE value_ IS NOT NULL
                            ORDER BY value_ ASC
                            FETCH FIRST  {limit_mins_maxs} ROWS ONLY

                        )"""

            maxs_cte = f"""maxs AS (
                            SELECT {cast_to_text("'maxs'")} AS metric_, ROW_NUMBER() OVER(ORDER BY value_ DESC) AS index_, value_, frequency_
                            FROM value_frequencies
                            WHERE value_ IS NOT NULL
                            ORDER BY value_ DESC
                            FETCH FIRST  {limit_mins_maxs} ROWS ONLY

                        )"""

            return dedent(
                f"""
                    WITH
                        {value_frequencies_cte},
                        {mins_cte},
                        {maxs_cte},
                        {frequent_values_cte},
                        result AS (
                            SELECT * FROM mins
                            {union}
                            SELECT * FROM maxs
                            {union}
                            SELECT * FROM frequent_values
                        )
                    SELECT *
                    FROM result
                    ORDER BY metric_ ASC, index_ ASC
                """
            )

        raise AssertionError("data_type_category must be either 'numeric' or 'text'")

    def sql_select_column_with_filter_and_limit(
        self,
        column_name: str,
        table_name: str,
        filter_clause: str | None = None,
        sample_clause: str | None = None,
        limit: int | None = None,
    ) -> str:
        """
        Returns a SQL query that selects a column from a table with optional filter, sample query
        and limit.
        """
        filter_clauses_str = f"\n WHERE {filter_clause}" if filter_clause else ""
        sample_clauses_str = f"\n {sample_clause}" if sample_clause else ""
        limit_str = f"\n FETCH FIRST {limit} ROWS ONLY" if limit else ""
        sql = f"SELECT \n" f"  {column_name} \n" f"FROM {table_name}{sample_clauses_str}{filter_clauses_str}{limit_str}"
        return sql
