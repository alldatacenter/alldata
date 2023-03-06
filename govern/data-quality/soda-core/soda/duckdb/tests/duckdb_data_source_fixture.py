from __future__ import annotations

import logging

from helpers.data_source_fixture import DataSourceFixture

logger = logging.getLogger(__name__)


class DuckDBDataSourceFixture(DataSourceFixture):
    def __init__(self, test_data_source: str):
        super().__init__(test_data_source)

    def _build_configuration_dict(self, schema_name: str | None = None) -> dict:
        return {"data_source duckdb": {"type": "duckdb", "path": ":memory:"}}

    def _create_schema_if_not_exists_sql(self) -> str:
        return f"CREATE SCHEMA IF NOT EXISTS {self.schema_name}"

    def _drop_schema_if_exists_sql(self):
        return f"DROP SCHEMA IF EXISTS {self.schema_name}"
