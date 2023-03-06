from __future__ import annotations

from typing import TYPE_CHECKING

from soda.profiling.discover_tables_result import DiscoverTablesResult
from soda.sodacl.data_source_check_cfg import DataSourceCheckCfg

if TYPE_CHECKING:
    from soda.execution.data_source_scan import DataSourceScan


class DiscoverTablesRun:
    def __init__(self, data_source_scan: DataSourceScan, data_source_check_cfg: DataSourceCheckCfg):
        self.data_source_scan = data_source_scan
        self.soda_cloud = data_source_scan.scan._configuration.soda_cloud
        self.data_source = data_source_scan.data_source
        self.data_source_check_cfg: DataSourceCheckCfg = data_source_check_cfg
        self.logs = self.data_source_scan.scan._logs

    def run(self) -> DiscoverTablesResult:
        discover_tables_result: DiscoverTablesResult = DiscoverTablesResult(self.data_source_check_cfg)
        self.logs.info(f"Running discover datasets for data source: {self.data_source.data_source_name}")

        # row_counts is a dict that maps table names to row counts.
        table_names: dict[str, int] = self.data_source.get_table_names(
            include_tables=self.data_source_check_cfg.include_tables,
            exclude_tables=self.data_source_check_cfg.exclude_tables,
            query_name="discover-tables-find-tables-and-row-counts",
        )

        if len(table_names) < 1:
            self.logs.warning(
                f"No table matching your SodaCL inclusion list found on your {self.data_source.data_source_name} "
                "data source. Table discovery results may be incomplete or entirely skipped",
                location=self.data_source_check_cfg.location,
            )
            return discover_tables_result

        self.logs.info(f"Discovering the following tables:")
        for table_name in table_names:
            self.logs.info(f"  - {table_name}")
            discover_tables_result_table = discover_tables_result.create_table(
                table_name, self.data_source.data_source_name
            )
            # get columns & metadata for current table
            columns_metadata_result = self.data_source.get_table_columns(
                table_name=table_name,
                query_name=f"discover-tables-column-metadata-for-{table_name}",
            )

            for column_name, column_type in columns_metadata_result.items():
                _ = discover_tables_result_table.create_column(column_name, column_type)

        return discover_tables_result
