from typing import List

from soda.profiling.discover_table_result_table import DiscoverTablesResultTable
from soda.sodacl.data_source_check_cfg import DataSourceCheckCfg


class DiscoverTablesResult:
    def __init__(self, data_source_check_cfg: DataSourceCheckCfg):
        self.data_source_check_cfg: DataSourceCheckCfg = data_source_check_cfg
        self.tables: List[DiscoverTablesResultTable] = []

    def create_table(self, table_name: str, data_source_name: str) -> DiscoverTablesResultTable:
        table = DiscoverTablesResultTable(table_name, data_source_name)
        self.tables.append(table)
        return table
