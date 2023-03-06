from typing import Optional

from soda.sodacl.check_cfg import CheckCfg
from soda.sodacl.location import Location


class RowCountComparisonCheckCfg(CheckCfg):
    def __init__(
        self,
        source_header: str,
        source_line: str,
        source_configurations: Optional[str],
        location: Location,
        name: Optional[str],
        other_table_name: str,
        other_partition_name: Optional[str],
        other_data_source_name: Optional[str],
    ):
        super().__init__(source_header, source_line, source_configurations, location, name)
        self.other_table_name: str = other_table_name
        self.other_partition_name: Optional[str] = other_partition_name
        self.other_data_source_name: Optional[str] = other_data_source_name
