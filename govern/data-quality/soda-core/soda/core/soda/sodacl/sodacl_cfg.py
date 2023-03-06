from typing import Dict, List

from soda.sodacl.data_source_scan_cfg import DataSourceScanCfg
from soda.sodacl.for_each_column_cfg import ForEachColumnCfg
from soda.sodacl.for_each_dataset_cfg import ForEachDatasetCfg


class SodaCLCfg:
    def __init__(self, scan):
        self.scan = scan
        self.data_source_scan_cfgs: Dict[str, DataSourceScanCfg] = {}
        self.for_each_dataset_cfgs: List[ForEachDatasetCfg] = []
        self.for_each_column_cfgs: List[ForEachColumnCfg] = []

    def get_or_create_data_source_scan_cfgs(self, data_source_name):
        data_source_scan_cfgs = self.data_source_scan_cfgs.get(data_source_name)
        if not data_source_scan_cfgs:
            data_source_scan_cfgs = DataSourceScanCfg(data_source_name)
            self.data_source_scan_cfgs[data_source_name] = data_source_scan_cfgs
        return data_source_scan_cfgs
