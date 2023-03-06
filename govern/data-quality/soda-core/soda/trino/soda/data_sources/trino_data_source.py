#  (c) 2022 Walt Disney Parks and Resorts U.S., Inc.
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

import trino
from soda.common.logs import Logs
from soda.execution.data_source import DataSource
from soda.execution.data_type import DataType

logger = logging.getLogger(__name__)


class TrinoDataSource(DataSource):
    TYPE = "trino"

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

    NUMERIC_TYPES_FOR_PROFILING = ["tinyint", "smallint", "integer", "bigint", "decimal", "double", "real"]
    TEXT_TYPES_FOR_PROFILING = ["char", "varchar"]

    def __init__(self, logs: Logs, data_source_name: str, data_source_properties: dict):
        super().__init__(logs, data_source_name, data_source_properties)
        self.host = data_source_properties.get("host", "localhost")
        self.port = data_source_properties.get("port", "443")
        self.http_scheme = data_source_properties.get("http_scheme", "https")
        self.catalog = data_source_properties.get("catalog")
        self.schema = data_source_properties.get("schema")
        self.username = data_source_properties.get("username")
        self.authType = data_source_properties.get("auth_type", "BasicAuthentication")
        self.password = data_source_properties.get("password")

    def connect(self):
        # Default to BasicAuthentication so we don't break current users.
        if self.authType == "BasicAuthentication":
            self.auth = trino.auth.BasicAuthentication(self.username, self.password)
        elif self.authType == "NoAuthentication":
            # No auth typically should use http. If your connection fails, try 'http_scheme: http' in your data source confguration.
            self.auth = None
        # Add other auth types here as needed
        else:
            logger.error("Unrecognized Trino authentication type.")
            self.connection = None
            return

        self.connection = trino.dbapi.connect(
            # experimental_python_types is required to recieve values in appropriate python data types
            # https://github.com/trinodb/trino-python-client#improved-python-types
            experimental_python_types=True,
            host=self.host,
            port=self.port,
            catalog=self.catalog,
            schema=self.schema,
            user=self.username,
            http_scheme=self.http_scheme,
            auth=self.auth,
        )

    def regex_replace_flags(self) -> str:
        return ""

    def sql_get_table_names_with_count(
        self, include_tables: list[str] | None = None, exclude_tables: list[str] | None = None
    ) -> str:
        table_filter_expression = self.sql_table_include_exclude_filter(
            "table_name", "table_schema", include_tables, exclude_tables
        )
        where_clause = f"AND {table_filter_expression}" if table_filter_expression else ""
        sql = f"""
            SELECT table_name, row_count
            FROM information_schema.tables
            WHERE table_schema != 'information_schema'
            {where_clause}
            """
        return sql

    def safe_connection_data(self):
        return [self.type, self.host, self.port, self.catalog, self.schema]
