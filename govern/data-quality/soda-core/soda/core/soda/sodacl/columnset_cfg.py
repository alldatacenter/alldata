from typing import List

from soda.sodacl.name_filter import NameFilter


class ColumnsetCfg:
    def __init__(self, columnset_name: str):
        self.columnset_name: str = columnset_name
        self.includes: List[NameFilter] = []
        self.excludes: List[NameFilter] = []
