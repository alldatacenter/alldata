from __future__ import annotations

import logging

from soda.common.logs import Logs
from soda.execution.data_source import DataSource

logger = logging.getLogger(__name__)


class DenodoDataSource(DataSource):
    TYPE = "denodo"

    def __init__(self, logs: Logs, data_source_name: str, data_source_properties: dict):
        super().__init__(logs, data_source_name, data_source_properties)
        self.host = data_source_properties.get("host")
        self.port = data_source_properties.get("port")
        self.password = data_source_properties.get("password")
        self.username = data_source_properties.get("username")

    def connect(self):
        import psycopg2

        self.connection = psycopg2.connect(
            user=self.username,
            password=self.password,
            host=self.host,
            port=self.port,
            connect_timeout=self.connection_timeout,
            database=self.database,
        )
        return self.connection

    def expr_regexp_like(self, expr: str, excaped_regex_pattern: str):
        return f"{expr} ~ '{excaped_regex_pattern}'"

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
        if metric_name in ["percentile", "percentile_disc"]:
            # TODO ensure proper error if the metric_args[0] is not a valid number
            percentile_fraction = metric_args[1] if metric_args else None
            return f"PERCENTILE_DISC({percentile_fraction}) WITHIN GROUP (ORDER BY {expr})"
        return super().get_metric_sql_aggregation_expression(metric_name, metric_args, expr)

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

    def sql_analyze_table(self, table: str) -> str:
        return f"ANALYZE {table}"

    def validate_configuration(self, logs: Logs) -> None:
        pass
