from typing import List

from soda.sodacl.name_filter import NameFilter


class TablesetCfg:
    def __init__(self, tableset_name: str):
        self.tableset_name: str = tableset_name
        self.includes: List[NameFilter] = []
        self.excludes: List[NameFilter] = []
