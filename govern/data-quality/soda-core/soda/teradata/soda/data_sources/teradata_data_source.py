from __future__ import annotations

import logging
from datetime import datetime
from textwrap import dedent

import teradatasql
from soda.common.exceptions import DataSourceConnectionError
from soda.common.logs import Logs
from soda.execution.data_source import DataSource
from soda.execution.data_type import DataType

logger = logging.getLogger(__name__)


class TeradataDataSource(DataSource):
    TYPE = "teradata"

    SCHEMA_CHECK_TYPES_MAPPING: dict = {
        "varchar(255) character set unicode": ["varchar"],
        "double precision": ["float", "real", "double precision"],
        "timestamp without time zone": ["timestamp(0)", "timestamp"],
        "timestamp with time zone": ["timestamp(0) with time zone", "timestamp with time zone"],
        "decimal": ["numeric", "decimal", "decimal(12,6)"],
    }

    SQL_TYPE_FOR_CREATE_TABLE_MAP: dict = {
        DataType.TEXT: "varchar(255) character set unicode",
        DataType.INTEGER: "integer",
        DataType.DECIMAL: "decimal(12,6)",
        DataType.DATE: "date",
        DataType.TIME: "time",
        DataType.TIMESTAMP: "timestamp(0)",
        DataType.TIMESTAMP_TZ: "timestamp(0) with time zone",
    }

    SQL_TYPE_FOR_SCHEMA_CHECK_MAP: dict = {
        DataType.TEXT: "varchar(255) character set unicode",
        DataType.INTEGER: "integer",
        DataType.DECIMAL: "decimal(12,6)",
        DataType.DATE: "date",
        DataType.TIME: "time",
        DataType.TIMESTAMP: "timestamp(0)",
        DataType.TIMESTAMP_TZ: "timestamp(0) with time zone",
    }

    NUMERIC_TYPES_FOR_PROFILING = [
        "byteint",
        "smallint",
        "integer",
        "bigint",
        "decimal",
        "numeric",
        "float",
        "real",
        "double precision",
        "number",
        "real",
    ]

    TEXT_TYPES_FOR_PROFILING = ["character varying", "varchar", "character", "char"]
    LIMIT_KEYWORD = "TOP"

    def __init__(self, logs: Logs, data_source_name: str, data_source_properties: dict):
        super().__init__(logs, data_source_name, data_source_properties)

        self.host = data_source_properties.get("host", "localhost")
        self.port: str | None = data_source_properties.get("port")
        self.user = data_source_properties.get("user")
        self.password = data_source_properties.get("password")
        # There is no schema in Teradata DB, only databases
        self.database: str | None = data_source_properties.get("database")
        self.logmech: str | None = data_source_properties.get("logmech")
        self.tmode: str | None = data_source_properties.get("tmode")

    def connect(self):
        try:
            connection_data = {
                key: value
                for key, value in [
                    ("host", self.host),
                    ("dbs_port", self.port),
                    ("user", self.user),
                    ("password", self.password),
                    ("database", self.database),
                    ("logmech", self.logmech),
                    ("tmode", self.tmode),
                ]
                if value
            }
            self.connection = teradatasql.connect(None, **connection_data)
            return self.connection
        except Exception as e:
            raise DataSourceConnectionError(self.TYPE, e)

    def validate_configuration(self, logs: Logs) -> None:
        # Looks like method is not used anywhere
        pass

    def safe_connection_data(self):
        return [self.type, self.host, self.port, self.database, self.logmech, self.tmode]

    def get_type_name(self, type_code):
        return str(type_code).lower()

    # Thanks to SO https://stackoverflow.com/questions/21587034/get-column-type-using-teradata-system-tables
    _teradata_column_type = """lower(CASE ColumnType
    WHEN 'BF' THEN 'BYTE('            || TRIM(CAST(ColumnLength AS INTEGER)) || ')'
    WHEN 'BV' THEN 'VARBYTE('         || TRIM(CAST(ColumnLength AS INTEGER)) || ')'
    WHEN 'CF' THEN 'CHAR('            || TRIM(CAST(ColumnLength AS INTEGER)/CASE WHEN CharType > 1 THEN 2 ELSE 1 END) || ')'
    WHEN 'CV' THEN 'VARCHAR('         || TRIM(CAST(ColumnLength AS INTEGER)/CASE WHEN CharType > 1 THEN 2 ELSE 1 END) || ')'
    WHEN 'D ' THEN 'DECIMAL('         || TRIM(DecimalTotalDigits) || ','
                                      || TRIM(DecimalFractionalDigits) || ')'
    WHEN 'DA' THEN 'DATE'
    WHEN 'F ' THEN 'FLOAT'
    WHEN 'I1' THEN 'BYTEINT'
    WHEN 'I2' THEN 'SMALLINT'
    WHEN 'I8' THEN 'BIGINT'
    WHEN 'I ' THEN 'INTEGER'
    WHEN 'AT' THEN 'TIME('            || TRIM(DecimalFractionalDigits) || ')'
    WHEN 'TS' THEN 'TIMESTAMP('       || TRIM(DecimalFractionalDigits) || ')'
    WHEN 'TZ' THEN 'TIME('            || TRIM(DecimalFractionalDigits) || ')' || ' WITH TIME ZONE'
    WHEN 'SZ' THEN 'TIMESTAMP('       || TRIM(DecimalFractionalDigits) || ')' || ' WITH TIME ZONE'
    WHEN 'YR' THEN 'INTERVAL YEAR('   || TRIM(DecimalTotalDigits) || ')'
    WHEN 'YM' THEN 'INTERVAL YEAR('   || TRIM(DecimalTotalDigits) || ')'      || ' TO MONTH'
    WHEN 'MO' THEN 'INTERVAL MONTH('  || TRIM(DecimalTotalDigits) || ')'
    WHEN 'DY' THEN 'INTERVAL DAY('    || TRIM(DecimalTotalDigits) || ')'
    WHEN 'DH' THEN 'INTERVAL DAY('    || TRIM(DecimalTotalDigits) || ')'      || ' TO HOUR'
    WHEN 'DM' THEN 'INTERVAL DAY('    || TRIM(DecimalTotalDigits) || ')'      || ' TO MINUTE'
    WHEN 'DS' THEN 'INTERVAL DAY('    || TRIM(DecimalTotalDigits) || ')'      || ' TO SECOND('
                                      || TRIM(DecimalFractionalDigits) || ')'
    WHEN 'HR' THEN 'INTERVAL HOUR('   || TRIM(DecimalTotalDigits) || ')'
    WHEN 'HM' THEN 'INTERVAL HOUR('   || TRIM(DecimalTotalDigits) || ')'      || ' TO MINUTE'
    WHEN 'HS' THEN 'INTERVAL HOUR('   || TRIM(DecimalTotalDigits) || ')'      || ' TO SECOND('
                                      || TRIM(DecimalFractionalDigits) || ')'
    WHEN 'MI' THEN 'INTERVAL MINUTE(' || TRIM(DecimalTotalDigits) || ')'
    WHEN 'MS' THEN 'INTERVAL MINUTE(' || TRIM(DecimalTotalDigits) || ')'      || ' TO SECOND('
                                      || TRIM(DecimalFractionalDigits) || ')'
    WHEN 'SC' THEN 'INTERVAL SECOND(' || TRIM(DecimalTotalDigits) || ','
                                      || TRIM(DecimalFractionalDigits) || ')'
    WHEN 'BO' THEN 'BLOB('            || TRIM(CAST(ColumnLength AS INTEGER)) || ')'
    WHEN 'CO' THEN 'CLOB('            || TRIM(CAST(ColumnLength AS INTEGER)) || ')'

    WHEN 'PD' THEN 'PERIOD(DATE)'
    WHEN 'PM' THEN 'PERIOD(TIMESTAMP('|| TRIM(DecimalFractionalDigits) || ')' || ' WITH TIME ZONE'
    WHEN 'PS' THEN 'PERIOD(TIMESTAMP('|| TRIM(DecimalFractionalDigits) || '))'
    WHEN 'PT' THEN 'PERIOD(TIME('     || TRIM(DecimalFractionalDigits) || '))'
    WHEN 'PZ' THEN 'PERIOD(TIME('     || TRIM(DecimalFractionalDigits) || '))' || ' WITH TIME ZONE'
    WHEN 'UT' THEN COALESCE(ColumnUDTName,  '<Unknown> ' || ColumnType)

    WHEN '++' THEN 'TD_ANYTYPE'
    WHEN 'N'  THEN 'NUMBER('          || CASE WHEN DecimalTotalDigits = -128 THEN '*' ELSE TRIM(DecimalTotalDigits) END
                                      || CASE WHEN DecimalFractionalDigits IN (0, -128) THEN '' ELSE ',' || TRIM(DecimalFractionalDigits) END
                                      || ')'
    WHEN 'A1' THEN COALESCE('SYSUDTLIB.' || ColumnUDTName,  '<Unknown> ' || ColumnType)
    WHEN 'AN' THEN COALESCE('SYSUDTLIB.' || ColumnUDTName,  '<Unknown> ' || ColumnType)

    ELSE '<Unknown> ' || ColumnType
  END
  || CASE
        WHEN ColumnType IN ('CV', 'CF', 'CO')
        THEN CASE CharType
                WHEN 1 THEN ' CHARACTER SET LATIN'
                WHEN 2 THEN ' CHARACTER SET UNICODE'
                WHEN 3 THEN ' CHARACTER SET KANJISJIS'
                WHEN 4 THEN ' CHARACTER SET GRAPHIC'
                WHEN 5 THEN ' CHARACTER SET KANJI1'
                ELSE ''
             END
         ELSE ''
      END) AS ColumnType"""

    @staticmethod
    def column_metadata_columns() -> list:
        """Columns to be used for retrieving column metadata."""
        return ["ColumnName", TeradataDataSource._teradata_column_type, "Nullable"]

    @staticmethod
    def column_metadata_catalog_column() -> str:
        """Column to be used as a 'database' equivalent."""
        return "DatabaseName"

    @staticmethod
    def column_metadata_table_name() -> str:
        return "TableName"

    @staticmethod
    def column_metadata_schema_name() -> str:
        return ""

    @staticmethod
    def column_metadata_column_name() -> str:
        return "ColumnName"

    @staticmethod
    def column_metadata_datatype_name() -> str:
        return TeradataDataSource._teradata_column_type

    def sql_select_all(self, table_name: str, limit: int | None = None, filter: str | None = None) -> str:
        qualified_table_name = self.qualified_table_name(table_name)

        filter_sql = ""
        if filter:
            filter_sql = f" \n WHERE {filter}"

        limit_sql = ""
        if limit is not None:
            limit_sql = f" \n TOP {limit} \n"

        columns_names = ", ".join(self.sql_select_all_column_names(table_name))

        sql = f"SELECT {limit_sql} {columns_names} FROM {qualified_table_name}{filter_sql}"
        return sql

    def get_ordinal_position_name(self) -> str:
        return "ColumnId"

    def sql_get_table_names_with_count(
        self, include_tables: list[str] | None = None, exclude_tables: list[str] | None = None
    ) -> str | None:
        return None

    def sql_find_table_names(
        self,
        filter: str | None = None,
        include_tables: list[str] = [],
        exclude_tables: list[str] = [],
        table_column_name: str = "TableName",
        schema_column_name: str = "DatabaseName",
    ) -> str:
        return super().sql_find_table_names(
            filter,
            include_tables,
            exclude_tables,
            table_column_name="TableName",
            schema_column_name="DatabaseName",
        )

    def sql_information_schema_tables(self) -> str:
        return "dbc.tablesv"

    def sql_information_schema_columns(self) -> str:
        return "dbc.columnsv"

    def sql_get_duplicates_aggregated(
        self,
        column_names: str,
        table_name: str,
        filter: str,
        limit: str | None = None,
        invert_condition: bool = False,
        exclude_patterns: list[str] | None = None,
    ) -> str | None:
        limit_sql = ""
        main_query_columns = f"{column_names}, frequency" if exclude_patterns else "*"

        if limit:
            limit_sql = f"TOP {limit}"

        sql = dedent(
            f"""
            WITH frequencies AS (
                SELECT {column_names}, COUNT(*) AS frequency
                FROM {table_name}
                WHERE {filter}
                GROUP BY {column_names})
            SELECT {limit_sql} {main_query_columns}
            FROM frequencies
            WHERE frequency {'<=' if invert_condition else '>'} 1
            ORDER BY frequency DESC"""
        )
        return sql

    def sql_get_duplicates(
        self,
        column_names: str,
        table_name: str,
        filter: str,
        limit: str | None = None,
        invert_condition: bool = False,
        exclude_patterns: list[str] | None = None,
    ) -> str | None:
        columns = column_names.split(", ")

        qualified_main_query_columns = ", ".join([f"main.{c}" for c in columns])
        main_query_columns = qualified_main_query_columns if exclude_patterns else "main.*"
        join = " AND ".join([f"main.{c} = frequencies.{c}" for c in columns])

        limit_sql = ""
        if limit:
            limit_sql = f"TOP {limit}"

        sql = dedent(
            f"""
            WITH frequencies AS (
                SELECT {column_names}
                FROM {table_name}
                WHERE {filter}
                GROUP BY {column_names}
                HAVING count(*) {'<=' if invert_condition else '>'} 1)
            SELECT {limit_sql} {main_query_columns}
            FROM {table_name} main
            JOIN frequencies ON {join}
            """
        )

        return sql

    def sql_reference_query(
        self,
        columns: str,
        table_name: str,
        target_table_name: str,
        join_condition: str,
        where_condition: str,
        limit: int | None = None,
    ) -> str:
        limit_sql = ""
        if limit:
            limit_sql = f"TOP {limit}"

        sql = dedent(
            f"""
            SELECT {limit_sql} {columns}
                FROM {table_name}  SOURCE
                LEFT JOIN {target_table_name} TARGET on {join_condition}
            WHERE {where_condition}"""
        )

        return sql

    def cast_to_text(self, expr: str) -> str:
        return f"CAST({expr} AS VARCHAR(255))"

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
                            SELECT TOP {limit_frequent_values} {cast_to_text("'frequent_values'")} AS metric_, ROW_NUMBER() OVER(ORDER BY frequency_ DESC) AS index_, value_, frequency_
                            FROM value_frequencies
                            ORDER BY frequency_ desc
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
                            SELECT TOP {limit_mins_maxs} {cast_to_text("'mins'")} AS metric_, ROW_NUMBER() OVER(ORDER BY value_ ASC) AS index_, value_, frequency_
                            FROM value_frequencies
                            WHERE value_ IS NOT NULL
                            ORDER BY value_ ASC

                        )"""

            maxs_cte = f"""maxs AS (
                            SELECT TOP {limit_mins_maxs} {cast_to_text("'maxs'")} AS metric_, ROW_NUMBER() OVER(ORDER BY value_ DESC) AS index_, value_, frequency_
                            FROM value_frequencies
                            WHERE value_ IS NOT NULL
                            ORDER BY value_ DESC

                        )"""

            return dedent(
                f"""
                    WITH
                        {value_frequencies_cte},
                        {mins_cte},
                        {maxs_cte},
                        {frequent_values_cte},
                        result_values AS (
                            SELECT * FROM mins
                            {union}
                            SELECT * FROM maxs
                            {union}
                            SELECT * FROM frequent_values
                        )
                    SELECT *
                    FROM result_values
                    ORDER BY metric_ ASC, index_ ASC
                """
            )

        raise AssertionError("data_type_category must be either 'numeric' or 'text'")

    def _create_table_prefix(self):
        return self.database

    def literal_datetime(self, datetime: datetime):
        formatted = datetime.strftime("%Y-%m-%d %H:%M:%S")
        return f"TIMESTAMP '{formatted}'"

    def literal_boolean(self, boolean: bool):
        return "Y" if boolean is True else "N"

    def expr_regexp_like(self, expr: str, regex_pattern: str):
        return f"REGEXP_SIMILAR({expr}, '{regex_pattern}') = 1"

    def regex_replace_flags(self) -> str:
        return ", 1, 0"

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
        limit_clauses_str = f"TOP {limit}" if limit else ""

        sql = (
            f"SELECT {limit_clauses_str} \n"
            f"  {column_name} \n"
            f"FROM {table_name}{sample_clauses_str}{filter_clauses_str}"
        )
        return sql

    def expr_false_condition(self):
        return "1 = 0"

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

        casify_function = self.default_casify_sql_function()
        filter_clauses = [
            f"{casify_function}({self.column_metadata_table_name()}) = '{unquoted_table_name_default_case}'"
        ]

        if self.database:
            filter_clauses.append(
                f"{casify_function}({self.column_metadata_catalog_column()}) = '{self.default_casify_system_name(self.database)}'"
            )

        if included_columns:
            include_clauses = []
            for col in included_columns:
                include_clauses.append(
                    f"{casify_function}({self.column_metadata_column_name()}) LIKE {casify_function}('{col}')"
                )
            include_causes_or = " OR ".join(include_clauses)
            filter_clauses.append(f"({include_causes_or})")

        if excluded_columns:
            for col in excluded_columns:
                filter_clauses.append(
                    f"{casify_function}({self.column_metadata_column_name()}) NOT LIKE {casify_function}('{col}')"
                )

        where_filter = " \n  AND ".join(filter_clauses)

        # compose query template
        # NOTE: we use `order by ordinal_position` to guarantee stable orders of columns
        # (see https://www.postgresql.org/docs/current/infoschema-columns.html)
        # this mainly has an advantage in testing but bears very little as to how Soda Cloud
        # displays those columns as they are ordered alphabetically in the UI.
        sql = (
            f"SELECT {', '.join(self.column_metadata_columns())} \n"
            f"FROM {self.sql_information_schema_columns()} \n"
            f"WHERE {where_filter}"
            f"\nORDER BY {self.get_ordinal_position_name()}"
        )
        return sql

    def get_metric_sql_aggregation_expression(self, metric_name: str, metric_args: list[object] | None, expr: str):
        if metric_name in [
            "stddev_pop",
            "stddev_samp",
            "var_pop",
            "var_samp",
        ]:
            return f"{metric_name.upper()}({expr})"

        if metric_name == "stddev":
            return f"STDDEV_SAMP({expr})"
        if metric_name == "variance":
            return f"VAR_SAMP({expr})"

        if metric_name in ["percentile", "percentile_disc"]:
            sql_func = "percentile_cont" if metric_name == "percentile" else "percentile_disc"
            # TODO ensure proper error if the metric_args[0] is not a valid number
            percentile_fraction = metric_args[1] if metric_args else None
            return f"{sql_func}({percentile_fraction}) WITHIN GROUP (ORDER BY {expr})"
        return super().get_metric_sql_aggregation_expression(metric_name, metric_args, expr)

    def profiling_sql_aggregates_numeric(self, table_name: str, column_name: str) -> str:
        column_name = self.quote_column(column_name)
        qualified_table_name = self.qualified_table_name(table_name)
        # average and sum are reserved words in Teradata
        return dedent(
            f"""
            SELECT
                avg({column_name}) as "average"
                , sum({column_name}) as "sum"
                , var_samp({column_name}) as variance
                , stddev_samp({column_name}) as standard_deviation
                , count(distinct({column_name})) as distinct_values
                , sum(case when {column_name} is null then 1 else 0 end) as missing_values
            FROM {qualified_table_name}
            """
        )

    def sql_table_include_exclude_filter(
        self,
        table_column_name: str,
        schema_column_name: str | None = None,
        include_tables: list[str] | None = [],
        exclude_tables: list[str] | None = [],
    ) -> str | None:
        tablename_filter_clauses = []

        def build_table_matching_conditions(tables: list[str], comparison_operator: str):
            conditions = []

            for table in tables:
                # This is intended to be future proof and support quoted table names. The method is not used in such way and table names/patterns that still
                # need to be quoted are passed here, e.g. `%sodatest_%`. I.e. this condition is always met and default casify is always used, table name below is therefore single quoted.
                if not self.is_quoted(table):
                    table = self.default_casify_table_name(table)

                conditions.append(f"{table_column_name} {comparison_operator} '{table}'")
            return conditions

        if include_tables:
            sql_include_clauses = " OR ".join(build_table_matching_conditions(include_tables, "like"))
            tablename_filter_clauses.append(f"({sql_include_clauses})")

        if exclude_tables:
            tablename_filter_clauses.extend(build_table_matching_conditions(exclude_tables, "not like"))

        if hasattr(self, "database") and self.database:
            tablename_filter_clauses.append(f"DatabaseName = '{self.database}'")

        return "\n      AND ".join(tablename_filter_clauses) if tablename_filter_clauses else None
