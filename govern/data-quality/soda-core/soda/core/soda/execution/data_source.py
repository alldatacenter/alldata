from __future__ import annotations

import datetime
import hashlib
import importlib
import json
import re
from collections import defaultdict
from datetime import date, datetime
from functools import lru_cache
from numbers import Number
from textwrap import dedent

from soda.common.exceptions import DataSourceError
from soda.common.logs import Logs
from soda.common.string_helper import string_matches_simple_pattern
from soda.execution.data_type import DataType
from soda.execution.query.query import Query
from soda.execution.query.schema_query import TableColumnsQuery
from soda.sampler.sample_ref import SampleRef
from soda.sodacl.location import Location


class FormatHelper:
    @staticmethod
    def build_default_formats():
        # s stands for an optional set of spaces
        s = r" *"
        sign_any_opt = r"[-+]?"
        plus_opt = r"\+?"
        min = r"\-"
        digits = r"[0-9]+"
        decimal_point_comma = r"(\d+([\.,]\d+)?|([\.,]\d+))"
        decimal_point = r"(\d+(\.\d+)?|(\.\d+))"
        decimal_comma = r"(\d+(,\d+)?|(,\d+))"
        zero_integer = r"0+"
        zero_point_comma = r"(0+([\.,]0+)?|([\.,]0+))"
        zero_point = r"(0+(\.0+)?|(\.0+))"
        zero_comma = r"(0+(,0+)?|(,0+))"
        money_point = r"\d{1,3}(\,\d\d\d)*(.\d+)?"
        money_comma = r"\d{1,3}(\.\d\d\d)*(,\d+)?"
        currency = r"([A-Z]{3}|[a-z]{3})"

        day = r"([1-9]|[012][0-9]|3[01])"
        month = r"([1-9]|0[1-9]|1[012])"
        year = r"(19|20)?\d\d"
        hour24 = r"(0?[0-9]|[01]\d|2[0-3])"
        hour12 = r"(0?[0-9]|1[0-2])"
        minute = r"[0-5]?[0-9]"
        second = r"[0-5]?[0-9]([.,]\d+)?"
        year4 = r"(19|20)\d\d"
        month2 = r"(0[0-9]|1[12])"
        day2 = r"([012][0-9]|3[01])"
        hour2 = r"(0[0-9]|1[012])"
        minute2 = r"[0-5][0-9]"
        second2 = minute2

        return {
            "integer": f"^{s}{sign_any_opt}{s}{digits}{s}$",
            "positive integer": f"^{s}{plus_opt}{s}{digits}{s}$",
            "negative integer": f"^{s}({min}{s}{digits}|{zero_integer}){s}$",
            "decimal": f"^{s}{sign_any_opt}{s}{decimal_point_comma}{s}$",
            "positive decimal": f"^{s}{plus_opt}{s}{decimal_point_comma}{s}$",
            "negative decimal": f"^{s}({min}{s}{decimal_point_comma}|{zero_point_comma}){s}$",
            "decimal point": f"^{s}{sign_any_opt}{s}{decimal_point}{s}$",
            "positive decimal point": f"^{s}{plus_opt}{s}{decimal_point}{s}$",
            "negative decimal point": f"^{s}({min}{s}{decimal_point}|{zero_point}){s}$",
            "decimal comma": f"^{s}{sign_any_opt}{s}{decimal_comma}{s}$",
            "positive decimal comma": f"^{s}{plus_opt}{s}{decimal_comma}{s}$",
            "negative decimal comma": f"^{s}({min}{s}{decimal_comma}|{zero_comma}){s}$",
            "percentage": f"^{s}{sign_any_opt}{s}{decimal_point_comma}{s}%{s}$",
            "positive percentage": f"^{s}{plus_opt}{s}{decimal_point_comma}{s}%{s}$",
            "negative percentage": f"^{s}({min}{s}{decimal_point_comma}|{zero_point_comma}){s}%{s}$",
            "percentage point": f"^{s}{sign_any_opt}{s}{decimal_point}{s}%{s}$",
            "positive percentage point": f"^{s}{plus_opt}{s}{decimal_point}{s}%{s}$",
            "negative percentage point": f"^{s}({min}{s}{decimal_point}|{zero_point}){s}%{s}$",
            "percentage comma": f"^{s}{sign_any_opt}{s}{decimal_comma}{s}%{s}$",
            "positive percentage comma": f"^{s}{plus_opt}{s}{decimal_comma}{s}%{s}$",
            "negative percentage comma": f"^{s}({min}{s}{decimal_comma}|{zero_comma}){s}%{s}$",
            "money": f"^{s}{sign_any_opt}{s}{decimal_point_comma}{s}{currency}{s}$",
            "money point": f"^{s}{sign_any_opt}{s}{money_point}{s}{currency}{s}$",
            "money comma": f"^{s}{sign_any_opt}{s}{money_comma}{s}{currency}{s}$",
            "date us": rf"^{s}{month}[-\./]{day}[-\./]{year}{s}$",
            "date eu": rf"^{s}{day}[-\./]{month}[-\./]{year}{s}$",
            "date inverse": rf"^{s}{year}[-\./]{month}[-\./]{day}{s}$",
            "date iso 8601": f"^{s}"
            rf"{year4}-?({month2}-?{day2}|W[0-5]\d(-?[1-7])?|[0-3]\d\d)"
            rf"([ T]{hour2}(:?{minute2}(:?{second2}([.,]\d+)?)?)?([+-]{hour2}:?{minute2}|Z)?)?"
            f"{s}$",
            "time 24h": f"^{s}{hour24}:{minute}(:{second})?{s}$",
            "time 24h nosec": f"^{s}{hour24}:{minute}{s}$",
            "time 12h": f"^{s}{hour12}:{minute}(:{second})?{s}$",
            "time 12h nosec": f"^{s}{hour12}:{minute}{s}$",
            "timestamp 24h": f"^{s}{hour24}:{minute}:{second}{s}$",
            "timestamp 12h": f"^{s}{hour12}:{minute}:{second}{s}$",
            "uuid": r"^[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}$",
            "ip address": r"^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$",
            "ipv4 address": r"^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$",
            # scary - but covers all cases
            "ipv6 address": r"^(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))$",
            "email": r"^[a-zA-ZÀ-ÿĀ-ſƀ-ȳ0-9.\-_%+]+@[a-zA-ZÀ-ÿĀ-ſƀ-ȳ0-9.\-_%]+\.[A-Za-z]{2,4}$",
            "phone number": r"^((\+[0-9]{1,2}\s)?\(?[0-9]{3}\)?[\s.-])?[0-9]{3}[\s.-][0-9]{4}$",
            "credit card number": r"^[0-9]{14}|[0-9]{15}|[0-9]{16}|[0-9]{17}|[0-9]{18}|[0-9]{19}|([0-9]{4}-){3}[0-9]{4}|([0-9]{4} ){3}[0-9]{4}$",
        }


