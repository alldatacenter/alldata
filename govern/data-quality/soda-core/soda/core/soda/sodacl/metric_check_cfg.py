from __future__ import annotations

from soda.sodacl.change_over_time_cfg import ChangeOverTimeCfg
from soda.sodacl.check_cfg import CheckCfg
from soda.sodacl.location import Location
from soda.sodacl.missing_and_valid_cfg import MissingAndValidCfg
from soda.sodacl.threshold_cfg import ThresholdCfg


class MetricCheckCfg(CheckCfg):
    MULTI_METRIC_CHECK_TYPES = [
        "duplicate_count",
        "duplicate_percent",
        "percentile",
    ]

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
        samples_limit: int | None = None,
    ):
        super().__init__(source_header, source_line, source_configurations, location, name, samples_limit)
        self.metric_name: str = metric_name
        self.metric_args: list[object] | None = metric_args
        self.missing_and_valid_cfg: MissingAndValidCfg = missing_and_valid_cfg
        self.filter: str | None = filter
        self.condition: str | None = condition
        self.metric_expression: str | None = metric_expression
        self.metric_query: str | None = metric_query
        self.change_over_time_cfg: ChangeOverTimeCfg | None = change_over_time_cfg
        self.fail_threshold_cfg: ThresholdCfg = fail_threshold_cfg
        self.warn_threshold_cfg: ThresholdCfg = warn_threshold_cfg

    def get_column_name(self) -> str | None:
        if self.metric_name == "duplicate_count":
            return self.metric_args[0] if self.metric_args and len(self.metric_args) == 1 else None
        else:
            return self.metric_args[0] if self.metric_args else None

    def has_threshold(self):
        return self.fail_threshold_cfg or self.warn_threshold_cfg

    def resolve_thresholds(self, f):
        if self.fail_threshold_cfg:
            self.fail_threshold_cfg = self.fail_threshold_cfg.resolve(f)
        if self.warn_threshold_cfg:
            self.warn_threshold_cfg = self.warn_threshold_cfg.resolve(f)
