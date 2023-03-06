from typing import List

from soda.sodacl.check_cfg import CheckCfg
from soda.sodacl.location import Location
from soda.sodacl.name_filter import NameFilter


class ForEachColumnCfg:
    def __init__(self):
        self.column_alias_name: str = None
        self.includes: List[NameFilter] = []
        self.excludes: List[NameFilter] = []
        self.check_cfgs: List[CheckCfg] = []
        self.locations: List[Location] = []