class DataSource:
    """
    Data source implementation.

    For documentation on implementing a new DataSource see the CONTRIBUTING-DATA-SOURCE.md document.
    """

    # Maps synonym types for the convenience of use in checks.
    # Keys represent the data_source type, values are lists of "aliases" that can be used in SodaCL as synonyms.
    SCHEMA_CHECK_TYPES_MAPPING: dict = {
        "character varying": ["varchar", "text"],
        "double precision": ["decimal"],
        "timestamp without time zone": ["timestamp"],
        "timestamp with time zone": ["timestamptz"],
    }

    # Supported data types used in create statements. These are used in tests for creating test data and do not affect the actual library functionality.
    SQL_TYPE_FOR_CREATE_TABLE_MAP: dict = {
        DataType.TEXT: "VARCHAR(255)",
        DataType.INTEGER: "INT",
        DataType.DECIMAL: "FLOAT",
        DataType.DATE: "DATE",
        DataType.TIME: "TIME",
        DataType.TIMESTAMP: "TIMESTAMP",
        DataType.TIMESTAMP_TZ: "TIMESTAMPTZ",
        DataType.BOOLEAN: "BOOLEAN",
    }

    # Supported data types as returned by the given data source when retrieving dataset schema. Used in schema checks.
    SQL_TYPE_FOR_SCHEMA_CHECK_MAP = {
        DataType.TEXT: "character varying",
        DataType.INTEGER: "integer",
        DataType.DECIMAL: "double precision",
        DataType.DATE: "date",
        DataType.TIME: "time",
        DataType.TIMESTAMP: "timestamp without time zone",
        DataType.TIMESTAMP_TZ: "timestamp with time zone",
        DataType.BOOLEAN: "boolean",
    }

    # Indicate which numeric/test data types can be used for profiling checks.
    NUMERIC_TYPES_FOR_PROFILING = [
        "integer",
        "double precision",
        "double",
        "smallint",
        "bigint",
        "decimal",
        "numeric",
        "real",
        "smallserial",
        "serial",
        "bigserial",
    ]
    TEXT_TYPES_FOR_PROFILING = ["character varying", "varchar", "text", "character", "char"]
    LIMIT_KEYWORD: str = "LIMIT"

    # Building up format queries normally works with regexp expression + a set of formats,
    # but some use cases require whole completely custom format expressions.
    # DEFAULT_FORMAT_EXPRESSIONS take precedence over DEFAULT_FORMATS.
    DEFAULT_FORMATS: dict[str, str] = FormatHelper.build_default_formats()
    DEFAULT_FORMAT_EXPRESSIONS: dict[str, str] = {}

    @staticmethod
    def camel_case_data_source_type(data_source_type: str) -> str:
        if "bigquery" == data_source_type:
            return "BigQuery"
        elif "spark_df" == data_source_type:
            return "SparkDf"
        elif "sqlserver" == data_source_type:
            return "SQLServer"
        elif "mysql" == data_source_type:
            return "MySQL"
        elif "duckdb" == data_source_type:
            return "DuckDB"
        else:
            return f"{data_source_type[0:1].upper()}{data_source_type[1:]}"

    @staticmethod
    def create(
        logs: Logs,
        data_source_name: str,
        data_source_type: str,
        data_source_properties: dict,
    ) -> DataSource:
        """
        The returned data_source does not have a connection.  It is the responsibility of
        the caller to initialize data_source.connection.  To create a new connection,
        use data_source.connect(...)
        """
        try:
            data_source_properties["connection_type"] = data_source_type
            module = importlib.import_module(f"soda.data_sources.{data_source_type}_data_source")
            data_source_class = f"{DataSource.camel_case_data_source_type(data_source_type)}DataSource"
            class_ = getattr(module, data_source_class)
            return class_(logs, data_source_name, data_source_properties)
        except ModuleNotFoundError:
            if data_source_type == "postgresql":
                logs.error(f'Data source type "{data_source_type}" not found. Did you mean postgres?')
            else:
                raise DataSourceError(
                    f'Data source type "{data_source_type}" not found. Did you spell {data_source_type} correctly? Did you install module soda-core-{data_source_type}?'
                )
            return None

    def __init__(
        self,
        logs: Logs,
        data_source_name: str,
        data_source_properties: dict,
    ):
        self.host = data_source_properties.get("host")
        self.logs = logs
        self.data_source_name = data_source_name
        self.data_source_properties: dict = data_source_properties
        # Pep 249 compliant connection object (aka DBAPI)
        # https://www.python.org/dev/peps/pep-0249/#connection-objects
        # @see self.connect() for initialization
        self.type = self.data_source_properties.get("connection_type")
        self.connection = None
        self.database: str | None = data_source_properties.get("database")
        self.schema: str | None = data_source_properties.get("schema")
        self.table_prefix: str | None = self._create_table_prefix()
        # self.data_source_scan is initialized in create_data_source_scan(...) below
        self.data_source_scan: DataSourceScan | None = None

    def has_valid_connection(self) -> bool:
        query = Query(
            data_source_scan=self.data_source_scan,
            sql=self.sql_test_connection(),
            unqualified_query_name="test-connection",
        )
        query.execute()

        if query.exception:
            return False

        return True

    def create_data_source_scan(self, scan: Scan, data_source_scan_cfg: DataSourceScanCfg):
        from soda.execution.data_source_scan import DataSourceScan

        data_source_scan = DataSourceScan(scan, data_source_scan_cfg, self)
        self.data_source_scan = data_source_scan

        return self.data_source_scan

    def validate_configuration(self, logs: Logs) -> None:
        raise NotImplementedError(f"TODO: Implement {type(self)}.validate_configuration(...)")

    def get_type_name(self, type_code):
        return str(type_code)

    def is_supported_metric_name(self, metric_name: str) -> bool:
        return (
            metric_name in ["row_count", "missing_count", "invalid_count", "valid_count", "duplicate_count"]
            or self.get_metric_sql_aggregation_expression(metric_name, None, None) is not None
        )

    def get_metric_sql_aggregation_expression(self, metric_name: str, metric_args: list[object] | None, expr: str):
        if "min" == metric_name:
            return self.expr_min(expr)
        if "max" == metric_name:
            return self.expr_max(expr)
        if "avg" == metric_name:
            return self.expr_avg(expr)
        if "sum" == metric_name:
            return self.expr_sum(expr)
        if "min_length" == metric_name:
            return self.expr_min(self.expr_length(expr))
        if "max_length" == metric_name:
            return self.expr_max(self.expr_length(expr))
        if "avg_length" == metric_name:
            return self.expr_avg(self.expr_length(expr))
        return None

    def is_same_type_in_schema_check(self, expected_type: str, actual_type: str):
        expected_type = expected_type.strip().lower()

        if (
            actual_type in self.SCHEMA_CHECK_TYPES_MAPPING
            and expected_type in self.SCHEMA_CHECK_TYPES_MAPPING[actual_type]
        ):
            return True

        return expected_type == actual_type.lower()

    @staticmethod
    def column_metadata_columns() -> list:
        """Columns to be used for retrieving column metadata."""
        return ["column_name", "data_type", "is_nullable"]

    @staticmethod
    def column_metadata_catalog_column() -> str:
        """Column to be used as a 'database' equivalent."""
        return "table_catalog"

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

    def tables_columns_metadata(self) -> list[str]:
        """Columns to be used for retrieving tables and columns metadata."""
        return [
            self.column_metadata_table_name(),
            self.column_metadata_column_name(),
            self.column_metadata_datatype_name(),
        ]

    ######################
    # Store Table Sample
    ######################

    def store_table_sample(self, table_name: str, limit: int | None = None, filter: str | None = None) -> SampleRef:
        sql = self.sql_select_all(table_name=table_name, limit=limit, filter=filter)
        query = Query(
            data_source_scan=self.data_source_scan,
            unqualified_query_name=f"store-sample-for-{table_name}",
            sql=sql,
            sample_name="table_sample",
        )
        query.store()
        return query.sample_ref

    def sql_select_all(self, table_name: str, limit: int | None = None, filter: str | None = None) -> str:
        qualified_table_name = self.qualified_table_name(table_name)

        filter_sql = ""
        if filter:
            filter_sql = f" \n WHERE {filter}"

        limit_sql = ""
        if limit is not None:
            limit_sql = f" \n LIMIT {limit}"

        columns_names = ", ".join(self.sql_select_all_column_names(table_name))

        sql = f"SELECT {columns_names} FROM {qualified_table_name}{filter_sql}{limit_sql}"
        return sql

    def sql_select_all_column_names(self, table_name: str) -> list:
        selectable_columns = []

        if self.get_exclude_column_patterns_for_table(table_name) and self.data_source_scan.scan._configuration.sampler:
            all_columns = self.get_table_columns(table_name, f"get_table_columns_{table_name}")
            exclude_columns = []

            for column in all_columns:
                if self.is_column_excluded(table_name, column):
                    exclude_columns.append(column)
                else:
                    selectable_columns.append(column)

            if exclude_columns:
                self.logs.debug(
                    f"Skipping columns {exclude_columns} from table '{table_name}' when selecting all columns data."
                )
            if not selectable_columns:
                self.logs.info(
                    f"Unable to select data for failed rows from table '{table_name}', all columns are excluded. Selecting '*' for the check, no failed rows samples will be created."
                )

        if not selectable_columns:
            selectable_columns = ["*"]

        return selectable_columns

    def get_exclude_column_patterns_for_table(self, table_name: str) -> list[str]:
        """Match table and column names case insensitive."""
        exclude_columns_config = {
            k.lower(): [c.lower() for c in v]
            for k, v in self.data_source_scan.scan._configuration.exclude_columns.items()
        }
        exclude_column_patterns = []

        table_name_lower = table_name.lower()

        for table_pattern, column_patterns in exclude_columns_config.items():
            "Table sometimes comes with a schema a prefix, try matching on 'prefix.pattern' as well"
            table_pattern_with_schema = f"{self.table_prefix if self.table_prefix else ''}.{table_pattern}".lower()
            if string_matches_simple_pattern(table_name_lower, table_pattern) or string_matches_simple_pattern(
                table_name_lower, table_pattern_with_schema
            ):
                exclude_column_patterns += column_patterns

        return list(set(exclude_column_patterns))

    def is_column_excluded(self, table_name: str, column: str) -> bool:
        column_patterns_for_table = self.get_exclude_column_patterns_for_table(table_name)

        for pattern in column_patterns_for_table:
            if string_matches_simple_pattern(column, pattern) or column == "*":
                # '*' is a special case - it is considered excluded in case there is at least one column exclude pattern for the given table.
                return True

        return False

    @staticmethod
    def parse_tables_columns_query(rows: list[tuple]) -> defaultdict(dict):
        tables_and_columns_metadata = defaultdict(dict)
        for table_name, column_name, data_type in rows:
            tables_and_columns_metadata[table_name][column_name] = data_type
        return tables_and_columns_metadata

    def get_tables_columns_metadata(
        self,
        query_name: str,
        include_patterns: list[dict[str, str]] | None = None,
        exclude_patterns: list[dict[str, str]] | None = None,
        table_names_only: bool = False,
    ) -> defaultdict[str, dict[str, str]] | None:
        # TODO: save/cache the result for later use.
        if (not include_patterns) and (not exclude_patterns):
            return []
        query = Query(
            data_source_scan=self.data_source_scan,
            unqualified_query_name=query_name,
            sql=self.sql_get_tables_columns_metadata(
                include_patterns=include_patterns, exclude_patterns=exclude_patterns, table_names_only=table_names_only
            ),
        )
        query.execute()

        rows = query.rows
        if rows and len(rows) > 0:
            if table_names_only:
                query_result = [self._optionally_quote_table_name_from_meta_data(row[0]) for row in rows]
            else:
                query_result: defaultdict[dict] = self.parse_tables_columns_query(rows)
            return query_result
        return None

    def create_table_column_sql_filters(
        self, sql_patterns: list[dict[str, str]], table_names_only: bool = False
    ) -> list[str]:
        sql_filters = []

        for pattern in sql_patterns:
            sql_filter = []

            table_name_pattern = pattern.get("table_name_pattern")
            if table_name_pattern is not None:
                table_name_filter = (
                    f"({self.column_metadata_table_name()} LIKE '{self.default_casify_table_name(table_name_pattern)}')"
                )
                sql_filter.append(table_name_filter)

            column_name_pattern = pattern.get("column_name_pattern")
            if (column_name_pattern is not None) and (table_names_only is False):
                column_name_filter = f"({self.default_casify_sql_function()}({self.column_metadata_column_name()}) LIKE {self.default_casify_sql_function()}('{column_name_pattern}'))"
                sql_filter.append(column_name_filter)

            if sql_filter:
                sql_filters.append(f"""({" AND ".join(sql_filter)})""")

        return sql_filters

    def catalog_column_filter(self):
        catalog_filter = f"{self.default_casify_sql_function()}({self.column_metadata_catalog_column()}) = '{self.default_casify_system_name(self.database)}'"
        return catalog_filter

    def sql_get_tables_columns_metadata(
        self,
        include_patterns: list[dict[str, str]] | None = None,
        exclude_patterns: list[dict[str, str]] | None = None,
        table_names_only: bool = False,
    ) -> str:
        filter_clauses = []

        if include_patterns and len(include_patterns) > 0:
            include_sql_filter_clauses = self.create_table_column_sql_filters(
                include_patterns, table_names_only=table_names_only
            )
            include_filter = " OR ".join(include_sql_filter_clauses)
            filter_clauses.append(f"({include_filter})")

        if exclude_patterns and len(exclude_patterns) > 0:
            exclude_sql_filter_clauses = [
                f"NOT {sql_filter_clause}"
                for sql_filter_clause in self.create_table_column_sql_filters(
                    exclude_patterns, table_names_only=table_names_only
                )
            ]
            exclude_filter = " AND ".join(exclude_sql_filter_clauses)
            filter_clauses.append(f"({exclude_filter})")

        if self.database:
            catalog_filter = self.catalog_column_filter()
            if catalog_filter:
                filter_clauses.append(catalog_filter)

        if hasattr(self, "schema") and self.schema:
            filter_clauses.append(
                f"{self.default_casify_sql_function()}({self.column_metadata_schema_name()}) = '{self.default_casify_system_name(self.schema)}'"
            )

        where_filter = " \n  AND ".join(filter_clauses)

        # compose query template
        # NOTE: we use `order by ordinal_position` to guarantee stable orders of columns
        # (see https://www.postgresql.org/docs/current/infoschema-columns.html)
        # this mainly has an advantage in testing but bears very little as to how Soda Cloud
        # displays those columns as they are ordered alphabetically in the UI.

        if table_names_only:
            metadata_columns = f"{self.column_metadata_table_name()}"
            information_schema_table = self.sql_information_schema_tables()
            order_by_clause = ""
        else:
            metadata_columns = ", ".join(self.tables_columns_metadata())
            information_schema_table = self.sql_information_schema_columns()
            order_by_clause = f"\nORDER BY {self.get_ordinal_position_name()}"

        sql = (
            f"SELECT {metadata_columns} \n"
            f"FROM {information_schema_table} \n"
            f"WHERE {where_filter}"
            f"{order_by_clause}"
        )

        return sql

    ############################################
    # For a table, get the columns metadata
    ############################################

    @lru_cache(maxsize=None)
    def get_table_columns(
        self,
        table_name: str,
        query_name: str,
        included_columns: list[str] | None = None,
        excluded_columns: list[str] | None = None,
    ) -> dict[str, str] | None:
        """
        :return: A dict mapping column names to data source data types.  Like eg
        {"id": "varchar", "cst_size": "int8", ...}
        """
        query = Query(
            data_source_scan=self.data_source_scan,
            unqualified_query_name=query_name,
            sql=self.sql_get_table_columns(
                table_name, included_columns=included_columns, excluded_columns=excluded_columns
            ),
        )
        query.execute()
        if query.rows and len(query.rows) > 0:
            return {row[0]: row[1] for row in query.rows}
        return None

    def create_table_columns_query(self, partition: Partition, schema_metric: SchemaMetric) -> TableColumnsQuery:
        return TableColumnsQuery(partition, schema_metric)

    def get_ordinal_position_name(self) -> str:
        return "ORDINAL_POSITION"

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
        filter_clauses = [f"{casify_function}(table_name) = '{unquoted_table_name_default_case}'"]

        if self.database:
            filter_clauses.append(
                f"{casify_function}({self.column_metadata_catalog_column()}) = '{self.default_casify_system_name(self.database)}'"
            )

        if self.schema:
            filter_clauses.append(f"{casify_function}(table_schema) = '{self.default_casify_system_name(self.schema)}'")

        if included_columns:
            include_clauses = []
            for col in included_columns:
                include_clauses.append(f"{casify_function}(column_name) LIKE {casify_function}('{col}')")
            include_causes_or = " OR ".join(include_clauses)
            filter_clauses.append(f"({include_causes_or})")

        if excluded_columns:
            for col in excluded_columns:
                filter_clauses.append(f"{casify_function}(column_name) NOT LIKE {casify_function}('{col}')")

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

    ############################################
    # Get table names with count in one go
    ############################################

    def sql_get_table_names_with_count(
        self, include_tables: list[str] | None = None, exclude_tables: list[str] | None = None
    ) -> str:
        table_filter_expression = self.sql_table_include_exclude_filter(
            "relname", "schemaname", include_tables, exclude_tables
        )
        where_clause = f"\nWHERE {table_filter_expression} \n" if table_filter_expression else ""
        return f"SELECT relname, n_live_tup \n" f"FROM pg_stat_user_tables" f"{where_clause}"

    def sql_get_table_count(self, table_name: str) -> str:
        return f"SELECT {self.expr_count_all()} from {self.qualified_table_name(table_name)}"

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

        if hasattr(self, "schema") and self.schema and schema_column_name:
            tablename_filter_clauses.append(f"lower({schema_column_name}) = '{self.schema.lower()}'")

        return "\n      AND ".join(tablename_filter_clauses) if tablename_filter_clauses else None

    def sql_find_table_names(
        self,
        filter: str | None = None,
        include_tables: list[str] = [],
        exclude_tables: list[str] = [],
        table_column_name: str = "table_name",
        schema_column_name: str = "table_schema",
    ) -> str:
        sql = f"SELECT {table_column_name} \n" f"FROM {self.sql_information_schema_tables()}"
        where_clauses = []

        if filter:
            where_clauses.append(f"lower({self.default_casify_column_name(table_column_name)}) like '{filter.lower()}'")

        includes_excludes_filter = self.sql_table_include_exclude_filter(
            table_column_name, schema_column_name, include_tables, exclude_tables
        )
        if includes_excludes_filter:
            where_clauses.append(includes_excludes_filter)

        if where_clauses:
            where_clauses_sql = "\n  AND ".join(where_clauses)
            sql += f"\nWHERE {where_clauses_sql}"

        return sql

    def sql_information_schema_tables(self) -> str:
        return "information_schema.tables"

    def sql_information_schema_columns(self) -> str:
        return "information_schema.columns"

    def sql_analyze_table(self, table: str) -> str | None:
        return None

    def sql_get_duplicates_count(
        self,
        column_names: str,
        table_name: str,
        filter: str,
    ) -> str | None:
        sql = dedent(
            f"""
            WITH frequencies AS (
                SELECT COUNT(*) AS frequency
                FROM {table_name}
                WHERE {filter}
                GROUP BY {column_names})
            SELECT count(*)
            FROM frequencies
            WHERE frequency > 1"""
        )

        return sql

    def sql_get_duplicates_aggregated(
        self,
        column_names: str,
        table_name: str,
        filter: str,
        limit: str | None = None,
        invert_condition: bool = False,
        exclude_patterns: list[str] | None = None,
    ) -> str | None:
        main_query_columns = f"{column_names}, frequency" if exclude_patterns else "*"
        sql = dedent(
            f"""
            WITH frequencies AS (
                SELECT {column_names}, COUNT(*) AS frequency
                FROM {table_name}
                WHERE {filter}
                GROUP BY {column_names})
            SELECT {main_query_columns}
            FROM frequencies
            WHERE frequency {'<=' if invert_condition else '>'} 1
            ORDER BY frequency DESC"""
        )

        if limit:
            sql += f"\nLIMIT {limit}"

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

        sql = dedent(
            f"""
            WITH frequencies AS (
                SELECT {column_names}
                FROM {table_name}
                WHERE {filter}
                GROUP BY {column_names}
                HAVING count(*) {'<=' if invert_condition else '>'} 1)
            SELECT {main_query_columns}
            FROM {table_name} main
            JOIN frequencies ON {join}
            """
        )

        if limit:
            sql += f"\nLIMIT {limit}"

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
        sql = dedent(
            f"""
            SELECT {columns}
                FROM {table_name}  SOURCE
                LEFT JOIN {target_table_name} TARGET on {join_condition}
            WHERE {where_condition}"""
        )

        if limit:
            sql += f"\nLIMIT {limit}"

        return sql

    def cast_to_text(self, expr: str) -> str:
        return f"CAST({expr} AS VARCHAR)"

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
                            SELECT {cast_to_text("'frequent_values'")} AS metric_, ROW_NUMBER() OVER(ORDER BY frequency_ DESC) AS index_, value_, frequency_
                            FROM value_frequencies
                            ORDER BY frequency_ desc
                            LIMIT {limit_frequent_values}
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
                            LIMIT {limit_mins_maxs}
                        )"""

            maxs_cte = f"""maxs AS (
                            SELECT {cast_to_text("'maxs'")} AS metric_, ROW_NUMBER() OVER(ORDER BY value_ DESC) AS index_, value_, frequency_
                            FROM value_frequencies
                            WHERE value_ IS NOT NULL
                            ORDER BY value_ DESC
                            LIMIT {limit_mins_maxs}
                        )"""

            return dedent(
                f"""
                    WITH
                        {value_frequencies_cte},
                        {mins_cte},
                        {maxs_cte},
                        {frequent_values_cte},
                        results AS (
                            SELECT * FROM mins
                            {union}
                            SELECT * FROM maxs
                            {union}
                            SELECT * FROM frequent_values
                        )
                    SELECT *
                    FROM results
                    ORDER BY metric_ ASC, index_ ASC
                """
            )

        raise AssertionError("data_type_category must be either 'numeric' or 'text'")

    def sql_union(self):
        return "UNION"

    def profiling_sql_value_frequencies_cte(self, table_name: str, column_name: str) -> str:
        quoted_column_name = self.quote_column(column_name)
        qualified_table_name = self.qualified_table_name(table_name)
        return f"""value_frequencies AS (
                            SELECT {quoted_column_name} AS value_, count(*) AS frequency_
                            FROM {qualified_table_name}
                            WHERE {quoted_column_name} IS NOT NULL
                            GROUP BY {quoted_column_name}
                        )"""

    def profiling_sql_aggregates_numeric(self, table_name: str, column_name: str) -> str:
        column_name = self.quote_column(column_name)
        qualified_table_name = self.qualified_table_name(table_name)
        return dedent(
            f"""
            SELECT
                avg({column_name}) as average
                , sum({column_name}) as sum
                , var_samp({column_name}) as variance
                , stddev_samp({column_name}) as standard_deviation
                , count(distinct({column_name})) as distinct_values
                , sum(case when {column_name} is null then 1 else 0 end) as missing_values
            FROM {qualified_table_name}
            """
        )

    def profiling_sql_aggregates_text(self, table_name: str, column_name: str) -> str:
        column_name = self.quote_column(column_name)
        qualified_table_name = self.qualified_table_name(table_name)
        return dedent(
            f"""
            SELECT
                count(distinct({column_name})) as distinct_values
                , sum(case when {column_name} is null then 1 else 0 end) as missing_values
                , avg(length({column_name})) as avg_length
                , min(length({column_name})) as min_length
                , max(length({column_name})) as max_length
            FROM {qualified_table_name}
            """
        )

    def histogram_sql_and_boundaries(
        self,
        table_name: str,
        column_name: str,
        min_value: int | float,
        max_value: int | float,
        n_distinct: int,
        column_type: str,
    ) -> tuple[str | None, list[int | float]]:
        # TODO: make configurable or derive dynamically based on data quantiles etc.
        max_n_bins = 20
        number_of_bins: int = max(1, min(n_distinct, max_n_bins))
        number_of_intervals: int = number_of_bins - 1

        if min_value >= max_value:
            self.logs.warning(
                "Min of {column_name} on table: {table_name} must be smaller than max value. Min is {min_value}, and max is {max_value}".format(
                    column_name=column_name,
                    table_name=table_name,
                    min_value=min_value,
                    max_value=max_value,
                )
            )
            return None, []

        bin_width = (max_value - min_value) / number_of_intervals

        if bin_width.is_integer() and column_type == "integer":
            bin_width = int(bin_width)
            min_value = int(min_value)
            max_value = int(max_value)

        bins_list = [round(min_value + i * bin_width, 2) for i in range(0, number_of_bins)]

        field_clauses = []
        for i in range(0, number_of_bins):
            lower_bound = "" if i == 0 else f"{bins_list[i]} <= value_"
            upper_bound = "" if i == number_of_bins - 1 else f"value_ < {bins_list[i + 1]}"
            optional_and = "" if lower_bound == "" or upper_bound == "" else " AND "
            field_clauses.append(f"SUM(CASE WHEN {lower_bound}{optional_and}{upper_bound} THEN frequency_ END)")

        fields = ",\n ".join(field_clauses)

        value_frequencies_cte = self.profiling_sql_value_frequencies_cte(table_name, column_name)

        sql = dedent(
            f"""
            WITH
                {value_frequencies_cte}
            SELECT {fields}
            FROM value_frequencies"""
        )
        return sql, bins_list

    def sql_test_connection(self) -> str:
        return "SELECT 1"

    ######################
    # Query Execution
    ######################

    def get_row_counts_all_tables(
        self,
        include_tables: list[str] | None = None,
        exclude_tables: list[str] | None = None,
        query_name: str | None = None,
    ) -> dict[str, int]:
        """
        Returns a dict that maps table names to row counts.
        """
        sql = self.sql_get_table_names_with_count(include_tables=include_tables, exclude_tables=exclude_tables)
        if sql:
            query = Query(
                data_source_scan=self.data_source_scan,
                unqualified_query_name=query_name or "get_row_counts_all_tables",
                sql=sql,
            )
            query.execute()
            return {self._optionally_quote_table_name_from_meta_data(row[0]): row[1] for row in query.rows}

        # Single query to get the metadata not available, get the counts one by one.
        all_tables = self.get_table_names(include_tables=include_tables, exclude_tables=exclude_tables)
        result = {}
        for table_name in all_tables:
            table_count = self.get_table_row_count(table_name)
            if table_count:
                result[table_name] = table_count

        return result

    def get_table_row_count(self, table_name: str) -> int | None:
        """Deprecated, use get_row_counts_all_tables whenever possible."""
        query_name_str = f"get_row_count_{table_name}"
        query = Query(
            data_source_scan=self.data_source_scan,
            unqualified_query_name=query_name_str,
            sql=self.sql_get_table_count(self.quote_table(table_name)),
        )
        query.execute()
        if query.rows:
            return query.rows[0][0]
        return None

    def get_table_names(
        self,
        filter: str | None = None,
        include_tables: list[str] = [],
        exclude_tables: list[str] = [],
        query_name: str | None = None,
    ) -> list[str]:
        if not include_tables and not exclude_tables:
            return []
        sql = self.sql_find_table_names(filter, include_tables, exclude_tables)
        query = Query(
            data_source_scan=self.data_source_scan,
            unqualified_query_name=query_name or "get_table_names",
            sql=sql,
        )
        query.execute()
        table_names = [self._optionally_quote_table_name_from_meta_data(row[0]) for row in query.rows]
        return table_names

    def _optionally_quote_table_name_from_meta_data(self, table_name: str) -> str:
        """
        To be used by all table names coming from metadata queries.  Quotes are added if needed if the table
        doesn't match the default casify rules.  The table_name is returned unquoted if it matches the default
        casify rules.
        """
        # if the table name needs quoting
        if table_name != self.default_casify_table_name(table_name):
            # add the quotes
            return self.quote_table(table_name)
        else:
            # return the bare table name
            return table_name

    def analyze_table(self, table: str):
        if self.sql_analyze_table(table):
            Query(
                data_source_scan=self.data_source_scan,
                unqualified_query_name=f"analyze_{table}",
                sql=self.sql_analyze_table(table),
            ).execute()

    def _create_table_prefix(self):
        """
        Use
            * self.schema
            * self.database
            * self.quote_table(unquoted_table_name)
        to compose the table prefix to be used in Soda Core queries.  The returned table prefix
        should not include the dot (.) and can optionally be None.  Consider quoting as well.
        Examples:
            return None
            return self.schema
            return self.database
            return f'"{self.database}"."{self.schema}"'
        """
        return self.schema

    def update_schema(self, schema_name):
        self.schema = schema_name
        self.table_prefix = self._create_table_prefix()

    def qualified_table_name(self, table_name: str) -> str:
        """
        table_name can be quoted or unquoted
        """
        if self.table_prefix:
            return f"{self.table_prefix}.{table_name}"
        return table_name

    def quote_table_declaration(self, table_name: str) -> str:
        return self.quote_table(table_name=table_name)

    def is_quoted(self, table_name: str) -> bool:
        return (table_name.startswith('"') and table_name.endswith('"')) or (
            table_name.startswith("'") and table_name.endswith("'")
        )

    def quote_table(self, table_name: str) -> str:
        return f'"{table_name}"'

    def quote_column_declaration(self, column_name: str) -> str:
        return self.quote_column(column_name)

    def quote_column(self, column_name: str) -> str:
        return f'"{column_name}"'

    def get_sql_type_for_create_table(self, data_type: str) -> str:
        if data_type in self.SQL_TYPE_FOR_CREATE_TABLE_MAP:
            return self.SQL_TYPE_FOR_CREATE_TABLE_MAP.get(data_type)
        else:
            return data_type

    def get_sql_type_for_schema_check(self, data_type: str) -> str:
        data_source_type = self.SQL_TYPE_FOR_SCHEMA_CHECK_MAP.get(data_type)
        if data_source_type is None:
            raise NotImplementedError(
                f"Data type {data_type} is not mapped in {type(self)}.SQL_TYPE_FOR_SCHEMA_CHECK_MAP"
            )
        return data_source_type

    def literal(self, o: object):
        if o is None:
            return "NULL"
        elif isinstance(o, Number):
            return self.literal_number(o)
        elif isinstance(o, str):
            return self.literal_string(o)
        elif isinstance(o, datetime):
            return self.literal_datetime(o)
        elif isinstance(o, date):
            return self.literal_date(o)
        elif isinstance(o, list) or isinstance(o, set) or isinstance(o, tuple):
            return self.literal_list(o)
        elif isinstance(o, bool):
            return self.literal_boolean(o)
        raise RuntimeError(f"Cannot convert type {type(o)} to a SQL literal: {o}")

    def literal_number(self, value: Number):
        if value is None:
            return None
        return str(value)

    def literal_string(self, value: str):
        if value is None:
            return None
        return "'" + self.escape_string(value) + "'"

    def literal_list(self, l: list):
        if l is None:
            return None
        return "(" + (",".join([self.literal(e) for e in l])) + ")"

    def literal_date(self, date: date):
        date_string = date.strftime("%Y-%m-%d")
        return f"DATE '{date_string}'"

    def literal_datetime(self, datetime: datetime):
        return f"'{datetime.isoformat()}'"

    def literal_boolean(self, boolean: bool):
        return "TRUE" if boolean is True else "FALSE"

    def expr_count_all(self) -> str:
        return "COUNT(*)"

    def expr_count_conditional(self, condition: str):
        return f"COUNT(CASE WHEN {condition} THEN 1 END)"

    def expr_conditional(self, condition: str, expr: str):
        return f"CASE WHEN {condition} THEN {expr} END"

    def expr_count(self, expr):
        return f"COUNT({expr})"

    def expr_distinct(self, expr):
        return f"DISTINCT({expr})"

    def expr_length(self, expr):
        return f"LENGTH({expr})"

    def expr_min(self, expr):
        return f"MIN({expr})"

    def expr_max(self, expr):
        return f"MAX({expr})"

    def expr_avg(self, expr):
        return f"AVG({expr})"

    def expr_sum(self, expr):
        return f"SUM({expr})"

    def expr_regexp_like(self, expr: str, regex_pattern: str):
        return f"REGEXP_LIKE({expr}, '{regex_pattern}')"

    def expr_regex_condition(self, expr: str, condition: str) -> str:
        return condition.format(expr=expr)

    def get_default_format_expression(self, expr: str, format: str, location: Location | None = None) -> str | None:
        default_expression = self.DEFAULT_FORMAT_EXPRESSIONS.get(format)
        if default_expression:
            return self.expr_regex_condition(expr, default_expression)

        default_format = self.DEFAULT_FORMATS.get(format)
        if default_format:
            return self.expr_regexp_like(expr, self.escape_regex(default_format))

        # TODO move this to a validate step between configuration parsing and execution so that it can be validated without running
        self.logs.error(
            f"Format {format} is not supported.",
            location=location,
        )

        return None

    def expr_in(self, left: str, right: str):
        return f"{left} IN {right}"

    def cast_text_to_number(self, column_name, validity_format: str):
        """Cast string to number

        - first regex replace removes extra chars, keeps: "digits + - . ,"
        - second regex changes "," to "."
        - Nullif makes sure that if regexes return empty string then Null is returned instead
        """
        regex = self.escape_regex(r"'[^-0-9\.\,]'")
        return f"CAST(NULLIF(REGEXP_REPLACE(REGEXP_REPLACE({column_name}, {regex}, ''{self.regex_replace_flags()}), ',', '.'{self.regex_replace_flags()}), '') AS {self.SQL_TYPE_FOR_CREATE_TABLE_MAP[DataType.DECIMAL]})"

    def regex_replace_flags(self) -> str:
        return ", 'g'"

    def escape_string(self, value: str):
        return re.sub(r"(\\.)", r"\\\1", value)

    def escape_regex(self, value: str):
        return value

    def get_max_aggregation_fields(self):
        """
        Max number of fields to be aggregated in 1 aggregation query
        """
        return 50

    def connect(self):
        """
        Subclasses use self.data_source_properties to initialize self.connection with a PEP 249 connection

        Any BaseException may be raised in case of errors.
        The caller of this method will catch the exception and add an error log to the scan.
        """
        raise NotImplementedError(f"TODO: Implement {type(self)}.connect()")

    def fetchall(self, sql: str):
        # TODO: Deprecated - not used, use Query object instead.
        try:
            cursor = self.connection.cursor()
            try:
                self.logs.info(f"Query: \n{sql}")
                cursor.execute(sql)
                return cursor.fetchall()
            finally:
                cursor.close()
        except BaseException as e:
            self.logs.error(f"Query error: {e}\n{sql}", exception=e)
            self.query_failed(e)

    def is_connected(self):
        return self.connection is not None

    def disconnect(self) -> None:
        if self.connection:
            self.connection.close()
            # self.connection = None is used in self.is_connected
            self.connection = None

    def commit(self):
        self.connection.commit()

    def query_failed(self, e: BaseException):
        self.rollback()

    def rollback(self):
        self.connection.rollback()

    def default_casify_sql_function(self) -> str:
        """Returns the sql function to use for default casify."""
        return "lower"

    def default_casify_system_name(self, identifier: str) -> str:
        """Formats database/schema/etc identifier to e.g. a default case for a given data source."""
        return identifier.lower()

    def default_casify_table_name(self, identifier: str) -> str:
        """Formats table identifier to e.g. a default case for a given data source."""
        return identifier

    def default_casify_column_name(self, identifier: str) -> str:
        """Formats column identifier to e.g. a default case for a given data source."""
        return identifier

    def default_casify_type_name(self, identifier: str) -> str:
        """Formats type identifier to e.g. a default case for a given data source."""
        return identifier

    def safe_connection_data(self):
        """Return non-critically sensitive connection details.

        Useful for debugging and telemetry.
        """
        # to be overridden by subclass

    def generate_hash_safe(self):
        """Generates a safe hash from non-sensitive connection details.

        Useful for debugging, identifying data sources anonymously and tracing.
        """
        data = self.safe_connection_data()

        return self.hash_data(data)

    def hash_data(self, data) -> str:
        """Hash provided data using a non-reversible hashing algorithm."""
        encoded = json.dumps(data, sort_keys=True).encode()
        return hashlib.sha256(encoded).hexdigest()

    def test(self, sql):
        import logging
        import textwrap

        from soda.sampler.log_sampler import LogSampler
        from soda.sampler.sample_schema import SampleColumn

        cursor = self.connection.cursor()
        try:
            indented_sql = textwrap.indent(text=sql, prefix="  #   ")
            logging.debug(f"  # Query: \n{indented_sql}")
            cursor.execute(sql)
            rows = cursor.fetchall()

            columns = SampleColumn.create_sample_columns(cursor.description, self)
            table, row_count, col_count = LogSampler.pretty_print(rows, columns)
            logging.debug(f"  # Query result: \n{table}")

        except Exception as e:
            logging.error(f"Error: {e}", e)

        finally:
            cursor.close()

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
        limit_str = f"\n LIMIT {limit}" if limit else ""
        sql = f"SELECT \n" f"  {column_name} \n" f"FROM {table_name}{sample_clauses_str}{filter_clauses_str}{limit_str}"
        return sql

    def expr_false_condition(self):
        return "FALSE"
