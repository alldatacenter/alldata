from __future__ import annotations

import logging
import os

from helpers.data_source_fixture import DataSourceFixture
from helpers.test_table import TestTable

logger = logging.getLogger(__name__)


class TeradataDataSourceFixture(DataSourceFixture):
    def _build_configuration_dict(self, schema_name: str | None = None) -> dict:
        return {
            "data_source teradata": {
                "type": "teradata",
                "host": os.getenv("TERADATA_HOST", "localhost"),
                "user": os.getenv("TERADATA_USERNAME", "dbc"),
                "password": os.getenv("TERADATA_PASSWORD", "dbc"),
                "database": os.getenv("TERADATA_DATABASE", "sodacore"),
            }
        }

    def _create_schema_name(self):
        return None

    def _create_schema_if_not_exists(self):
        # Database should be exists. No schemas in Teradata
        pass

    def _drop_schema_if_exists(self):
        # Nothing to drop. No schemas in Teradata
        pass

    def _drop_test_table_sql(self, table_name):
        qualified_table_name = self.data_source.qualified_table_name(table_name)
        return f"DROP TABLE {qualified_table_name}"

    def _create_test_table_sql_compose(self, qualified_table_name, columns_sql) -> str:
        return f"CREATE MULTISET TABLE {qualified_table_name} ( \n{columns_sql} \n) NO PRIMARY INDEX"

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

            return "".join(
                [
                    f"""INSERT INTO {qualified_table_name}
                                  VALUES ({sql_test_table_row(row)})\n;"""
                    for row in test_table.values
                ]
            )
