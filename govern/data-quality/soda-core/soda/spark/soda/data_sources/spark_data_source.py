from __future__ import annotations

import logging
import re
from collections import defaultdict, namedtuple
from datetime import date, datetime
from enum import Enum
from typing import Any

from soda.__version__ import SODA_CORE_VERSION
from soda.common.exceptions import DataSourceConnectionError
from soda.common.logs import Logs
from soda.execution.data_source import DataSource
from soda.execution.data_type import DataType
from soda.execution.query.query import Query

logger = logging.getLogger(__name__)
ColumnMetadata = namedtuple("ColumnMetadata", ["name", "data_type", "is_nullable"])


def hive_connection_function(
    username: str,
    password: str,
    host: str,
    port: str,
    database: str,
    auth_method: str,
    **kwargs,
):
    """
    Connect to hive.

    Parameters
    ----------
    username : str
        The user name
    password : str
        The password
    host: str
        The host.
    port : str
        The port
    database : str
        The databse
    auth_method : str
        The authentication method

    Returns
    -------
    out : hive.Connection
        The hive connection
    """
    from pyhive import hive

    connection = hive.connect(
        username=username,
        password=password,
        host=host,
        port=port,
        database=database,
        auth=auth_method,
    )
    return connection


def _build_odbc_connnection_string(**kwargs: Any) -> str:
    return ";".join([f"{k}={v}" for k, v in kwargs.items()])


def odbc_connection_function(
    driver: str,
    host: str,
    port: str,
    token: str,
    organization: str,
    cluster: str,
    server_side_parameters: dict[str, str],
    **kwargs,
):
    """
    Connect to odbc.

    Parameters
    ----------
    driver : str
        The path to the driver
    host: str
        The host.
    port : str
        The port
    token : str
        The login token
    organization : str
        The organization
    cluster : str
        The cluster
    server_side_parameters : Dict[str]
        The server side parameters

    Returns
    -------
    out : pyobc.Connection
        The connection
    """
    import pyodbc

    http_path = f"/sql/protocolv1/o/{organization}/{cluster}"
    user_agent_entry = f"soda-sql-spark/{SODA_CORE_VERSION} (Databricks)"

    connection_str = _build_odbc_connnection_string(
        DRIVER=driver,
        HOST=host,
        PORT=port,
        UID="token",
        PWD=token,
        HTTPPath=http_path,
        AuthMech=3,
        SparkServerType=3,
        ThriftTransport=2,
        SSL=1,
        UserAgentEntry=user_agent_entry,
        LCaseSspKeyName=0 if server_side_parameters else 1,
        **server_side_parameters,
    )
    connection = pyodbc.connect(connection_str, autocommit=True)
    return connection


def databricks_connection_function(host: str, http_path: str, token: str, database: str, schema: str, **kwargs):
    from databricks import sql

    logging.getLogger("databricks.sql").setLevel(logging.INFO)
    connection = sql.connect(
        server_hostname=host,
        catalog=database,
        schema=schema,
        http_path=http_path,
        access_token=token,
    )
    return connection


class SparkConnectionMethod(str, Enum):
    HIVE = "hive"
    ODBC = "odbc"
    DATABRICKS = "databricks"


