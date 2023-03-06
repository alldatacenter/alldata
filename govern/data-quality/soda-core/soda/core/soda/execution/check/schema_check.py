import re
from typing import Dict, List, Optional

from soda.execution.check.check import Check
from soda.execution.check_outcome import CheckOutcome
from soda.execution.metric.metric import Metric
from soda.execution.partition import Partition
from soda.execution.schema_comparator import SchemaComparator
from soda.soda_cloud.historic_descriptor import HistoricChangeOverTimeDescriptor
from soda.sodacl.change_over_time_cfg import ChangeOverTimeCfg
from soda.sodacl.schema_check_cfg import SchemaCheckCfg, SchemaValidations

KEY_SCHEMA_MEASURED = "schema_measured"
KEY_SCHEMA_PREVIOUS = "schema_previous"


class SchemaCheck(Check):
    def __init__(
        self,
        check_cfg: "CheckCfg",
        data_source_scan: "DataSourceScan",
        partition: "Partition",
    ):
        super().__init__(
            check_cfg=check_cfg,
            data_source_scan=data_source_scan,
            partition=partition,
            column=None,
        )

        self.cloud_check_type = "schema"
        self.measured_schema: Optional[List[Dict[str, str]]] = None
        self.schema_missing_column_names: Optional[List[str]] = None
        self.schema_present_column_names: Optional[List[str]] = None
        self.schema_column_type_mismatches: Optional[Dict[str, str]] = None
        self.schema_column_index_mismatches: Optional[Dict[str, str]] = None
        self.schema_comparator = None
        from soda.execution.metric.schema_metric import SchemaMetric

        schema_metric = data_source_scan.resolve_metric(
            SchemaMetric(
                data_source_scan=self.data_source_scan,
                partition=partition,
                check=self,
            )
        )
        self.metrics[KEY_SCHEMA_MEASURED] = schema_metric

        schema_check_cfg: SchemaCheckCfg = self.check_cfg
        if schema_check_cfg.has_change_validations():
            historic_descriptor = HistoricChangeOverTimeDescriptor(
                metric_identity=schema_metric.identity, change_over_time_cfg=ChangeOverTimeCfg()
            )
            self.historic_descriptors[KEY_SCHEMA_PREVIOUS] = historic_descriptor

    def evaluate(self, metrics: Dict[str, Metric], historic_values: Dict[str, object]):
        schema_check_cfg: SchemaCheckCfg = self.check_cfg

        self.measured_schema: List[Dict[str, str]] = metrics.get(KEY_SCHEMA_MEASURED).value

        schema_previous_measurement = (
            historic_values.get(KEY_SCHEMA_PREVIOUS).get("measurements").get("results")[0].get("value")
            if historic_values
            and historic_values.get(KEY_SCHEMA_PREVIOUS, {}).get("measurements", {}).get("results", {})
            else None
        )
        schema_previous = (
            [{"name": sp.get("columnName"), "type": sp.get("sourceDataType")} for sp in schema_previous_measurement]
            if schema_previous_measurement
            else None
        )

        self.schema_missing_column_names = []
        self.schema_present_column_names = []
        self.schema_column_type_mismatches = {}
        self.schema_column_index_mismatches = {}

        if schema_previous:
            self.schema_comparator = SchemaComparator(schema_previous, self.measured_schema)
        else:
            if schema_check_cfg.has_change_validations():
                warning_message = "Skipping schema checks since there is no historic schema metrics!"
                self.logs.warning(warning_message)
                self.add_outcome_reason(outcome_type="notEnoughHistory", message=warning_message, severity="warn")
                return

        if self.has_schema_violations(schema_check_cfg.fail_validations):
            self.outcome = CheckOutcome.FAIL
        elif self.has_schema_violations(schema_check_cfg.warn_validations):
            self.outcome = CheckOutcome.WARN
        else:
            self.outcome = CheckOutcome.PASS

    def has_schema_violations(
        self,
        schema_validations: SchemaValidations,
    ) -> bool:
        if schema_validations is None:
            return False

        measured_schema = self.measured_schema

        measured_column_names = [column["name"] for column in measured_schema]

        column_types = {column["name"]: column["type"] for column in measured_schema}

        required_column_names = set()
        if schema_validations.required_column_names:
            required_column_names.update(schema_validations.required_column_names)
        if schema_validations.required_column_types:
            required_column_names.update(schema_validations.required_column_types.keys())

        if required_column_names:
            for required_column_name in required_column_names:
                if required_column_name not in measured_column_names:
                    self.schema_missing_column_names.append(required_column_name)

        if schema_validations.forbidden_column_names:
            for forbidden_column_name in schema_validations.forbidden_column_names:
                regex = forbidden_column_name.replace("%", ".*").replace("*", ".*")
                forbidden_pattern = re.compile(regex)
                for column_name in measured_column_names:
                    if forbidden_pattern.match(column_name):
                        self.schema_present_column_names.append(column_name)

        if schema_validations.required_column_types:
            data_source = self.data_source_scan.data_source
            for (
                expected_column_name,
                expected_column_type,
            ) in schema_validations.required_column_types.items():
                if expected_column_name in column_types:
                    actual_type = column_types[expected_column_name]
                    is_same_type = data_source.is_same_type_in_schema_check(expected_column_type, actual_type)
                    if expected_column_name in column_types and not is_same_type:
                        self.schema_column_type_mismatches[expected_column_name] = {
                            "expected_type": expected_column_type,
                            "actual_type": column_types[expected_column_name],
                        }

        if schema_validations.required_column_indexes:
            for required_column_name in schema_validations.required_column_indexes:
                if required_column_name not in column_types:
                    self.schema_missing_column_names.append(required_column_name)

            measured_column_indexes = {
                measured_column_name: index for index, measured_column_name in enumerate(measured_column_names)
            }

            for (
                expected_column_name,
                expected_index,
            ) in schema_validations.required_column_indexes.items():
                if expected_column_name in measured_column_indexes:
                    actual_index = measured_column_indexes[expected_column_name]
                    if expected_index != actual_index:
                        column_on_index = (
                            measured_schema[expected_index]["name"] if expected_index < len(measured_schema) else None
                        )
                        self.schema_column_index_mismatches[expected_column_name] = {
                            "expected_index": expected_index,
                            "actual_index": actual_index,
                            "column_on_expected_index": column_on_index,
                        }

        return (
            len(self.schema_missing_column_names) > 0
            or len(self.schema_present_column_names) > 0
            or len(self.schema_column_type_mismatches) > 0
            or len(self.schema_column_index_mismatches) > 0
            or (
                schema_validations.is_column_addition_forbidden
                and self.schema_comparator
                and len(self.schema_comparator.schema_column_additions) > 0
            )
            or (
                schema_validations.is_column_deletion_forbidden
                and self.schema_comparator
                and len(self.schema_comparator.schema_column_deletions) > 0
            )
            or (
                schema_validations.is_column_type_change_forbidden
                and self.schema_comparator
                and len(self.schema_comparator.schema_column_type_changes) > 0
            )
            or (
                schema_validations.is_column_index_change_forbidden
                and self.schema_comparator
                and len(self.schema_comparator.schema_column_index_changes) > 0
            )
        )

    def get_cloud_diagnostics_dict(self) -> dict:
        schema_diagnostics = {}

        if self.measured_schema:
            schema_diagnostics["schema"] = [
                {"columnName": c["name"], "sourceDataType": c["type"]} for c in self.measured_schema
            ]
        # TODO Update these based on API contract
        # if self.schema_missing_column_names:
        #     schema_diagnostics["schema_missing_column_names"] = self.schema_missing_column_names
        # if self.schema_present_column_names:
        #     schema_diagnostics["schema_present_column_names"] = self.schema_present_column_names
        # if self.schema_column_type_mismatches:
        #     schema_diagnostics["schema_column_type_mismatches"] = self.schema_column_type_mismatches
        # if self.schema_column_index_mismatches:
        #     schema_diagnostics["schema_column_index_mismatches"] = self.schema_column_index_mismatches
        if self.schema_comparator:
            changes = []
            if self.schema_comparator.schema_column_additions:
                schema_diagnostics["schema_column_additions"] = self.schema_comparator.schema_column_additions
            if self.schema_comparator.schema_column_deletions:
                schema_diagnostics["schema_column_deletions"] = self.schema_comparator.schema_column_deletions
            if self.schema_comparator.schema_column_type_changes:
                schema_diagnostics["schema_column_type_changes"] = self.schema_comparator.schema_column_type_changes
            if self.schema_comparator.schema_column_index_changes:
                schema_diagnostics["schema_column_index_changes"] = self.schema_comparator.schema_column_index_changes
            if changes:
                schema_diagnostics["changes"] = changes

        return schema_diagnostics

    def get_log_diagnostic_lines(self) -> List[str]:
        diagnostics_texts: List[str] = []

        if self.outcome in [CheckOutcome.FAIL, CheckOutcome.WARN]:
            if self.schema_missing_column_names:
                diagnostics_texts.append(
                    f"missing_column_names = {self.__list_of_texts(self.schema_missing_column_names)}"
                )
            if self.schema_present_column_names:
                diagnostics_texts.append(
                    f"forbidden_present_column_names = {self.__list_of_texts(self.schema_present_column_names)}"
                )
            if self.schema_column_type_mismatches:
                for (
                    column_name,
                    column_type_mismatch_data,
                ) in self.schema_column_type_mismatches.items():
                    expected_type = column_type_mismatch_data["expected_type"]
                    actual_type = column_type_mismatch_data["actual_type"]
                    diagnostics_texts.append(
                        f"column_type_mismatch[{column_name}] expected({expected_type}) actual({actual_type})"
                    )
            if self.schema_column_index_mismatches:
                for (
                    column_name,
                    column_index_mismatch_data,
                ) in self.schema_column_index_mismatches.items():
                    expected_index = column_index_mismatch_data["expected_index"]
                    actual_index = column_index_mismatch_data["actual_index"]
                    column_on_expected_index = column_index_mismatch_data["column_on_expected_index"]
                    column_on_expected_index_text = (
                        f"column_on_expected_index({column_on_expected_index})" if column_on_expected_index else ""
                    )
                    diagnostics_texts.append(
                        f"column_index_mismatch[{column_name}] expected({expected_index}) actual({actual_index}){column_on_expected_index_text}"
                    )

            if self.schema_comparator:
                if self.schema_comparator.schema_column_additions:
                    diagnostics_texts.append(
                        f"column_additions = {self.__list_of_texts(self.schema_comparator.schema_column_additions)}"
                    )
                if self.schema_comparator.schema_column_deletions:
                    diagnostics_texts.append(
                        f"column_deletions = {self.__list_of_texts(self.schema_comparator.schema_column_deletions)}"
                    )
                if self.schema_comparator.schema_column_type_changes:
                    for (
                        column_name,
                        column_type_change_data,
                    ) in self.schema_comparator.schema_column_type_changes.items():
                        previous_type = column_type_change_data["previous_type"]
                        new_type = column_type_change_data["new_type"]
                        diagnostics_texts.append(
                            f"column_type_change[{column_name}] new_type({new_type}) previous_type({previous_type})"
                        )
                if self.schema_comparator.schema_column_index_changes:
                    changes = []
                    for (
                        column_name,
                        column_index_change_data,
                    ) in self.schema_comparator.schema_column_index_changes.items():
                        previous_index = column_index_change_data["previous_index"]
                        new_index = column_index_change_data["new_index"]
                        changes.append(f"{column_name}[{previous_index}->{new_index}]")
                    changes_txt = ", ".join(changes)
                    diagnostics_texts.append(f"column_index_changes = {changes_txt}")

            diagnostics_texts.append(f"schema_measured = {self.__schema_diagnostics_texts()}")
        return diagnostics_texts

    def __list_of_texts(self, texts):
        elements = ", ".join(texts)
        return f"[{elements}]"

    def __schema_diagnostics_texts(self):
        return self.__list_of_texts((column["name"] + " " + column["type"]) for column in self.measured_schema)
