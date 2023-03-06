from __future__ import annotations

import logging
import os

from helpers.data_source_fixture import DataSourceFixture

logger = logging.getLogger(__name__)


class MySQLDataSourceFixture(DataSourceFixture):
    def _build_configuration_dict(self, schema_name: str | None = None) -> dict:
        return {
            "data_source mysql": {
                "type": "mysql",
                "host": os.getenv("MYSQL_HOST", "localhost"),
                "username": os.getenv("MYSQL_USERNAME", "root"),
                "password": os.getenv("MYSQL_PASSWORD", "sodacore"),
                "database": schema_name if schema_name else os.getenv("MYSQL_DATABASE", "sodacore"),
            }
        }

    def _drop_schema_if_exists(self):
        super()._drop_schema_if_exists()

    def _create_schema_if_not_exists_sql(self):
        return f"CREATE SCHEMA IF NOT EXISTS {self.schema_name}"

    def _drop_schema_if_exists_sql(self):
        return f"DROP SCHEMA IF EXISTS {self.schema_name}"
