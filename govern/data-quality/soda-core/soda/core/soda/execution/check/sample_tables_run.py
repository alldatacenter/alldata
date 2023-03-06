from __future__ import annotations

from typing import TYPE_CHECKING

from soda.profiling.sample_tables_result import SampleTablesResult
from soda.sodacl.data_source_check_cfg import DataSourceCheckCfg

if TYPE_CHECKING:
    from soda.execution.data_source_scan import DataSourceScan


class SampleTablesRun:
    def __init__(self, data_source_scan: DataSourceScan, data_source_check_cfg: DataSourceCheckCfg):
        self.data_source_scan = data_source_scan
        self.soda_cloud = data_source_scan.scan._configuration.soda_cloud
        self.data_source = data_source_scan.data_source
        self.data_source_name = self.data_source.data_source_name
        self.data_source_check_cfg: DataSourceCheckCfg = data_source_check_cfg
        self.logs = self.data_source_scan.scan._logs

    def run(self) -> SampleTablesResult:
        sample_tables_result: SampleTablesResult = SampleTablesResult(self.data_source_check_cfg)
        self.logs.info(f"Running sample tables for data source: {self.data_source_name}")

        table_names: list[str] = self.data_source.get_table_names(
            include_tables=self.data_source_check_cfg.include_tables,
            exclude_tables=self.data_source_check_cfg.exclude_tables,
            query_name=f"sample-datasets-find-datasets",
        )

        self.logs.info(f"Sampling the following tables:")
        for table_name in table_names:
            self.logs.info(f"  - {table_name}")

            # get columns and first n rows
            sample_ref = self.data_source.store_table_sample(table_name, limit=100)
            sample_tables_result.append_table(table_name, self.data_source_name, sample_ref)
            self.logs.info(f"Successfully collected samples for dataset: {table_name}!")

        if not isinstance(sample_tables_result.tables, list):
            self.logs.error(f"Sample tables for data source: {self.data_source_name} failed")

        return sample_tables_result
