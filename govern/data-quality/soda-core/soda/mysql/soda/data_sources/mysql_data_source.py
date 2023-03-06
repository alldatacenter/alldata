from __future__ import annotations

import logging
import re

import mysql.connector
from soda.common.exceptions import DataSourceConnectionError
from soda.common.logs import Logs
from soda.execution.data_source import DataSource
from soda.execution.data_type import DataType

logger = logging.getLogger(__name__)


class MySQLDataSource(DataSource):
    TYPE = "mysql"

    SCHEMA_CHECK_TYPES_MAPPING: dict = {"TEXT": ["text", "varchar", "char"]}

    SQL_TYPE_FOR_CREATE_TABLE_MAP: dict = {
        DataType.TEXT: "varchar(255)",
        DataType.INTEGER: "int",
        DataType.DECIMAL: "float",
        DataType.DATE: "date",
        DataType.TIME: "time",
        DataType.TIMESTAMP: "timestamp",
        DataType.TIMESTAMP_TZ: "timestamp",
        DataType.BOOLEAN: "boolean",
    }

    SQL_TYPE_FOR_SCHEMA_CHECK_MAP: dict = {
        DataType.TEXT: "varchar",
        DataType.INTEGER: "int",
        DataType.DECIMAL: "float",
        DataType.DATE: "date",
        DataType.TIME: "time",
        DataType.TIMESTAMP: "timestamp",
        DataType.TIMESTAMP_TZ: "timestamp",
        DataType.BOOLEAN: "boolean",
    }
    NUMERIC_TYPES_FOR_PROFILING = [
        "bigint",
        "numeric",
        "bit",
        "smallint",
        "decimal",
        "smallmoney",
        "int",
        "tinyint",
        "money",
        "float",
        "real",
    ]

    TEXT_TYPES_FOR_PROFILING = ["char", "varchar", "text"]

    def __init__(self, logs: Logs, data_source_name: str, data_source_properties: dict):
        super().__init__(logs, data_source_name, data_source_properties)

        self.host = data_source_properties.get("host", "localhost")
        self.port = data_source_properties.get("port", "3306")
        self.username = data_source_properties.get("username")
        self.password = data_source_properties.get("password")
        self.database = data_source_properties.get("database")
        # Override the formats
        self.DEFAULT_FORMATS.update(
            {
                "email": r"^[a-zA-ZÀ-ÿĀ-ſƀ-ȳ0-9.\\-_%+]+@[a-zA-ZÀ-ÿĀ-ſƀ-ȳ0-9.\-_%]+\\.[A-Za-z]{2,4}$",
                "percentage": r"^[[:blank:]]*[-+]?[[:blank:]]*([[:digit:]]+([\.,][[:digit:]]+)?|([\.,][[:digit:]]+))[[:blank:]]*%[[:blank:]]*$",
                "decimal": r"^[[:blank:]]*[-+]?[[:blank:]]*[[:digit:]]*[\.,]*[[:digit:]]*[[:blank:]]*$",
                "integer": r"^[[:blank:]]*[-+]?[[:blank:]]*[[:digit:]]+$",
                "positive integer": r"^[[:blank:]]*[+]?[[:blank:]]*[[:digit:]]+$",
                "negative integer": r"^[[:blank:]]*(-[[:blank:]]*[[:digit:]]+|0)[[:blank:]]*$",
                "date iso 8601": r"^ *(19|20)[[:digit:]][[:digit:]]-?((0[0-9]|1[12])-?([012][0-9]|3[01])|W[0-5][[:digit:]](-?[1-7])?|[0-3][[:digit:]][[:digit:]])([ T](0[0-9]|1[012])(:?[0-5][0-9](:?[0-5][0-9]([.,][[:digit:]]+)?)?)?([+-](0[0-9]|1[012]):?[0-5][0-9]|Z)?)? *$",
            }
        )

    def connect(self):
        try:
            self.connection = mysql.connector.connect(
                user=self.username, password=self.password, host=self.host, port=self.port, database=self.database
            )
            return self.connection
        except Exception as e:
            raise DataSourceConnectionError(self.TYPE, e)

    def validate_configuration(self, logs: Logs) -> None:
        pass

    def safe_connection_data(self):
        return [self.type, self.host, self.port, self.database]

    @staticmethod
    def column_metadata_catalog_column() -> str:
        return "table_schema"

    @staticmethod
    def column_metadata_table_name() -> str:
        return "table_name"

    @staticmethod
    def column_metadata_column_name() -> str:
        return "column_name"

    @staticmethod
    def column_metadata_datatype_name() -> str:
        return " CAST(data_type AS CHAR) "

    def quote_table(self, table_name: str) -> str:
        return f"{table_name}"

    def _create_table_prefix(self):
        return self.schema

    @staticmethod
    def column_metadata_columns() -> list:
        return ["column_name ", " CAST(data_type AS CHAR) ", "is_nullable"]

    def cast_to_text(self, expr: str) -> str:
        return f"CAST({expr} AS CHAR)"

    def quote_column(self, column_name: str) -> str:
        return column_name

    def regex_replace_flags(self) -> str:
        return ""

    def expr_regexp_like(self, expr: str, pattern: str):
        return f"{expr} RLIKE '{pattern}'"

    def default_casify_sql_function(self) -> str:
        return ""

    def default_casify_system_name(self, identifier: str) -> str:
        return identifier

    def escape_regex(self, value: str):
        return re.sub(r"(\\.)", r"\\\1", value)
