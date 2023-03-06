from __future__ import annotations

from soda.sodacl.column_checks_cfg import ColumnChecksCfg


class ColumnMetrics:
    def __init__(self, partition: Partition, column_name: str):
        from soda.execution.metric.metric import Metric
        from soda.execution.partition import Partition
        from soda.sodacl.column_checks_cfg import ColumnChecksCfg

        self.data_source_scan = partition.data_source_scan
        self.partition: Partition = partition
        self.column = partition.table.get_or_create_column(column_name)
        self.column_checks_cfg: ColumnChecksCfg = None
        self.metrics: list[Metric] = []

    def set_column_check_cfg(self, column_checks_cfg: ColumnChecksCfg):
        self.column_checks_cfg = column_checks_cfg

    def add_check_cfg(self, check_cfg):
        if self.column_checks_cfg is None:
            self.column_checks_cfg = ColumnChecksCfg(self.column.column_name)
        self.column_checks_cfg.add_check_cfg(check_cfg)
