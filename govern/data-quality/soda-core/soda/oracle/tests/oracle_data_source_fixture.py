from __future__ import annotations

import logging
import os

from helpers.data_source_fixture import DataSourceFixture
from helpers.test_table import TestTable

logger = logging.getLogger(__name__)


class OracleDataSourceFixture(DataSourceFixture):
    def _build_configuration_dict(self, schema_name: str | None = None) -> dict:
        return {
            "data_source oracle": {
                "type": "oracle",
                "username": os.getenv("ORACLE_USERNAME", "sodacore"),
                "password": os.getenv("ORACLE_PASSWORD", "password123"),
                "connectstring": os.getenv("ORACLE_CONNECTSTRING", "localhost:1521/xepdb1"),
            }
        }

    def __init__(self, test_data_source: str):
        super().__init__(test_data_source)

    def _create_schema_if_not_exists_sql(self):
        return "select * from dual"

    def _drop_schema_if_exists_sql(self):
        return "select * from dual"

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

            test_table_columns = ", ".join([c.name for c in test_table.test_columns])
            rows_sql = "\n".join(
                [
                    f" INTO {qualified_table_name} ({test_table_columns}) VALUES ( {sql_test_table_row(row)})"
                    for row in test_table.values
                ]
            )
            return f"INSERT ALL {rows_sql} \n SELECT 1 FROM DUAL"

    # def _create_schema_if_not_exists_sql(self):
    #     return f"""
    #     declare
    #     userexist integer;
    #     begin
    #         select count(*) into userexist from dba_users where username='{self.schema_name}';
    #         if (userexist = 0) then
    #             execute immediate 'create user {self.schema_name}';
    #         end if;
    #     end;
    #     """
    #
    # def _drop_schema_if_exists_sql(self):
    #     return f"""
    #     declare
    #     userexist integer;
    #     begin
    #         select count(*) into userexist from dba_users where username = '{self.schema_name}';
    #         if (userexist = 1) then
    #             execute immediate 'drop user {self.schema_name} cascade';
    #         end if;
    #     end;
    #     """
