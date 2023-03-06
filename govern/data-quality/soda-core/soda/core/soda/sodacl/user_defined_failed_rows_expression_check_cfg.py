from __future__ import annotations

from soda.sodacl.check_cfg import CheckCfg
from soda.sodacl.location import Location


class UserDefinedFailedRowsExpressionCheckCfg(CheckCfg):
    def __init__(
        self,
        source_header: str,
        source_line: str,
        source_configurations: dict | None,
        location: Location,
        name: str,
        fail_condition_sql_expr: str | None,
        samples_limit: int | None = None,
    ):
        super().__init__(source_header, source_line, source_configurations, location, name)
        self.samples_limit = samples_limit
        self.fail_condition_sql_expr: str | None = fail_condition_sql_expr
