from typing import Dict

from soda.execution.check.check import Check
from soda.execution.check_outcome import CheckOutcome
from soda.execution.metric.metric import Metric
from soda.execution.metric.numeric_query_metric import NumericQueryMetric
from soda.execution.partition import Partition


class RowCountComparisonCheck(Check):
    def __init__(
        self,
        check_cfg: "RowCountComparisonCheckCfg",
        data_source_scan: "DataSourceScan",
        partition: Partition,
    ):
        super().__init__(
            check_cfg=check_cfg,
            data_source_scan=data_source_scan,
            partition=partition,
            column=None,
        )
        from soda.execution.table import Table
        from soda.sodacl.row_count_comparison_check_cfg import (
            RowCountComparisonCheckCfg,
        )

        data_source_scan = self.data_source_scan
        scan = data_source_scan.scan

        row_count_comparison_check_cfg: RowCountComparisonCheckCfg = self.check_cfg

        if row_count_comparison_check_cfg.other_data_source_name:
            data_source_scan = scan._get_or_create_data_source_scan(
                row_count_comparison_check_cfg.other_data_source_name
            )

        other_table: Table = data_source_scan.get_or_create_table(row_count_comparison_check_cfg.other_table_name)
        self.other_partition = other_table.get_or_create_partition(row_count_comparison_check_cfg.other_partition_name)

        self.metrics["row_count"] = self.data_source_scan.resolve_metric(
            NumericQueryMetric(
                data_source_scan=self.data_source_scan,
                partition=partition,
                column=None,
                metric_name="row_count",
                metric_args=None,
                filter=None,
                aggregation=None,
                check_missing_and_valid_cfg=None,
                column_configurations_cfg=None,
                check=self,
            )
        )

        self.metrics["other_row_count"] = data_source_scan.resolve_metric(
            NumericQueryMetric(
                data_source_scan=data_source_scan,
                partition=self.other_partition,
                column=None,
                metric_name="row_count",
                metric_args=None,
                filter=None,
                aggregation=None,
                check_missing_and_valid_cfg=None,
                column_configurations_cfg=None,
                check=self,
            )
        )

    def get_cloud_diagnostics_dict(self) -> dict:
        row_count: int = self.metrics["row_count"].value
        other_row_count: int = self.metrics["other_row_count"].value

        return {
            # TODO Check with Soda Cloud what should be the value here
            "value": row_count - other_row_count,
            "rowCount": row_count,
            "otherRowCount": other_row_count,
        }

    def evaluate(self, metrics: Dict[str, Metric], historic_values: Dict[str, object]):
        row_count: int = metrics["row_count"].value
        other_row_count: int = metrics["other_row_count"].value

        diagnostic_values = {
            "row_count": row_count,
            "other_row_count": other_row_count,
        }

        row_count_diff = row_count - other_row_count

        self.outcome = CheckOutcome.PASS if row_count_diff == 0 else CheckOutcome.FAIL
        self.check_value = row_count_diff
        self.diagnostic_values = diagnostic_values
