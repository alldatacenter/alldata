from __future__ import annotations

import os

from helpers.data_source_fixture import DataSourceFixture
from helpers.test_table import TestTable


class VerticaDataSourceFixture(DataSourceFixture):
    def __init__(self, test_data_source: str):
        super().__init__(test_data_source=test_data_source)

    def _build_configuration_dict(self, schema_name: str | None = None) -> dict:
        return {
            "data_source vertica": {
                "type": "vertica",
                "host": "localhost",
                "username": os.getenv("VERTICA_USERNAME", "dbadmin"),
                "password": os.getenv("VERTICA_PASSWORD", "password"),
                "database": os.getenv("VERTICA_DATABASE", "vmart"),
                "schema": schema_name if schema_name else os.getenv("VERTICA_SCHEMA", "public"),
            }
        }

    def _create_schema_if_not_exists_sql(self):
        return f"CREATE SCHEMA IF NOT EXISTS {self.schema_name}"

    def _drop_schema_if_exists_sql(self):
        return f"DROP SCHEMA IF EXISTS {self.schema_name} CASCADE"

    def _insert_test_table_sql(self, test_table: TestTable) -> str:
        if test_table.values:
            quoted_table_name = (
                self.data_source.quote_table(test_table.unique_table_name)
                if test_table.quote_names
                else test_table.unique_table_name
            )

            qualified_table_name = self.data_source.qualified_table_name(quoted_table_name)

            def sql_test_table_row(row):
                return ",".join(self.data_source.literal(value) for value in row)

            insert_statements = []

            for row in test_table.values:
                record = ",".join(self.data_source.literal(value) for value in row)

                insert_statements.append(f"INSERT INTO {qualified_table_name} VALUES ({record});")

            return "\n".join(insert_statements)
