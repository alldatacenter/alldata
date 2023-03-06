from __future__ import annotations

import json
import logging

from google.auth import default, impersonated_credentials
from google.cloud import bigquery
from google.cloud.bigquery import dbapi
from google.oauth2.service_account import Credentials
from soda.common.exceptions import DataSourceConnectionError
from soda.common.file_system import file_system
from soda.common.logs import Logs
from soda.execution.data_source import DataSource
from soda.execution.data_type import DataType

logger = logging.getLogger(__name__)


class BigQueryDataSource(DataSource):
    TYPE = "bigquery"

    SCHEMA_CHECK_TYPES_MAPPING: dict = {
        "STRING": ["character varying", "varchar", "text"],
        "INT64": ["integer", "int"],
        "NUMERIC": ["decimal"],
        "TIMESTAMP": ["timestamptz"],
    }
    SQL_TYPE_FOR_CREATE_TABLE_MAP: dict = {
        DataType.TEXT: "STRING",
        DataType.INTEGER: "INT64",
        DataType.DECIMAL: "NUMERIC",
        DataType.DATE: "DATE",
        DataType.TIME: "TIME",
        DataType.TIMESTAMP: "TIMESTAMP",
        DataType.TIMESTAMP_TZ: "TIMESTAMP",
        DataType.BOOLEAN: "BOOL",
    }

    SQL_TYPE_FOR_SCHEMA_CHECK_MAP = {
        DataType.TEXT: "STRING",
        DataType.INTEGER: "INT64",
        DataType.DECIMAL: "NUMERIC",
        DataType.DATE: "DATE",
        DataType.TIME: "TIME",
        DataType.TIMESTAMP: "TIMESTAMP",
        DataType.TIMESTAMP_TZ: "TIMESTAMP",
        DataType.BOOLEAN: "BOOL",
    }

    NUMERIC_TYPES_FOR_PROFILING = [
        "NUMERIC",
        "INT64",
        "INT",
        "SMALLINT",
        "INTEGER",
        "BIGINT",
        "TINYINT",
        "DECIMAL",
        "BIGNUMERIC",
        "BIGDECIMAL",
        "FLOAT64",
    ]
    TEXT_TYPES_FOR_PROFILING = ["STRING"]

    def __init__(self, logs: Logs, data_source_name: str, data_source_properties: dict):
        super().__init__(logs, data_source_name, data_source_properties)

        # Authentication parameters
        self.account_info_dict = None

        account_info_json_str = None
        account_info_path = self.data_source_properties.get("account_info_json_path")
        if account_info_path:
            if file_system().is_file(account_info_path):
                account_info_json_str = file_system().file_read_as_str(account_info_path)
            else:
                logger.error(f"File not found: account_info_json_path: {account_info_path} ")
        else:
            account_info_json_str = self.data_source_properties.get("account_info_json")

        if account_info_json_str:
            self.account_info_dict = json.loads(account_info_json_str)

        default_auth_scopes = [
            "https://www.googleapis.com/auth/bigquery",
            "https://www.googleapis.com/auth/cloud-platform",
            "https://www.googleapis.com/auth/drive",
        ]
        self.auth_scopes = data_source_properties.get("auth_scopes", default_auth_scopes)

        self.credentials = None
        self.project_id = None

        if self.account_info_dict:
            self.credentials = Credentials.from_service_account_info(
                self.account_info_dict,
                scopes=self.auth_scopes,
            )
            self.project_id = self.account_info_dict.get("project_id")

        if self.data_source_properties.get("use_context_auth") or self.account_info_dict is None:
            self.logs.info("Using application default credentials.")
            self.credentials, self.project_id = default()

        if self.data_source_properties.get("impersonation_account"):
            self.logs.info("Using impersonation of Service Account.")
            self.credentials = impersonated_credentials.Credentials(
                source_credentials=self.credentials,
                target_principal=str(self.data_source_properties.get("impersonation_account")),
                target_scopes=self.auth_scopes,
            )

        # Users can optionally overwrite in the connection properties
        self.project_id = data_source_properties.get("project_id", self.project_id)

        self.dataset = data_source_properties.get("dataset")

        self.location = data_source_properties.get("location")
        self.client_info = data_source_properties.get("client_info")
        self.client_options = data_source_properties.get("client_options")

        # Allow to separate default dataset location from compute (project_id).
        self.storage_project_id = self.project_id
        storage_project_id = data_source_properties.get("storage_project_id")
        if storage_project_id:
            self.storage_project_id = storage_project_id

    def connect(self):
        try:
            self.client = bigquery.Client(
                project=self.project_id,
                credentials=self.credentials,
                default_query_job_config=bigquery.QueryJobConfig(
                    default_dataset=f"{self.storage_project_id}.{self.dataset}",
                ),
                location=self.location,
                client_info=self.client_info,
                client_options=self.client_options,
            )
            self.connection = dbapi.Connection(self.client)

            return self.connection
        except Exception as e:
            raise DataSourceConnectionError(self.TYPE, e)

    def sql_get_table_columns(
        self,
        table_name: str,
        included_columns: list[str] | None = None,
        excluded_columns: list[str] | None = None,
    ):
        included_columns_filter = ""
        excluded_columns_filter = ""
        if included_columns:
            for col in included_columns:
                included_columns_filter += f"\n AND lower(column_name) LIKE lower('{col}')"

        if excluded_columns:
            for col in excluded_columns:
                excluded_columns_filter += f"\n AND lower(column_name) NOT LIKE lower('{col}')"

        sql = (
            f"SELECT column_name, data_type, is_nullable "
            f"FROM {self.sql_information_schema_columns()} "
            f"WHERE table_name = '{table_name}'"
            f"{included_columns_filter}"
            f"{excluded_columns_filter}"
            ";"
        )
        return sql

    def sql_get_column(self, include_tables: list[str] | None = None, exclude_tables: list[str] | None = None) -> str:
        table_filter_expression = self.sql_table_include_exclude_filter(
            "table_name", "table_schema", include_tables, exclude_tables
        )
        where_clause = f"\nWHERE {table_filter_expression} \n" if table_filter_expression else ""
        return (
            f"SELECT table_name, column_name, data_type, is_nullable \n"
            f"FROM {self.sql_information_schema_tables()}"
            f"{where_clause}"
        )

    def sql_get_table_names_with_count(
        self, include_tables: list[str] | None = None, exclude_tables: list[str] | None = None
    ) -> str:
        table_filter_expression = self.sql_table_include_exclude_filter(
            "table_id", "dataset_id", include_tables, exclude_tables
        )
        where_clause = f"\nWHERE {table_filter_expression} \n" if table_filter_expression else ""
        return f"SELECT table_id, row_count \n" f"FROM {self.schema}.__TABLES__" f"{where_clause}"

    def quote_table(self, table_name) -> str:
        return f"`{table_name}`"

    def quote_column(self, column_name: str) -> str:
        return f"`{column_name}`"

    def escape_regex(self, value: str):
        if value.startswith("r'") or value.startswith('r"'):
            return value
        if value.startswith("'") or value.startswith('"'):
            return f"r{value}"

        return f"r'{value}'"

    def expr_regexp_like(self, expr: str, regex_pattern: str):
        return f"REGEXP_CONTAINS({expr}, {regex_pattern})"

    def regex_replace_flags(self) -> str:
        return ""

    def get_metric_sql_aggregation_expression(self, metric_name: str, metric_args: list[object] | None, expr: str):
        # TODO add all of these bigquery specific statistical aggregate functions: https://cloud.google.com/bigquery/docs/reference/standard-sql/aggregate_analytic_functions
        if metric_name in [
            "stddev",
            "stddev_pop",
            "stddev_samp",
            "variance",
            "var_pop",
            "var_samp",
        ]:
            return f"{metric_name.upper()}({expr})"
        return super().get_metric_sql_aggregation_expression(metric_name, metric_args, expr)

    def sql_information_schema_tables(self) -> str:
        return f"{self.dataset}.INFORMATION_SCHEMA.TABLES"

    def sql_information_schema_columns(self) -> str:
        return f"{self.dataset}.INFORMATION_SCHEMA.COLUMNS"

    def default_casify_type_name(self, identifier: str) -> str:
        return identifier.upper()

    def safe_connection_data(self):
        return [
            self.type,
            self.data_source_properties.get("project_id"),
        ]

    def rollback(self):
        pass

    def cast_to_text(self, expr: str) -> str:
        return f"CAST({expr} AS STRING)"

    def sql_union(self):
        return "UNION ALL"
