from __future__ import annotations

import re

from soda.cloud.historic_descriptor import HistoricChangeOverTimeDescriptor
from soda.execution.check.check import Check
from soda.execution.check_outcome import CheckOutcome
from soda.execution.metric.group_evolution_metric import GroupEvolutionMetric
from soda.execution.metric.metric import Metric
from soda.sodacl.change_over_time_cfg import ChangeOverTimeCfg
from soda.sodacl.group_evolution_check_cfg import (
    GroupEvolutionCheckCfg,
    GroupValidations,
)

KEY_GROUPS_MEASURED = "groups measured"
KEY_GROUPS_PREVIOUS = "groups previous"


# TODO - add support for cloud diagnostics and retrieving values from cloud
class GroupEvolutionCheck(Check):
    def __init__(
        self,
        check_cfg: CheckCfg,
        data_source_scan: DataSourceScan,
        partition: Partition,
    ):
        super().__init__(
            check_cfg=check_cfg,
            data_source_scan=data_source_scan,
            partition=partition,
            column=None,
        )
        self.cloud_check_type = "generic"
        from soda.sodacl.user_defined_failed_rows_check_cfg import (
            UserDefinedFailedRowsCheckCfg,
        )

        check_cfg: UserDefinedFailedRowsCheckCfg = self.check_cfg

        metric = GroupEvolutionMetric(
            data_source_scan=self.data_source_scan,
            query=check_cfg.query,
            check=self,
            partition=partition,
        )
        metric = self.data_source_scan.resolve_metric(metric)
        self.metrics[KEY_GROUPS_MEASURED] = metric

        self.failures = 0
        self.warnings = 0
        self.failure_results = {"missing": [], "forbidden": [], "additions": [], "deletions": []}
        self.warning_results = {"missing": [], "forbidden": [], "additions": [], "deletions": []}

        group_evolution_check_cfg: GroupEvolutionCheckCfg = self.check_cfg
        if group_evolution_check_cfg.has_change_validations():
            historic_descriptor = HistoricChangeOverTimeDescriptor(
                metric_identity=metric.identity, change_over_time_cfg=ChangeOverTimeCfg()
            )
            self.historic_descriptors[KEY_GROUPS_PREVIOUS] = historic_descriptor

    def evaluate(self, metrics: dict[str, Metric], historic_values: dict[str, object]):
        group_evolution_check_cfg: GroupEvolutionCheckCfg = self.check_cfg

        self.measured_groups = metrics.get(KEY_GROUPS_MEASURED).value

        previous_groups = (
            historic_values.get(KEY_GROUPS_PREVIOUS).get("measurements").get("results")[0].get("value")
            if historic_values
            and historic_values.get(KEY_GROUPS_PREVIOUS, {}).get("measurements", {}).get("results", {})
            else None
        )

        if previous_groups:
            self.group_comparator = GroupComparator(previous_groups, self.measured_groups)
        else:
            if group_evolution_check_cfg.has_change_validations():
                warning_message = "Skipping group checks since there are no historic groups available!"
                self.logs.warning(warning_message)
                self.add_outcome_reason(outcome_type="notEnoughHistory", message=warning_message, severity="warn")
                return

        if group_evolution_check_cfg.fail_validations:
            (self.failures, self.failure_results) = self.group_validations(group_evolution_check_cfg.fail_validations)
        if group_evolution_check_cfg.warn_validations:
            (self.warnings, self.warning_results) = self.group_validations(group_evolution_check_cfg.warn_validations)

        if self.failures > 0:
            self.outcome = CheckOutcome.FAIL
        elif self.warnings > 0:
            self.outcome = CheckOutcome.WARN
        else:
            self.outcome = CheckOutcome.PASS

    def group_validations(self, validations: GroupValidations) -> (int, dict):
        measured_groups = self.measured_groups
        required_groups = set()
        missing_groups = []
        present_groups = []

        if validations.required_group_names:
            required_groups.update(validations.required_group_names)

        if validations:
            for required_group_name in required_groups:
                if required_group_name not in measured_groups:
                    missing_groups.append(required_group_name)

        if validations.forbidden_group_names:
            for forbidden_group_name in validations.forbidden_group_names:
                regex = forbidden_group_name.replace("%", ".*").replace("*", ".*")
                forbidden_pattern = re.compile(regex)
                for group_name in measured_groups:
                    if forbidden_pattern.match(group_name):
                        present_groups.append(group_name)

        result = {"missing": missing_groups, "forbidden": present_groups, "additions": [], "deletions": set()}

        total_count = sum(len(result[key]) for key in result.keys())

        if validations.is_group_addition_forbidden and self.group_comparator:
            result["additions"] = self.group_comparator.group_additions
        if validations.is_group_deletion_forbidden and self.group_comparator:
            result["deletions"] = self.group_comparator.group_deletions

        return total_count, result

    def get_cloud_diagnostics_dict(self) -> dict:
        import inflect

        p = inflect.engine()
        failures = f"{self.failures} {p.plural('Failure', self.failures)}"
        warnings = f"{self.warnings} {p.plural('Warning', self.warnings)}"

        cloud_diagnostics = {
            "blocks": [],
            "preferredChart": "bars",
            "valueLabel": f"{failures}, {warnings}",
            "valueSeries": {
                "values": [
                    {"label": "fail", "value": self.failures, "outcome": "fail"},
                    {"label": "warn", "value": self.warnings, "outcome": "warn"},
                ]
            },
        }

        diagnostic_text = self.diagnoistics_text(self.warning_results, "warn") + self.diagnoistics_text(
            self.failure_results, "fail"
        )

        cloud_diagnostics["blocks"].append(
            {"type": "csv", "title": "Diagnostics", "text": f"Group, Reason\n{diagnostic_text}"}
        )

        return cloud_diagnostics

    def diagnoistics_text(self, results, result_type):
        return "\n".join(
            ([f":icon-{result_type}: {m}, Missing\n" for m in results["missing"]])
            + ([f":icon-{result_type}: {f}, Forbidden\n" for f in results["forbidden"]])
            + ([f":icon-{result_type}: {a}, Added\n" for a in results["additions"]])
            + ([f":icon-{result_type}: {d}, Deleted\n" for d in results["deletions"]])
        )


class GroupComparator:
    def __init__(self, previous_groups, measured_groups):
        self.group_additions = []
        self.group_deletions = []
        self.__compute_group_changes(previous_groups, measured_groups)

    def __compute_group_changes(self, previous_groups, measured_groups):
        for previous_group in previous_groups:
            if previous_group not in measured_groups:
                self.group_additions.append(previous_group)
        for group in measured_groups:
            if group not in previous_groups:
                self.group_additions.append(group)
