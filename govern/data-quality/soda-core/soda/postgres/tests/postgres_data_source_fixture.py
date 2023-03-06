from __future__ import annotations

import logging
import os

from helpers.data_source_fixture import DataSourceFixture

logger = logging.getLogger(__name__)


class PostgresDataSourceFixture(DataSourceFixture):
    def __init__(self, test_data_source: str):
        super().__init__(test_data_source=test_data_source)
        is_local_dev = os.getenv("GITHUB_ACTIONS") is None
        is_schema_reuse_disabled = os.getenv("POSTGRES_REUSE_SCHEMA", "").lower() == "disabled"
        self.local_dev_schema_reused = is_local_dev and not is_schema_reuse_disabled

    def _build_configuration_dict(self, schema_name: str | None = None) -> dict:
        return {
            "data_source postgres": {
                "type": "postgres",
                "host": "localhost",
                "username": os.getenv("POSTGRES_USERNAME", "sodasql"),
                "password": os.getenv("POSTGRES_PASSWORD"),
                "database": os.getenv("POSTGRES_DATABASE", "sodasql"),
                "schema": schema_name if schema_name else os.getenv("POSTGRES_SCHEMA", "public"),
            }
        }

    def _test_session_starts(self):
        if self.local_dev_schema_reused:
            self.schema_data_source = self._create_schema_data_source()
            self._create_schema_if_not_exists()
            self.data_source = self.schema_data_source
            self._update(f"SET search_path = {self.schema_name}")
            self.data_source.update_schema(self.schema_name)
        else:
            super()._test_session_starts()

    def _test_session_ends(self):
        if self.local_dev_schema_reused:
            self.data_source.connection.close()
        else:
            super()._test_session_ends()

    def _drop_schema_if_exists(self):
        if not self.local_dev_schema_reused:
            super()._drop_schema_if_exists()

    def _create_schema_if_not_exists_sql(self):
        return f"CREATE SCHEMA IF NOT EXISTS {self.schema_name} AUTHORIZATION CURRENT_USER"

    def _drop_schema_if_exists_sql(self):
        return f"DROP SCHEMA IF EXISTS {self.schema_name} CASCADE"
