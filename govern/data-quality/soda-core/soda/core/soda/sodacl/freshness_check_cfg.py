from __future__ import annotations

from datetime import timedelta

from soda.sodacl.check_cfg import CheckCfg
from soda.sodacl.location import Location


class FreshnessCheckCfg(CheckCfg):
    def __init__(
        self,
        source_header: str,
        source_line: str,
        source_configurations: str | None,
        location: Location,
        name: str | None,
        column_name: str,
        variable_name: str | None,
        fail_freshness_threshold: timedelta,
        warn_freshness_threshold: timedelta,
    ):
        super().__init__(source_header, source_line, source_configurations, location, name)
        self.column_name: str = column_name
        self.variable_name: str = "NOW" if variable_name is None else variable_name
        self.fail_freshness_threshold: timedelta = fail_freshness_threshold
        self.warn_freshness_threshold: timedelta = warn_freshness_threshold

    def get_column_name(self) -> str | None:
        return self.column_name
