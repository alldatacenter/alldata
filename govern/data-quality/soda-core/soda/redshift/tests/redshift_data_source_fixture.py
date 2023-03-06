from __future__ import annotations

import logging
import os

from helpers.data_source_fixture import DataSourceFixture

logger = logging.getLogger(__name__)


class RedshiftDataSourceFixture(DataSourceFixture):
    def __init__(self, test_data_source: str):
        super().__init__(test_data_source)

    def _build_configuration_dict(self, schema_name: str | None = None) -> dict:
        return {
            "data_source redshift": {
                "type": "redshift",
                "host": os.getenv("REDSHIFT_HOST", "localhost"),
                "port": os.getenv("REDSHIFT_PORT", "5432"),
                "username": os.getenv("REDSHIFT_USERNAME", "soda"),
                "password": os.getenv("REDSHIFT_PASSWORD"),
                "database": os.getenv("REDSHIFT_DATABASE", "sodasql"),
                "schema": schema_name if schema_name else os.getenv("REDSHIFT_SCHEMA", "public"),
            }
        }

    def _create_schema_data_source(self) -> DataSource:
        schema_data_source = super()._create_schema_data_source()
        schema_data_source.connection.set_session(autocommit=True)
        return schema_data_source

    def _drop_schema_if_exists_sql(self) -> str:
        return f"DROP SCHEMA IF EXISTS {self.schema_name} CASCADE"

    def _create_schema_if_not_exists_sql(self) -> str:
        return f"CREATE SCHEMA IF NOT EXISTS {self.schema_name}"
