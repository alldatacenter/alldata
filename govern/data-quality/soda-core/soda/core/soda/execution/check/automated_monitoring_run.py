from typing import Dict, List

from soda.execution.check.anomaly_metric_check import AnomalyMetricCheck
from soda.execution.check.check import Check
from soda.execution.check.schema_check import SchemaCheck
from soda.execution.data_source_scan import DataSourceScan
from soda.execution.partition import Partition
from soda.sodacl.anomaly_metric_check_cfg import AnomalyMetricCheckCfg
from soda.sodacl.data_source_check_cfg import DataSourceCheckCfg
from soda.sodacl.schema_check_cfg import SchemaCheckCfg, SchemaValidations
from soda.sodacl.threshold_cfg import ThresholdCfg


class AutomatedMonitoringRun:
    def __init__(self, data_source_scan: DataSourceScan, data_source_check_cfg: DataSourceCheckCfg):
        self.data_source_scan: DataSourceScan = data_source_scan
        self.soda_cloud = data_source_scan.scan._configuration.soda_cloud
        self.data_source = data_source_scan.data_source
        self.data_source_check_cfg: DataSourceCheckCfg = data_source_check_cfg
        self.logs = self.data_source_scan.scan._logs
        self.table_names = self._get_table_names()

    def run(self) -> List[Check]:
        automated_checks: List[Check] = []
        annomaly_detection_checks: List[AnomalyMetricCheck] = self.create_anomaly_detection_checks()
        schema_checks: List[SchemaCheck] = self.create_schema_checks()

        automated_checks.extend(annomaly_detection_checks)
        automated_checks.extend(schema_checks)
        return automated_checks

    def create_anomaly_detection_checks(self) -> List[AnomalyMetricCheck]:
        annomaly_detection_checks = []

        for measured_table_name in self.table_names:
            anomaly_metric_check_cfg = AnomalyMetricCheckCfg(
                source_header=f"checks for {measured_table_name}",
                source_line="anomaly score for row_count < default",
                source_configurations=None,
                location=self.data_source_check_cfg.location,
                name=None,
                metric_name="row_count",
                metric_args=None,
                missing_and_valid_cfg=None,
                filter=None,
                condition=None,
                metric_expression=None,
                metric_query=None,
                change_over_time_cfg=None,
                fail_threshold_cfg=None,
                warn_threshold_cfg=ThresholdCfg(gt=0.9),
                is_automated_monitoring=True,
            )

            # Mock partition
            table = self.data_source_scan.get_or_create_table(measured_table_name)
            partition: Partition = table.get_or_create_partition(None)
            anomaly_metric_check = AnomalyMetricCheck(
                anomaly_metric_check_cfg, self.data_source_scan, partition=partition
            )
            anomaly_metric_check.archetype = "volumeConsistency"

            # Execute query to change the value of metric class to get the historical results
            self.data_source_scan.execute_queries()

            annomaly_detection_checks.append(anomaly_metric_check)

        return annomaly_detection_checks

    def create_schema_checks(self) -> List[SchemaCheck]:
        schema_checks = []

        fail_validations = SchemaValidations(
            required_column_names=None,
            required_column_types=None,
            required_column_indexes=None,
            forbidden_column_names=None,
            is_column_addition_forbidden=False,
            is_column_deletion_forbidden=True,
            is_column_type_change_forbidden=True,
            is_column_index_change_forbidden=True,
        )

        warn_validations = SchemaValidations(
            required_column_names=None,
            required_column_types=None,
            required_column_indexes=None,
            forbidden_column_names=None,
            is_column_addition_forbidden=True,
            is_column_deletion_forbidden=False,
            is_column_type_change_forbidden=False,
            is_column_index_change_forbidden=False,
        )

        for measured_table_name in self.table_names:
            schema_check_cfg = SchemaCheckCfg(
                source_header=f"checks for {measured_table_name}",
                source_line="schema",
                source_configurations=None,
                location=self.data_source_check_cfg.location,
                name=None,
                warn_validations=warn_validations,
                fail_validations=fail_validations,
                is_automated_monitoring=True,
            )

            # Mock partition
            table = self.data_source_scan.get_or_create_table(measured_table_name)
            partition: Partition = table.get_or_create_partition(None)
            schema_check = SchemaCheck(schema_check_cfg, self.data_source_scan, partition=partition)
            schema_check.archetype = "schemaConsistency"

            # Execute query to change the value of metric class to get the historical results
            self.data_source_scan.execute_queries()
            schema_checks.append(schema_check)
        return schema_checks

    def _get_table_names(self) -> Dict[str, Dict[str, str]]:
        """
        Returns a dict that maps table names to a dict that maps column names to column types.
        {table_name -> {column_name -> column_type}}
        """
        include_tables = self.data_source_check_cfg.include_tables
        exclude_tables = self.data_source_check_cfg.exclude_tables
        table_names = self.data_source.get_table_names(include_tables=include_tables, exclude_tables=exclude_tables)
        return table_names