class SparkSQLBase(DataSource):
    SCHEMA_CHECK_TYPES_MAPPING: dict = {
        "string": ["character varying", "varchar"],
        "int": ["integer", "int"],
    }
    SQL_TYPE_FOR_CREATE_TABLE_MAP: dict = {
        DataType.TEXT: "string",
        DataType.INTEGER: "integer",
        DataType.DECIMAL: "double",
        DataType.DATE: "date",
        DataType.TIME: "timestamp",
        DataType.TIMESTAMP: "timestamp",
        DataType.TIMESTAMP_TZ: "timestamp",  # No timezone support in Spark
        DataType.BOOLEAN: "boolean",
    }

    SQL_TYPE_FOR_SCHEMA_CHECK_MAP = {
        DataType.TEXT: "string",
        DataType.INTEGER: "int",
        DataType.DECIMAL: "double",
        DataType.DATE: "date",
        DataType.TIME: "timestamp",
        DataType.TIMESTAMP: "timestamp",
        DataType.TIMESTAMP_TZ: "timestamp",  # No timezone support in Spark
        DataType.BOOLEAN: "boolean",
    }

    NUMERIC_TYPES_FOR_PROFILING = ["integer", "int", "double", "float"]
    TEXT_TYPES_FOR_PROFILING = ["string"]

    def __init__(self, logs: Logs, data_source_name: str, data_source_properties: dict):
        super().__init__(logs, data_source_name, data_source_properties)

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
        columns = {}
        query = Query(
            data_source_scan=self.data_source_scan,
            unqualified_query_name=query_name,
            sql=self.sql_get_table_columns(
                table_name, included_columns=included_columns, excluded_columns=excluded_columns
            ),
        )
        query.execute()
        if len(query.rows) > 0:
            rows = query.rows
            # Remove the partitioning information (see https://spark.apache.org/docs/latest/sql-ref-syntax-aux-describe-table.html)
            partition_indices = [i for i in range(len(rows)) if rows[i][0].startswith("# Partition")]
            if partition_indices:
                rows = rows[: partition_indices[0]]
            columns = {row[0]: row[1] for row in rows}

            if included_columns or excluded_columns:
                column_names = list(columns.keys())
                filtered_column_names = self._filter_include_exclude(column_names, included_columns, excluded_columns)
                columns = {col_name: dtype for col_name, dtype in columns.items() if col_name in filtered_column_names}
        return columns

    def sql_get_table_columns(
        self,
        table_name: str,
        included_columns: list[str] | None = None,
        excluded_columns: list[str] | None = None,
    ):
        return f"DESCRIBE TABLE {table_name}"

    def sql_get_column(self, include_tables: list[str] | None = None, exclude_tables: list[str] | None = None) -> str:
        table_filter_expression = self.sql_table_include_exclude_filter(
            "table_name", "table_schema", include_tables, exclude_tables
        )
        where_clause = f"\nWHERE {table_filter_expression} \n" if table_filter_expression else ""
        return (
            f"SELECT table_name, column_name, data_type, is_nullable \n"
            f"FROM {self.schema}.INFORMATION_SCHEMA.COLUMNS"
            f"{where_clause}"
        )

    def sql_find_table_names(
        self,
        filter: str | None = None,
        include_tables: list[str] = [],
        exclude_tables: list[str] = [],
        table_column_name: str = "table_name",
        schema_column_name: str = "table_schema",
    ) -> str:
        from_clause = f" FROM {self.schema}" if self.schema else ""
        return f"SHOW TABLES{from_clause}"

    def sql_get_table_names_with_count(
        self, include_tables: list[str] | None = None, exclude_tables: list[str] | None = None
    ) -> str:
        return ""

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
            data_source_scan=self.data_source_scan, unqualified_query_name=query_name or "get_table_names", sql=sql
        )
        query.execute()
        table_names = [row[1] for row in query.rows]
        table_names = self._filter_include_exclude(table_names, include_tables, exclude_tables)
        return table_names

    @staticmethod
    def pattern_matches(spark_object_name: str, spark_object_name_pattern: str | None) -> bool:
        if spark_object_name_pattern is None:
            return True
        else:
            pattern_regex = spark_object_name_pattern.replace("%", ".*").lower()
            is_match = re.fullmatch(pattern_regex, spark_object_name.lower())
            return bool(is_match)

    def get_included_table_names(
        self,
        query_name: str,
        include_patterns: list[dict[str, str]],
        exclude_patterns: list[dict[str, str]],
        table_names_only: bool = False,
    ) -> list[str]:
        query = Query(
            data_source_scan=self.data_source_scan,
            unqualified_query_name=query_name,
            sql=self.sql_find_table_names(),
        )
        query.execute()
        table_names = [row[1] for row in query.rows]

        included_table_names = [
            table_name
            for table_name in table_names
            if any(
                self.pattern_matches(table_name, include_pattern["table_name_pattern"])
                for include_pattern in include_patterns
            )
            and not any(
                self.pattern_matches(table_name, exclude_pattern["table_name_pattern"])
                and (exclude_pattern.get("column_name_pattern") == "%" or table_names_only)
                for exclude_pattern in exclude_patterns
            )
        ]

        return included_table_names

    def column_table_pattern_match(
        self, table_name: str, column_name: str, profiling_patterns: list[dict[str, str]]
    ) -> bool:
        column_table_name_pattern_match = any(
            (
                self.pattern_matches(table_name, pattern["table_name_pattern"])
                and self.pattern_matches(column_name, pattern.get("column_name_pattern"))
            )
            for pattern in profiling_patterns
        )
        return column_table_name_pattern_match

    def get_tables_columns_metadata(
        self,
        query_name: str,
        include_patterns: list[dict[str, str]] | None = None,
        exclude_patterns: list[dict[str, str]] | None = None,
        table_names_only: bool = False,
    ) -> dict[str, str] | None:
        if (not include_patterns) and (not exclude_patterns):
            return []
        included_table_names: list[str] = self.get_included_table_names(
            query_name, include_patterns, exclude_patterns, table_names_only=table_names_only
        )
        if table_names_only:
            return included_table_names
        tables_and_columns_metadata = defaultdict(dict)
        for table_name in included_table_names:
            query = Query(
                data_source_scan=self.data_source_scan,
                unqualified_query_name=f"get-tables-columns-metadata-describe-table-{table_name}-spark",
                sql=f"DESCRIBE TABLE {table_name}",
            )
            query.execute()
            columns_metadata = query.rows

            partition_indices = [
                i for i in range(len(columns_metadata)) if columns_metadata[i][0].startswith("# Partition")
            ]
            if partition_indices:
                columns_metadata = columns_metadata[: partition_indices[0]]

            if columns_metadata and len(columns_metadata) > 0:
                for column_name, column_datatype, _ in columns_metadata:
                    column_name_included = self.column_table_pattern_match(table_name, column_name, include_patterns)
                    column_name_excluded = self.column_table_pattern_match(table_name, column_name, exclude_patterns)
                    if column_name_included and not column_name_excluded:
                        tables_and_columns_metadata[table_name][column_name] = column_datatype

        if tables_and_columns_metadata:
            return tables_and_columns_metadata
        else:
            return None

    @staticmethod
    def _filter_include_exclude(
        item_names: list[str], included_items: list[str], excluded_items: list[str]
    ) -> list[str]:
        filtered_names = item_names
        if included_items or excluded_items:

            def matches(name, pattern: str) -> bool:
                pattern_regex = pattern.replace("%", ".*").lower()
                is_match = re.fullmatch(pattern_regex, name.lower())
                return bool(is_match)

            if included_items:
                filtered_names = [
                    filtered_name
                    for filtered_name in filtered_names
                    if any(matches(filtered_name, included_item) for included_item in included_items)
                ]
            if excluded_items:
                filtered_names = [
                    filtered_name
                    for filtered_name in filtered_names
                    if all(not matches(filtered_name, excluded_item) for excluded_item in excluded_items)
                ]
        return filtered_names

    def default_casify_table_name(self, identifier: str) -> str:
        return identifier.lower()

    def rollback(self):
        # Spark does not have transactions so do nothing here.
        pass

    def literal_date(self, date: date):
        date_string = date.strftime("%Y-%m-%d")
        return f"date'{date_string}'"

    def literal_datetime(self, datetime: datetime):
        formatted = datetime.strftime("%Y-%m-%d %H:%M:%S")
        return f"timestamp'{formatted}'"

    def expr_regexp_like(self, expr: str, regex_pattern: str):
        return f"{expr} rlike('{regex_pattern}')"

    def escape_regex(self, value: str):
        return re.sub(r"(\\.)", r"\\\1", value)

    def quote_table(self, table_name) -> str:
        return f"`{table_name}`"

    def quote_column(self, column_name: str) -> str:
        return f"`{column_name}`"

    def regex_replace_flags(self) -> str:
        return ""

    def safe_connection_data(self):
        """TODO: implement for spark."""


