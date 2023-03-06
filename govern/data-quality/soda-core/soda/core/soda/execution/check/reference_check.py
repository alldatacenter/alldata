from typing import Dict

from soda.execution.check.check import Check
from soda.execution.check_outcome import CheckOutcome
from soda.execution.metric.metric import Metric
from soda.execution.metric.reference_metric import ReferenceMetric

KEY_INVALID_REFERENCE_COUNT = "invalid_reference_count"


class ReferenceCheck(Check):
    def __init__(self, check_cfg: "ReferenceCheckCfg", data_source_scan: "DataSourceScan", partition: "Partition"):
        single_source_column_name = (
            check_cfg.source_column_names[0] if len(check_cfg.source_column_names) == 1 else None
        )
        single_source_column = (
            partition.table.get_or_create_column(single_source_column_name) if single_source_column_name else None
        )
        super().__init__(
            check_cfg=check_cfg,
            data_source_scan=data_source_scan,
            partition=partition,
            column=single_source_column,
        )
        metric = ReferenceMetric(
            data_source_scan=self.data_source_scan,
            check=self,
            partition=partition,
            single_source_column=single_source_column,
        )
        metric = self.data_source_scan.resolve_metric(metric)
        self.metrics[KEY_INVALID_REFERENCE_COUNT] = metric
        self.failed_rows_sample_ref = None

    def get_cloud_diagnostics_dict(self) -> dict:
        cloud_diagnostics = super().get_cloud_diagnostics_dict()

        return cloud_diagnostics

    def evaluate(self, metrics: Dict[str, Metric], historic_values: Dict[str, object]):
        metric = metrics.get(KEY_INVALID_REFERENCE_COUNT)
        invalid_reference_count: int = metric.value

        self.outcome = CheckOutcome.PASS
        if invalid_reference_count > 0:
            self.outcome = CheckOutcome.FAIL

        self.check_value = invalid_reference_count
        self.failed_rows_sample_ref = metric.failed_rows_sample_ref
