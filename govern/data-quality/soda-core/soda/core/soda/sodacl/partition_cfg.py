from typing import Dict, List, Optional

from soda.sodacl.check_cfg import CheckCfg
from soda.sodacl.column_checks_cfg import ColumnChecksCfg
from soda.sodacl.location import Location


class PartitionCfg:
    def __init__(self, file_path: str, partition_name: Optional[str] = None):
        self.file_path: str = file_path
        # partition_name None means the default partition (without filter)
        self.partition_name: Optional[str] = partition_name
        self.sql_partition_filter: Optional[str] = None
        self.check_cfgs: List[CheckCfg] = []
        self.column_checks_cfgs: Dict[str, ColumnChecksCfg] = {}
        self.locations: List[Location] = []

    def add_check_cfg(self, check_cfg: CheckCfg):
        self.check_cfgs.append(check_cfg)

    def get_or_create_column_checks(self, column_name: str) -> ColumnChecksCfg:
        column_checks_cfg = self.column_checks_cfgs.get(column_name)
        if not column_checks_cfg:
            column_checks_cfg = ColumnChecksCfg(column_name)
            self.column_checks_cfgs[column_name] = column_checks_cfg
        return column_checks_cfg
