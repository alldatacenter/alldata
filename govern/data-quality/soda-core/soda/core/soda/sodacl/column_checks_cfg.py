from typing import List

from soda.sodacl.check_cfg import CheckCfg


class ColumnChecksCfg:
    def __init__(self, column_name: str):
        self.column_name = column_name
        self.check_cfgs: List[CheckCfg] = []

    def add_check_cfg(self, check_cfg: CheckCfg):
        self.check_cfgs.append(check_cfg)
