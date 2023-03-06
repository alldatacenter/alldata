from __future__ import annotations

from soda.sodacl.check_cfg import CheckCfg
from soda.sodacl.location import Location


class ReferenceCheckCfg(CheckCfg):
    def __init__(
        self,
        source_header: str,
        source_line: str,
        source_configurations: str | None,
        location: Location,
        name: str | None,
        source_column_names: list[str],
        target_table_name: str,
        target_column_names: list[str],
        samples_limit: int | None,
    ):
        super().__init__(source_header, source_line, source_configurations, location, name, samples_limit)
        self.source_column_names: list[str] = source_column_names
        self.target_table_name: str = target_table_name
        self.target_column_names: list[str] = target_column_names
