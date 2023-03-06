from typing import TYPE_CHECKING, Dict, List

from soda.execution.check.discover_tables_run import DiscoverTablesRun
from soda.execution.check.profile_columns_run import ProfileColumnsRun
from soda.execution.check.sample_tables_run import SampleTablesRun
from soda.execution.data_source import DataSource
from soda.execution.metric.metric import Metric
from soda.execution.query.query import Query
from soda.execution.table import Table
from soda.sodacl.data_source_check_cfg import (
    AutomatedMonitoringCfg,
    DiscoverTablesCfg,
    ProfileColumnsCfg,
    SampleTablesCfg,
)
from soda.sodacl.data_source_scan_cfg import DataSourceScanCfg

if TYPE_CHECKING:
    from soda.scan import Scan


class DataSourceScan:
    def __init__(
        self,
        scan: "Scan",
        data_source_scan_cfg: DataSourceScanCfg,
        data_source: DataSource,
    ):
        from soda.execution.metric.metric import Metric
        from soda.execution.table import Table

        self.scan: Scan = scan
        self.data_source_scan_cfg: DataSourceScanCfg = data_source_scan_cfg
        self.metrics: List[Metric] = []
        self.data_source: DataSource = data_source
        self.tables: Dict[str, Table] = {}
        self.queries: List[Query] = []

    def get_or_create_table(self, table_name: str) -> Table:
        table = self.tables.get(table_name)
        if table is None:
            table = Table(self, table_name)
            self.tables[table_name] = table
        return table

    def resolve_metric(self, metric: Metric) -> Metric:
        """
        If the metric is not added before, this method will:
         - Add the metric to scan.metrics
         - Ensure the metric is added to the appropriate query (if applicable)
        """
        existing_metric = self.scan._find_existing_metric(metric)
        if existing_metric:
            existing_metric.merge_checks(metric)
            return existing_metric
        self.scan._add_metric(metric)
        metric.ensure_query()
        return metric

    def get_queries(self):
        return

    def execute_queries(self):
        all_data_source_queries: List[Query] = []
        for table in self.tables.values():
            for partition in table.partitions.values():
                partition_queries = partition.collect_queries()
                all_data_source_queries.extend(partition_queries)
        all_data_source_queries.extend(self.queries)

        for query in all_data_source_queries:
            query.execute()

    def run(self, data_source_check_cfg: DataSourceScanCfg, scan: "Scan"):
        if isinstance(data_source_check_cfg, AutomatedMonitoringCfg):
            from soda.execution.check.automated_monitoring_run import (
                AutomatedMonitoringRun,
            )

            automated_monitoring_run = AutomatedMonitoringRun(self, data_source_check_cfg).run()
            scan._checks.extend(automated_monitoring_run)

        if isinstance(data_source_check_cfg, ProfileColumnsCfg):
            profile_columns_run = ProfileColumnsRun(self, data_source_check_cfg).run()
            scan._profile_columns_result_tables.extend(profile_columns_run.tables)

        if isinstance(data_source_check_cfg, DiscoverTablesCfg):
            discover_tables_run = DiscoverTablesRun(self, data_source_check_cfg).run()
            scan._discover_tables_result_tables.extend(discover_tables_run.tables)

        if isinstance(data_source_check_cfg, SampleTablesCfg):
            sample_tables_run = SampleTablesRun(self, data_source_check_cfg).run()
            scan._sample_tables_result_tables.extend(sample_tables_run.tables)
