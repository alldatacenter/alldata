from __future__ import annotations

import logging
import re

from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from snowflake import connector
from snowflake.connector.network import DEFAULT_SOCKET_CONNECT_TIMEOUT
from soda.common.logs import Logs
from soda.execution.data_source import DataSource
from soda.execution.data_type import DataType

logger = logging.getLogger(__name__)


class SnowflakeDataSource(DataSource):
    TYPE = "snowflake"

    SCHEMA_CHECK_TYPES_MAPPING: dict = {
        "TEXT": ["character varying", "varchar", "string"],
        "NUMBER": ["integer", "int"],
        "FLOAT": ["decimal"],
        "TIMESTAMP_NTZ": ["timestamp"],
        "TIMESTAMP_TZ": ["timestamptz"],
    }
    SQL_TYPE_FOR_CREATE_TABLE_MAP: dict = {
        DataType.TEXT: "TEXT",
        DataType.INTEGER: "INT",
        DataType.DECIMAL: "FLOAT",
        DataType.DATE: "DATE",
        DataType.TIME: "TIME",
        DataType.TIMESTAMP: "TIMESTAMP_NTZ",
        DataType.TIMESTAMP_TZ: "TIMESTAMP_TZ",
        DataType.BOOLEAN: "BOOLEAN",
    }

    SQL_TYPE_FOR_SCHEMA_CHECK_MAP = {
        DataType.TEXT: "TEXT",
        DataType.INTEGER: "NUMBER",
        DataType.DECIMAL: "FLOAT",
        DataType.DATE: "DATE",
        DataType.TIME: "TIME",
        DataType.TIMESTAMP: "TIMESTAMP_NTZ",
        DataType.TIMESTAMP_TZ: "TIMESTAMP_TZ",
        DataType.BOOLEAN: "BOOLEAN",
    }

    NUMERIC_TYPES_FOR_PROFILING = [
        "FLOAT",
        "NUMBER",
        "INT",
        "DECIMAL",
        "NUMERIC",
        "INTEGER",
        "BIGINT",
        "SMALLINT",
        "TINYINT",
        "FLOAT4",
        "FLOAT8",
        "REAL",
    ]
    TEXT_TYPES_FOR_PROFILING = [
        "TEXT",
        "VARCHAR",
        "CHAR",
        "CHARACTER",
        "NCHAR",
        "STRING",
        "NVARCHAR",
        "NVARCHAR2",
        "CHAR VARYING",
        "NCHAR VARYING",
    ]

    def __init__(self, logs: Logs, data_source_name: str, data_source_properties: dict):
        super().__init__(logs, data_source_name, data_source_properties)
        self.user = data_source_properties.get("username")
        self.password = data_source_properties.get("password")
        self.token = data_source_properties.get("token")
        self.account = data_source_properties.get("account")
        self.data_source = data_source_properties.get("data_source")
        self.warehouse = data_source_properties.get("warehouse")
        self.login_timeout = data_source_properties.get("connection_timeout", DEFAULT_SOCKET_CONNECT_TIMEOUT)
        self.role = data_source_properties.get("role")
        self.client_session_keep_alive = data_source_properties.get("client_session_keep_alive")
        self.client_store_temporary_credential = data_source_properties.get("client_store_temporary_credential", False)
        self.session_parameters = data_source_properties.get("session_params")

        self.passcode_in_password = data_source_properties.get("passcode_in_password", False)
        self.private_key_passphrase = data_source_properties.get("private_key_passphrase")
        self.private_key = data_source_properties.get("private_key")
        self.private_key_path = data_source_properties.get("private_key_path")
        self.client_prefetch_threads = data_source_properties.get("client_prefetch_threads", 4)
        self.client_session_keep_alive = data_source_properties.get("client_session_keep_alive", False)
        self.authenticator = data_source_properties.get("authenticator", "snowflake")
        self.session_params = data_source_properties.get("session_parameters")

    def connect(self):
        self.connection = connector.connect(
            user=self.user,
            password=self.password,
            token=self.token,
            account=self.account,
            data_source=self.data_source,
            database=self.database,
            schema=self.schema,
            warehouse=self.warehouse,
            login_timeout=self.login_timeout,
            role=self.role,
            client_session_keep_alive=self.client_session_keep_alive,
            client_store_temporary_credential=self.client_store_temporary_credential,
            session_parameters=self.session_parameters,
            passcode_in_password=self.passcode_in_password,
            private_key=self.__get_private_key(),
            client_prefetch_threads=self.client_prefetch_threads,
            authenticator=self.authenticator,
            application="Soda",
        )

    def __get_private_key(self):
        if not (self.private_key_path or self.private_key):
            return None

        if self.private_key_passphrase:
            encoded_passphrase = self.private_key_passphrase.encode()
        else:
            encoded_passphrase = None

        pk_bytes = None
        if self.private_key:
            pk_bytes = self.private_key.encode()
        elif self.private_key_path:
            with open(self.private_key_path, "rb") as pk:
                pk_bytes = pk.read()

        p_key = serialization.load_pem_private_key(pk_bytes, password=encoded_passphrase, backend=default_backend())

        return p_key.private_bytes(
            encoding=serialization.Encoding.DER,
            format=serialization.PrivateFormat.PKCS8,
            encryption_algorithm=serialization.NoEncryption(),
        )

    def escape_regex(self, value: str):
        return re.sub(r"(\\.)", r"\\\1", value)

    def regex_replace_flags(self) -> str:
        return ""

    def expr_regexp_like(self, expr: str, pattern: str):
        return f"REGEXP_LIKE(COLLATE({expr}, ''), '{pattern}')"

    def cast_text_to_number(self, column_name, validity_format: str):
        """Cast string to number
        - first regex replace removes extra chars, keeps: "digits + - . ,"
        - second regex changes "," to "."
        - Nullif makes sure that if regexes return empty string then Null is returned instead
        """
        regex = self.escape_regex(r"'[^-0-9\.\,]'")
        return f"CAST(NULLIF(REGEXP_REPLACE(REGEXP_REPLACE(COLLATE({column_name}, ''), {regex}, ''{self.regex_replace_flags()}), ',', '.'{self.regex_replace_flags()}), '') AS {self.SQL_TYPE_FOR_CREATE_TABLE_MAP[DataType.DECIMAL]})"

    def get_metric_sql_aggregation_expression(self, metric_name: str, metric_args: list[object] | None, expr: str):
        # TODO add all of these snowflake specific statistical aggregate functions: https://docs.snowflake.com/en/sql-reference/functions-aggregation.html
        if metric_name in [
            "stddev",
            "stddev_pop",
            "stddev_samp",
            "variance",
            "var_pop",
            "var_samp",
        ]:
            return f"{metric_name.upper()}({expr})"
        if metric_name in ["percentile", "percentile_disc"]:
            # TODO ensure proper error if the metric_args[0] is not a valid number
            percentile_fraction = metric_args[1] if metric_args else None
            return f"PERCENTILE_DISC({percentile_fraction}) WITHIN GROUP (ORDER BY {expr})"
        return super().get_metric_sql_aggregation_expression(metric_name, metric_args, expr)

    def sql_get_table_names_with_count(
        self, include_tables: list[str] | None = None, exclude_tables: list[str] | None = None
    ) -> str:
        table_filter_expression = self.sql_table_include_exclude_filter(
            "table_name", "table_schema", include_tables, exclude_tables
        )
        where_clause = f"AND {table_filter_expression}" if table_filter_expression else ""
        sql = f"""
            SELECT table_name, row_count
            FROM information_schema.tables
            WHERE table_schema != 'INFORMATION_SCHEMA'
            {where_clause}
            """
        return sql

    def default_casify_sql_function(self) -> str:
        return "upper"

    def default_casify_system_name(self, identifier: str) -> str:
        return identifier.upper()

    def default_casify_table_name(self, identifier: str) -> str:
        return identifier.upper()

    def default_casify_column_name(self, identifier: str) -> str:
        return identifier.upper()

    def default_casify_type_name(self, identifier: str) -> str:
        return identifier.upper()

    def safe_connection_data(self):
        return [
            self.type,
            self.account,
        ]
