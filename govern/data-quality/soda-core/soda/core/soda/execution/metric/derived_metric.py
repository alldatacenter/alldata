from typing import Dict, List, Optional

from soda.common.undefined_instance import undefined
from soda.execution.metric.metric import Metric

METRIC_NAME_MISSING_PERCENT = "missing_percent"
METRIC_NAME_INVALID_PERCENT = "invalid_percent"
METRIC_NAME_DUPLICATE_PERCENT = "duplicate_percent"
DERIVED_METRIC_NAMES = [
    METRIC_NAME_MISSING_PERCENT,
    METRIC_NAME_INVALID_PERCENT,
    METRIC_NAME_DUPLICATE_PERCENT,
]


def derive_missing_percentage(values: Dict[str, int]):
    missing_count = values["missing_count"]
    row_count = values["row_count"]

    if _is_not_undefined_and_is(missing_count) and _is_not_undefined_and_is(row_count):
        missing_percentage = missing_count * 100 / row_count
    else:
        missing_percentage = 0

    return float(round(missing_percentage, 2))


def derive_invalid_percentage(values: Dict[str, int]):
    invalid_count = values["invalid_count"]
    row_count = values["row_count"]

    if _is_not_undefined_and_is(invalid_count) and _is_not_undefined_and_is(row_count):
        invalid_percentage = invalid_count * 100 / row_count
    else:
        invalid_percentage = 0

    return float(round(invalid_percentage, 2))


def derive_duplicate_percentage(values: Dict[str, int]):
    duplicate_count = values["duplicate_count"]
    row_count = values["row_count"]

    if _is_not_undefined_and_is(duplicate_count) and _is_not_undefined_and_is(row_count):
        duplicate_percentage = duplicate_count * 100 / row_count
    else:
        duplicate_percentage = 0

    return float(round(duplicate_percentage, 2))


def _is_not_undefined_and_is(variable) -> bool:
    return variable and variable != undefined


class DerivedMetric(Metric):
    def __init__(
        self,
        data_source_scan: "DataSourceScan",
        partition: Optional["Partition"],
        column: Optional["Column"],
        metric_name: str,
        metric_args: Optional[List[object]],
        filter: str,
        check_missing_and_valid_cfg: "MissingAndValidCfg",
        column_configurations_cfg: "ColumnConfigurationsCfg",
        check: "Check",
    ):
        from soda.sodacl.missing_and_valid_cfg import MissingAndValidCfg

        merged_missing_and_valid_cfg = MissingAndValidCfg.merge(check_missing_and_valid_cfg, column_configurations_cfg)
        other_metric_args = metric_args[1:] if isinstance(metric_args, list) and len(metric_args) > 1 else None
        super().__init__(
            data_source_scan=data_source_scan,
            partition=partition,
            column=column,
            name=metric_name,
            check=check,
            identity_parts=[
                other_metric_args,
                filter,
                merged_missing_and_valid_cfg.get_identity_parts() if merged_missing_and_valid_cfg else None,
            ],
        )

        from soda.execution.derived_formula import DerivedFormula

        # Metric identity fields
        # The properties that logically define the metric
        # (this logical identity is actually implemented with Python equality see __eq__,
        # and hence different from Python identity)

        self.metric_name: Optional[str] = metric_name
        self.metric_args: Optional[List[object]] = metric_args
        self.filter: str = filter
        self.missing_and_valid_cfg: MissingAndValidCfg = merged_missing_and_valid_cfg
        self.derived_formula: DerivedFormula = self.create_percentage_formula()

    def create_percentage_formula(self):
        from soda.execution.derived_formula import DerivedFormula

        if METRIC_NAME_MISSING_PERCENT == self.name:
            return DerivedFormula(
                function=derive_missing_percentage,
                metric_dependencies={
                    "row_count": self.build_dependency_metric("row_count"),
                    "missing_count": self.build_dependency_metric("missing_count"),
                },
            )

        if METRIC_NAME_INVALID_PERCENT == self.name:
            return DerivedFormula(
                function=derive_invalid_percentage,
                metric_dependencies={
                    "row_count": self.build_dependency_metric("row_count"),
                    "invalid_count": self.build_dependency_metric("invalid_count"),
                },
            )

        if METRIC_NAME_DUPLICATE_PERCENT == self.name:
            return DerivedFormula(
                function=derive_duplicate_percentage,
                metric_dependencies={
                    "row_count": self.build_dependency_metric("row_count"),
                    "duplicate_count": self.build_dependency_metric("duplicate_count"),
                },
            )

    def build_dependency_metric(self, metric_name: str) -> "Metric":
        from soda.execution.metric.numeric_query_metric import NumericQueryMetric

        return self.data_source_scan.resolve_metric(
            NumericQueryMetric(
                data_source_scan=self.data_source_scan,
                partition=self.partition,
                column=self.column if metric_name != "row_count" else None,
                metric_name=metric_name,
                metric_args=self.metric_args,
                filter=self.filter,
                aggregation=None,
                check_missing_and_valid_cfg=self.missing_and_valid_cfg if metric_name != "row_count" else None,
                column_configurations_cfg=None,
                check=next(iter(self.checks)),
            )
        )

    def compute_derived_metric_values(self):
        """
        Resolves derived metrics recursively.
        Query metric values are all found in parameter metric_values.
        As derived metrics are resolved, the new derived metric values are added to metric_values.
        """

        for dependency_metric in self.derived_formula.metric_dependencies.values():
            if isinstance(dependency_metric, DerivedMetric):
                dependency_metric.compute_derived_metric_values()

        formula_values: Dict[str, object] = {}
        for (
            metric_dependency_name,
            dependency_metric,
        ) in self.derived_formula.metric_dependencies.items():
            formula_values[metric_dependency_name] = dependency_metric.value

        self.value = self.derived_formula.function(formula_values)
        self.formula_values = formula_values
        failed_rows_sample_refs = self.derived_formula.collect_failed_rows_sample_refs()
        if len(failed_rows_sample_refs) == 1:
            self.failed_rows_sample_ref = failed_rows_sample_refs[0]
