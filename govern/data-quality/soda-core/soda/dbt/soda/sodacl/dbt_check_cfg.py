from __future__ import annotations

from soda.sodacl.check_cfg import CheckCfg


class DbtCheckCfg(CheckCfg):
    def __init__(self, name: str, file_path: str, column_name: str, table_name: str | None = None):
        self.name = name
        self.column_name = column_name
        self.file_path = file_path
        self.table_name = table_name

    def get_column_name(self) -> str | None:
        return self.column_name
