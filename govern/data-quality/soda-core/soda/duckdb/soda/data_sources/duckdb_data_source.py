#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#   http://www.apache.org/licenses/LICENSE-2.0
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import logging
from typing import List, Optional

from soda.common.exceptions import DataSourceConnectionError
from soda.common.logs import Logs
from soda.execution.data_source import DataSource
from soda.execution.data_type import DataType

logger = logging.getLogger(__name__)


class DuckDBCursor:
    def __init__(self, connection):
        self._connection = connection

    def __getattr__(self, attr):
        if attr in self.__dict__:
            return getattr(self, attr)
        return getattr(self._connection, attr)

    def close(self):
        # because a duckdb cursor is actually the current connection,
        # we don't want to close it
        pass


class DuckDBDataSourceConnectionWrapper:
    def __init__(self, delegate):
        self._delegate = delegate

    def __getattr__(self, attr):
        if attr in self.__dict__:
            return getattr(self, attr)
        return getattr(self._delegate, attr)

    def cursor(self):
        return DuckDBCursor(self._delegate)


class DuckDBDataSource(DataSource):
    TYPE = "duckdb"

    # Maps synonym types for the convenience of use in checks.
    # Keys represent the data_source type, values are lists of "aliases" that can be used in SodaCL as synonyms.
    SCHEMA_CHECK_TYPES_MAPPING: dict = {
        "character varying": ["varchar"],
        "double precision": ["double"],
        "timestamp without time zone": ["timestamp"],
        "decimal": ["decimal(18,3)"],
    }

    SQL_TYPE_FOR_CREATE_TABLE_MAP: dict = {
        DataType.TEXT: "varchar",
        DataType.INTEGER: "integer",
        DataType.DECIMAL: "decimal",
        DataType.DATE: "date",
        DataType.TIME: "time",
        DataType.TIMESTAMP: "timestamp",
        DataType.TIMESTAMP_TZ: "timestamp with time zone",
        DataType.BOOLEAN: "boolean",
    }

    SQL_TYPE_FOR_SCHEMA_CHECK_MAP = {
        DataType.TEXT: "varchar",
        DataType.INTEGER: "integer",
        DataType.DECIMAL: "decimal(18,3)",
        DataType.DATE: "date",
        DataType.TIME: "time",
        DataType.TIMESTAMP: "timestamp",
        DataType.TIMESTAMP_TZ: "timestamp with time zone",
        DataType.BOOLEAN: "boolean",
    }

    NUMERIC_TYPES_FOR_PROFILING = [
        "tinyint",
        "smallint",
        "integer",
        "bigint",
        "decimal",
        "decimal(18,3)",
        "double",
        "real",
    ]
    TEXT_TYPES_FOR_PROFILING = ["char", "varchar"]

    def __init__(self, logs: Logs, data_source_name: str, data_source_properties: dict):
        super().__init__(logs, data_source_name, data_source_properties)
        self.path = data_source_properties.get("path")
        self.read_only = data_source_properties.get("read_only", False)
        self.duckdb_connection = data_source_properties.get("duckdb_connection")

    def connect(self):
        import duckdb

        try:
            if self.duckdb_connection:
                self.connection = DuckDBDataSourceConnectionWrapper(self.duckdb_connection)
            else:
                self.connection = DuckDBDataSourceConnectionWrapper(
                    duckdb.connect(database=self.path if self.path else ":memory:", read_only=self.read_only)
                )
        except Exception as e:
            raise DataSourceConnectionError(self.TYPE, e)

        return self.connection

    def safe_connection_data(self):
        return [self.path, self.read_only]

    def expr_regexp_like(self, expr: str, regex_pattern: str):
        return f"REGEXP_MATCHES({expr}, '{regex_pattern}')"

    def default_casify_type_name(self, identifier: str) -> str:
        return identifier.lower()

    def default_casify_sql_function(self) -> str:
        return ""

    @staticmethod
    def column_metadata_columns() -> list:
        return ["column_name", "lower(data_type) as data_type", "is_nullable"]

    @staticmethod
    def column_metadata_datatype_name() -> str:
        return "lower(data_type) as data_type"

    def get_metric_sql_aggregation_expression(self, metric_name: str, metric_args: Optional[List[object]], expr: str):
        # https://duckdb.org/docs/sql/aggregates
        if metric_name in [
            "stddev",
            "stddev_pop",
            "stddev_samp",
            "variance",
            "var_pop",
            "var_samp",
        ]:
            return f"{metric_name.upper()}({expr})"
        if metric_name in ["percentile", "percentile_disc"]:
            # TODO ensure proper error if the metric_args[0] is not a valid number
            percentile_fraction = metric_args[1] if metric_args else None
            return f"PERCENTILE_DISC({percentile_fraction}) WITHIN GROUP (ORDER BY {expr})"
        return super().get_metric_sql_aggregation_expression(metric_name, metric_args, expr)
