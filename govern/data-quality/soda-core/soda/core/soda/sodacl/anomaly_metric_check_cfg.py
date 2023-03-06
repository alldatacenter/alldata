from __future__ import annotations

from soda.sodacl.location import Location
from soda.sodacl.metric_check_cfg import MetricCheckCfg
from soda.sodacl.missing_and_valid_cfg import MissingAndValidCfg
from soda.sodacl.threshold_cfg import ThresholdCfg


class AnomalyMetricCheckCfg(MetricCheckCfg):
    def __init__(
        self,
        source_header: str,
        source_line: str,
        source_configurations: str | None,
        location: Location,
        name: str | None,
        metric_name: str,
        metric_args: list[object] | None,
        missing_and_valid_cfg: MissingAndValidCfg | None,
        filter: str | None,
        condition: str | None,
        metric_expression: str | None,
        metric_query: str | None,
        change_over_time_cfg: ChangeOverTimeCfg | None,
        fail_threshold_cfg: ThresholdCfg | None,
        warn_threshold_cfg: ThresholdCfg | None,
        is_automated_monitoring: bool = False,
        samples_limit: int | None = None,
    ):
        super().__init__(
            source_header,
            source_line,
            source_configurations,
            location,
            name,
            metric_name,
            metric_args,
            missing_and_valid_cfg,
            filter,
            condition,
            metric_expression,
            metric_query,
            change_over_time_cfg,
            fail_threshold_cfg,
            warn_threshold_cfg,
            samples_limit=samples_limit,
        )
        self.is_automated_monitoring = is_automated_monitoring
