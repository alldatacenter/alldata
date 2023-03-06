from __future__ import annotations

import logging
import os

from helpers.data_source_fixture import DataSourceFixture

logger = logging.getLogger(__name__)


class SparkDataSourceFixture(DataSourceFixture):
    def __init__(self, test_data_source: str):
        super().__init__(test_data_source)

    def _build_configuration_dict(self, schema_name: str | None = None) -> dict:
        return {
            "data_source spark": {
                "type": "spark",
                "host": os.getenv("DATABRICKS_HOST"),
                "method": os.getenv("SPARK_METHOD"),
                "http_path": os.getenv("DATABRICKS_HTTP_PATH"),
                "token": os.getenv("DATABRICKS_TOKEN"),
                "database": os.getenv("DATABRICKS_DATABASE"),
            }
        }

    def _create_schema_if_not_exists_sql(self) -> str:
        return f"CREATE SCHEMA IF NOT EXISTS {self.schema_name}"

    def _use_schema_sql(self) -> str | None:
        return None

    def _drop_schema_if_exists_sql(self):
        return f"DROP SCHEMA IF EXISTS {self.schema_name} CASCADE"
