from __future__ import annotations

from soda.sodacl.column_configurations_cfg import ColumnConfigurationsCfg
from soda.sodacl.location import Location
from soda.sodacl.partition_cfg import PartitionCfg


class TableCfg:
    def __init__(self, table_name: str):
        self.table_name: str = table_name
        self.column_configurations_cfgs: dict[str, ColumnConfigurationsCfg] = {}
        self.column_configuration_locations: list[Location] = []
        self.partition_cfgs: list[PartitionCfg] = []

    def get_or_create_column_configurations(self, column_name: str) -> ColumnConfigurationsCfg:
        column_configurations_cfg = self.column_configurations_cfgs.get(column_name)
        if not column_configurations_cfg:
            column_configurations_cfg = ColumnConfigurationsCfg(column_name)
            self.column_configurations_cfgs[column_name] = column_configurations_cfg
        return column_configurations_cfg

    def create_partition(self, file_path: str, partition_name: str) -> PartitionCfg:
        # Because the default table level checks are modelled with filter_name = None
        if partition_name is None:
            # file paths should be ignored
            file_path = None
        partition_cfg = PartitionCfg(file_path=file_path, partition_name=partition_name)
        self.partition_cfgs.append(partition_cfg)
        return partition_cfg

    def find_partition(self, file_path: str, partition_name: str) -> PartitionCfg:
        # Because the default table level checks are modelled with filter_name = None
        if partition_name is None:
            # file paths should be ignored
            file_path = None
        partition_cfg = next(
            (
                filter
                for filter in self.partition_cfgs
                if filter.file_path == file_path and filter.partition_name == partition_name
            ),
            None,
        )
        # Because the default table level checks are modelled with filter_name = None
        if partition_cfg is None and partition_name is None:
            return self.create_partition(None, None)
        return partition_cfg
