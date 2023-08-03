#  (c) 2022 Soda Data NV.
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#   http://www.apache.org/licenses/LICENSE-2.0
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

from __future__ import annotations

import logging
from textwrap import dedent

import pyodbc
from soda.common.exceptions import DataSourceConnectionError
from soda.common.logs import Logs
from soda.execution.data_source import DataSource
from soda.execution.data_type import DataType

logger = logging.getLogger(__name__)


class DremioDataSource(DataSource):
    TYPE = "dremio"

    # Maps synonym types for the convenience of use in checks.
    # Keys represent the data_source type, values are lists of "aliases" that can be used in SodaCL as synonyms.
    SCHEMA_CHECK_TYPES_MAPPING: dict = {
        "character varying": ["varchar"],
        "double precision": ["double"],
        "timestamp without time zone": ["timestamp"],
        "timestamp with time zone": ["timestamp with time zone"],
    }

    SQL_TYPE_FOR_CREATE_TABLE_MAP: dict = {
        DataType.TEXT: "varchar",
        DataType.INTEGER: "integer",
        DataType.DECIMAL: "decimal",
        DataType.DATE: "date",
        DataType.TIME: "time(3)",
        DataType.TIMESTAMP: "timestamp(3)",
        DataType.TIMESTAMP_TZ: "timestamp(3) with time zone",
        DataType.BOOLEAN: "boolean",
    }

    SQL_TYPE_FOR_SCHEMA_CHECK_MAP = {
        DataType.TEXT: "varchar",
        DataType.INTEGER: "integer",
        DataType.DECIMAL: "decimal",
        DataType.DATE: "date",
        DataType.TIME: "time(3)",
        DataType.TIMESTAMP: "timestamp(3)",
        DataType.TIMESTAMP_TZ: "timestamp(3) with time zone",
        DataType.BOOLEAN: "boolean",
    }

    NUMERIC_TYPES_FOR_PROFILING = ["INT", "BIGINT", "DECIMAL", "DOUBLE", "FLOAT"]
    TEXT_TYPES_FOR_PROFILING = ["CHAR", "VARCHAR", "CHARACTER VARYING"]

    def __init__(self, logs: Logs, data_source_name: str, data_source_properties: dict):
        super().__init__(logs, data_source_name, data_source_properties)
        self.driver = data_source_properties.get("driver", "Arrow Flight SQL ODBC Driver")
        self.host = data_source_properties.get("host", "localhost")
        self.port = data_source_properties.get("port", "32010")
        self.username = data_source_properties.get("username")
        self.password = data_source_properties.get("password")
        self.schema = data_source_properties.get("schema")
        self.use_encryption = data_source_properties.get("use_encryption", "false")
        self.routing_queue = data_source_properties.get("routing_queue", "")

    def connect(self):
        try:
            self.connection = pyodbc.connect(
                "DRIVER={"
                + self.driver
                + "};HOST="
                + self.host
                + ";PORT="
                + self.port
                + ";UID="
                + self.username
                + ";PWD="
                + self.password
                + ";useEncryption="
                + self.use_encryption
                + ";routing_queue="
                + self.routing_queue,
                autocommit=True,
            )
        except Exception as e:
            raise DataSourceConnectionError(self.TYPE, e)

    def regex_replace_flags(self) -> str:
        return ""

    def default_casify_table_name(self, identifier: str) -> str:
        """Formats table identifier to e.g. a default case for a given data source."""
        return identifier

    def qualified_table_name(self, table_name: str) -> str:
        """
        table_name can be quoted or unquoted
        """
        if self.table_prefix:
            return f'"{self.table_prefix}".{table_name}'
        return table_name

    def safe_connection_data(self):
        return [self.type, self.host, self.port, self.schema]

    def sql_information_schema_tables(self) -> str:
        return 'INFORMATION_SCHEMA."TABLES"'

    def default_casify_column_name(self, column_name: str) -> str:
        return column_name

    def default_casify_sql_function(self) -> str:
        """Returns the sql function to use for default casify."""
        return ""

    def default_casify_system_name(self, identifier: str) -> str:
        """Formats database/schema/etc identifier to e.g. a default case for a given data source."""
        return identifier

    def profiling_sql_aggregates_numeric(self, table_name: str, column_name: str) -> str:
        column_name = self.quote_column(column_name)

        qualified_table_name = self.qualified_table_name(table_name)
        return dedent(
            f"""
            SELECT
                avg({column_name}) as average
                , sum({column_name}) as "sum"
                , variance({column_name}) as "variance"
                , stddev({column_name}) as standard_deviation
                , count(distinct({column_name})) as distinct_values
                , sum(case when {column_name} is null then 1 else 0 end) as missing_values
            FROM {qualified_table_name}
            """
        )
