from typing import List, Optional

from soda.sodacl.location import Location


class DataSourceCheckCfg:
    def __init__(self, data_source_name: str, location: Location):
        self.data_source_name: str = data_source_name
        self.location: Location = location
        self.include_tables: Optional[List[str]] = []
        self.exclude_tables: Optional[List[str]] = []


class AutomatedMonitoringCfg(DataSourceCheckCfg):
    def __init__(self, data_source_name: str, location: Location):
        super().__init__(data_source_name, location)


class ProfileColumnsCfg(DataSourceCheckCfg):
    def __init__(self, data_source_name: str, location: Location):
        super().__init__(data_source_name, location)
        self.include_columns: List[str] = []
        self.exclude_columns: List[str] = []
        # TODO add parsing for this configuration
        self.limit_mins_maxs: int = 5
        # TODO add parsing for this configuration
        self.limit_frequent_values: int = 10


class DiscoverTablesCfg(DataSourceCheckCfg):
    def __init__(self, data_source_name: str, location: Location):
        super().__init__(data_source_name, location)


class SampleTablesCfg(DataSourceCheckCfg):
    def __init__(self, data_source_name: str, location: Location):
        super().__init__(data_source_name, location)
