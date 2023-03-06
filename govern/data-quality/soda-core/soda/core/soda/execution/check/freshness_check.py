from __future__ import annotations

from datetime import date, datetime, timedelta, timezone

from soda.execution.check.check import Check
from soda.execution.check_outcome import CheckOutcome
from soda.execution.metric.metric import Metric
from soda.execution.metric.numeric_query_metric import NumericQueryMetric

MAX_COLUMN_TIMESTAMP = "max_column_timestamp"


class FreshnessCheck(Check):
    def __init__(
        self,
        check_cfg: FreshnessCheckCfg,
        data_source_scan: DataSourceScan,
        partition: Partition,
        column: Column,
    ):
        super().__init__(
            check_cfg=check_cfg,
            data_source_scan=data_source_scan,
            partition=partition,
            column=column,
        )
        self.freshness_values: dict | None = None
        self.metrics[MAX_COLUMN_TIMESTAMP] = data_source_scan.resolve_metric(
            NumericQueryMetric(
                data_source_scan=self.data_source_scan,
                partition=self.partition,
                column=self.column,
                metric_name="max",
                metric_args=None,
                filter=None,
                aggregation=None,
                check_missing_and_valid_cfg=None,
                column_configurations_cfg=None,
                check=self,
            )
        )

    def evaluate(self, metrics: dict[str, Metric], historic_values: dict[str, object]):
        from soda.sodacl.freshness_check_cfg import FreshnessCheckCfg

        check_cfg: FreshnessCheckCfg = self.check_cfg

        now_variable_name = check_cfg.variable_name
        now_variable_timestamp_text = self.data_source_scan.scan.get_variable(now_variable_name)
        if not now_variable_timestamp_text:
            self.outcome = CheckOutcome.FAIL
            self.logs.error(
                f"Could not parse variable {now_variable_name} as a timestamp: variable not found",
            )
            return

        max_column_timestamp: datetime | None = metrics.get(MAX_COLUMN_TIMESTAMP).value
        now_variable_timestamp: datetime | None = None

        if type(max_column_timestamp) == date:  # using type instead of isinstance because datetime is subclass of date.
            # Convert data to datetime if its date, use max time (1ms before midnight)
            min_time = datetime.max.time()
            max_column_timestamp = datetime.combine(max_column_timestamp, min_time)

        is_max_column_timestamp_valid = isinstance(max_column_timestamp, datetime)
        if not is_max_column_timestamp_valid and max_column_timestamp is not None:
            self.logs.error(
                f"Could not evaluate freshness: max({self.check_cfg.column_name}) "
                f"is not a datetime: {type(max_column_timestamp).__name__}",
                location=self.check_cfg.location,
            )

        try:
            now_variable_timestamp = datetime.fromisoformat(now_variable_timestamp_text)
        except:
            self.logs.error(
                f"Could not parse variable {now_variable_name} as a timestamp: {now_variable_timestamp_text}",
                location=check_cfg.location,
            )

        is_now_variable_timestamp_valid = isinstance(now_variable_timestamp, datetime)

        freshness = None
        if not is_now_variable_timestamp_valid:
            self.logs.error(
                f"Could not evaluate freshness: variable {check_cfg.variable_name} is not a datetime: {now_variable_timestamp_text}",
                location=self.check_cfg.location,
            )

        max_column_timestamp_utc = (
            self._datetime_to_utc(max_column_timestamp) if isinstance(max_column_timestamp, datetime) else None
        )
        now_timestamp_utc = (
            self._datetime_to_utc(now_variable_timestamp) if isinstance(now_variable_timestamp, datetime) else None
        )

        if is_max_column_timestamp_valid and is_now_variable_timestamp_valid:
            freshness = now_timestamp_utc - max_column_timestamp_utc
            if check_cfg.fail_freshness_threshold is not None and freshness > check_cfg.fail_freshness_threshold:
                self.outcome = CheckOutcome.FAIL
            elif check_cfg.warn_freshness_threshold is not None and freshness > check_cfg.warn_freshness_threshold:
                self.outcome = CheckOutcome.WARN
            else:
                self.outcome = CheckOutcome.PASS
        elif max_column_timestamp is None:
            self.outcome = CheckOutcome.FAIL

        self.freshness_values = {
            "max_column_timestamp": str(max_column_timestamp) if max_column_timestamp else None,
            "max_column_timestamp_utc": str(max_column_timestamp_utc) if max_column_timestamp_utc else None,
            "now_variable_name": now_variable_name,
            "now_timestamp": now_variable_timestamp_text,
            "now_timestamp_utc": str(now_timestamp_utc) if now_timestamp_utc else None,
            "freshness": freshness,
        }

    def get_cloud_diagnostics_dict(self):
        cloud_diagnostics = super().get_cloud_diagnostics_dict()

        freshness = 0
        if self.freshness_values["freshness"] and isinstance(self.freshness_values["freshness"], timedelta):
            freshness = round(self.freshness_values["freshness"].total_seconds() * 1000)

        cloud_diagnostics["value"] = freshness  # milliseconds difference
        cloud_diagnostics["measure"] = "time"
        cloud_diagnostics["maxColumnTimestamp"] = self.freshness_values["max_column_timestamp"]
        cloud_diagnostics["maxColumnTimestampUtc"] = self.freshness_values["max_column_timestamp_utc"]
        cloud_diagnostics["nowVariableName"] = self.freshness_values["now_variable_name"]
        cloud_diagnostics["nowTimestamp"] = self.freshness_values["now_timestamp"]
        cloud_diagnostics["nowTimestampUtc"] = self.freshness_values["now_timestamp_utc"]
        cloud_diagnostics["freshness"] = self.freshness_values["freshness"]

        return cloud_diagnostics

    def get_log_diagnostic_dict(self) -> dict:
        return self.freshness_values

    @staticmethod
    def _datetime_to_utc(input: datetime) -> datetime:
        if input.tzinfo is None:
            return input.replace(tzinfo=timezone.utc)
        return input.astimezone(timezone.utc)