class SparkDataSource(SparkSQLBase):
    TYPE = "spark"

    def __init__(self, logs: Logs, data_source_name: str, data_source_properties: dict):
        super().__init__(logs, data_source_name, data_source_properties)

        self.method = data_source_properties.get("method", "hive")
        self.host = data_source_properties.get("host", "localhost")
        self.http_path = data_source_properties.get("http_path", "http_path")
        self.token = data_source_properties.get("token")
        self.port = data_source_properties.get("port", "10000")
        self.username = data_source_properties.get("username")
        self.password = data_source_properties.get("password")
        self.database = data_source_properties.get("catalog", "default")
        self.schema = data_source_properties.get("schema", "default")
        self.auth_method = data_source_properties.get("authentication", None)
        self.configuration = data_source_properties.get("configuration", {})
        self.driver = data_source_properties.get("driver", None)
        self.organization = data_source_properties.get("organization", None)
        self.cluster = data_source_properties.get("cluster", None)
        self.server_side_parameters = {
            f"SSP_{k}": f"{{{v}}}" for k, v in data_source_properties.get("server_side_parameters", {})
        }

    def connect(self):
        if self.method == SparkConnectionMethod.HIVE:
            connection_function = hive_connection_function
        elif self.method == SparkConnectionMethod.ODBC:
            connection_function = odbc_connection_function
        elif self.method == SparkConnectionMethod.DATABRICKS:
            connection_function = databricks_connection_function
        else:
            raise NotImplementedError(f"Unknown Spark connection method {self.method}")

        try:
            connection = connection_function(
                username=self.username,
                password=self.password,
                host=self.host,
                port=self.port,
                database=self.database,
                auth_method=self.auth_method,
                driver=self.driver,
                token=self.token,
                schema=self.schema,
                http_path=self.http_path,
                organization=self.organization,
                cluster=self.cluster,
                server_side_parameters=self.server_side_parameters,
            )

            self.connection = connection
        except Exception as e:
            raise DataSourceConnectionError(self.type, e)
