from __future__ import annotations

from soda.profiling.discover_tables_result_column import DiscoverTablesResultColumn


class DiscoverTablesResultTable:
    def __init__(self, table_name: str, data_source: str):
        self.table_name: str = table_name
        self.data_source: str = data_source
        self.result_columns: list[DiscoverTablesResultColumn] = []

    def create_column(self, column_name: str, column_type: str) -> DiscoverTablesResultColumn:
        column = DiscoverTablesResultColumn(column_name, column_type)
        self.result_columns.append(column)
        return column

    def get_cloud_dict(self) -> dict:
        cloud_dict = {
            "table": self.table_name,
            "dataSource": self.data_source,
            "schema": [result_column.get_cloud_dict() for result_column in self.result_columns],
        }
        return cloud_dict

    def get_dict(self) -> dict:
        return {
            "table": self.table_name,
            "dataSource": self.data_source,
            "schema": [result_column.get_dict() for result_column in self.result_columns],
        }
