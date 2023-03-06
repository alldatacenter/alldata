from __future__ import annotations

import logging
import os

from helpers.data_source_fixture import DataSourceFixture

logger = logging.getLogger(__name__)


class SnowflakeDataSourceFixture(DataSourceFixture):
    def __init__(self, test_data_source: str):
        super().__init__(test_data_source)

    def _build_configuration_dict(self, schema_name: str | None = None) -> dict:
        return {
            "data_source snowflake": {
                "type": "snowflake",
                "host": os.getenv("SNOWFLAKE_HOST"),
                "account": os.getenv("SNOWFLAKE_ACCOUNT"),
                "username": os.getenv("SNOWFLAKE_USERNAME"),
                "password": os.getenv("SNOWFLAKE_PASSWORD"),
                "database": os.getenv("SNOWFLAKE_DATABASE", "sodasql"),
                "schema": schema_name if schema_name else os.getenv("SNOWFLAKE_SCHEMA", "public"),
            }
        }

    def _create_schema_if_not_exists_sql(self) -> str:
        return f"CREATE SCHEMA IF NOT EXISTS {self.schema_name}"

    def _use_schema_sql(self) -> str | None:
        return None

    def _drop_schema_if_exists_sql(self):
        return f"DROP SCHEMA IF EXISTS {self.schema_name} CASCADE"
