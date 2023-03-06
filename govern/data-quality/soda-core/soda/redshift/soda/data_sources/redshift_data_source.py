import logging
import re
from typing import List, Optional

import boto3
import psycopg2
from soda.common.aws_credentials import AwsCredentials
from soda.common.logs import Logs
from soda.execution.data_source import DataSource

logger = logging.getLogger(__name__)


class RedshiftDataSource(DataSource):
    TYPE = "redshift"

    def __init__(self, logs: Logs, data_source_name: str, data_source_properties: dict):
        super().__init__(logs, data_source_name, data_source_properties)

        self.host = data_source_properties.get("host", "localhost")
        self.port = data_source_properties.get("port", "5439")
        self.connect_timeout = data_source_properties.get("connection_timeout_sec")
        self.username = data_source_properties.get("username")
        self.password = data_source_properties.get("password")

        if not self.username or not self.password:
            aws_credentials = AwsCredentials(
                access_key_id=data_source_properties.get("access_key_id"),
                secret_access_key=data_source_properties.get("secret_access_key"),
                role_arn=data_source_properties.get("role_arn"),
                session_token=data_source_properties.get("session_token"),
                region_name=data_source_properties.get("region", "eu-west-1"),
                profile_name=data_source_properties.get("profile_name"),
            )
            self.username, self.password = self.__get_cluster_credentials(aws_credentials)

    def connect(self):
        options = f"-c search_path={self.schema}" if self.schema else None

        self.connection = psycopg2.connect(
            user=self.username,
            password=self.password,
            host=self.host,
            port=self.port,
            connect_timeout=self.connect_timeout,
            database=self.database,
            options=options,
        )

    def __get_cluster_credentials(self, aws_credentials: AwsCredentials):
        resolved_aws_credentials = aws_credentials.resolve_role(
            role_session_name="soda_redshift_get_cluster_credentials"
        )

        client = boto3.client(
            "redshift",
            region_name=resolved_aws_credentials.region_name,
            aws_access_key_id=resolved_aws_credentials.access_key_id,
            aws_secret_access_key=resolved_aws_credentials.secret_access_key,
            aws_session_token=resolved_aws_credentials.session_token,
        )

        cluster_name = self.host.split(".")[0]
        username = self.username
        db_name = self.database
        cluster_creds = client.get_cluster_credentials(
            DbUser=username, DbName=db_name, ClusterIdentifier=cluster_name, AutoCreate=False, DurationSeconds=3600
        )

        return cluster_creds["DbUser"], cluster_creds["DbPassword"]

    def sql_get_table_names_with_count(
        self, include_tables: Optional[List[str]] = None, exclude_tables: Optional[List[str]] = None
    ) -> str:
        table_filter_expression = self.sql_table_include_exclude_filter(
            '"table"', "schema", include_tables, exclude_tables
        )
        where_clause = f"\nWHERE {table_filter_expression} \n" if table_filter_expression else ""
        return f'SELECT "table", tbl_rows \n FROM svv_table_info {where_clause}'

    def expr_regexp_like(self, expr: str, regex_pattern: str):
        return f"{expr} ~ '{regex_pattern}'"

    def escape_regex(self, value: str):
        return re.sub(r"(\\.)", r"\\\1", value)

    def get_metric_sql_aggregation_expression(self, metric_name: str, metric_args: Optional[List[object]], expr: str):
        # TODO add all of these specific statistical aggregate functions: https://docs.aws.amazon.com/redshift/latest/dg/c_Aggregate_Functions.html
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

    def expr_avg(self, expr):
        return f"AVG({expr}::real)"

    def regex_replace_flags(self) -> str:
        return ""

    def default_casify_table_name(self, identifier: str) -> str:
        return identifier.lower()

    def default_casify_column_name(self, identifier: str) -> str:
        return identifier.lower()

    def default_casify_type_name(self, identifier: str) -> str:
        return identifier.lower()

    def safe_connection_data(self):
        return [
            self.type,
            self.host,
            self.port,
            self.database,
        ]

    def sql_information_schema_columns(self) -> str:
        return "SVV_COLUMNS"
