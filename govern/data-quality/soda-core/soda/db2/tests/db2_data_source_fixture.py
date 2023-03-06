from __future__ import annotations

import logging
import os

from helpers.data_source_fixture import DataSourceFixture

logger = logging.getLogger(__name__)


class Db2DataSourceFixture(DataSourceFixture):
    def __init__(self, test_data_source: str):
        super().__init__(test_data_source=test_data_source)

    def _build_configuration_dict(self, schema_name: str | None = None) -> dict:
        return {
            "data_source db2": {
                "type": "db2",
                "host": "localhost",
                "port": 50000,
                "username": os.getenv("DB2_USERNAME", "db2inst1"),
                "password": os.getenv("DB2_PASSWORD", "password"),
                "database": os.getenv("DB2_DATABASE", "testdb"),
                "schema": schema_name if schema_name else os.getenv("DB2_SCHEMA", "DB2INST1"),
            }
        }

    def _create_schema_if_not_exists_sql(self):
        return f"""
            BEGIN
                DECLARE CONTINUE HANDLER FOR SQLSTATE '42710' BEGIN END ;
                EXECUTE IMMEDIATE 'CREATE SCHEMA "{self.schema_name.upper()}"';
            END;
        """

    def _drop_schema_if_exists_sql(self):
        return f"""
            CREATE OR REPLACE VARIABLE ERROR_SCHEMA VARCHAR(128) DEFAULT 'SYSTOOLS';
            CREATE OR REPLACE VARIABLE ERROR_TAB    VARCHAR(128) DEFAULT 'ADS_ERRORS';
            DROP TABLE IF EXISTS SYSTOOLS.ADS_ERRORS;
            CALL ADMIN_DROP_SCHEMA('{self.schema_name.upper()}', NULL, ERROR_SCHEMA, ERROR_TAB);
        """
