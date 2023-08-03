from __future__ import annotations

import re
from dataclasses import dataclass, field

from soda.cloud.historic_descriptor import HistoricChangeOverTimeDescriptor
from soda.execution.check.check import Check
from soda.execution.check_outcome import CheckOutcome
from soda.execution.metric.metric import Metric
from soda.execution.partition import Partition
from soda.execution.schema_comparator import SchemaComparator
from soda.sodacl.change_over_time_cfg import ChangeOverTimeCfg
from soda.sodacl.schema_check_cfg import SchemaCheckCfg, SchemaValidations

KEY_SCHEMA_MEASURED = "schema_measured"
KEY_SCHEMA_PREVIOUS = "schema_previous"


@dataclass
class SchemaCheckValidationResult:
    missing_column_names: list[str] = field(default_factory=list)
    present_column_names: list[str] = field(default_factory=list)
    column_type_mismatches: dict[str, str] = field(default_factory=dict)
    column_index_mismatches: dict[str, str] = field(default_factory=dict)
    column_additions: list = field(default_factory=list)
    column_deletions: list = field(default_factory=list)
    column_type_changes: dict = field(default_factory=dict)
    column_index_changes: dict = field(default_factory=dict)


class SchemaCheck(Check):
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

        self.measured_schema: list[dict[str, str]] | None = None
        self.schema_comparator = None

        self.warn_result: SchemaCheckValidationResult | None = None
        self.fail_result: SchemaCheckValidationResult | None = None

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

    def evaluate(self, metrics: dict[str, Metric], historic_values: dict[str, object]):
        schema_check_cfg: SchemaCheckCfg = self.check_cfg

        self.measured_schema: list[dict[str, str]] = metrics.get(KEY_SCHEMA_MEASURED).value

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

        if schema_previous:
            self.schema_comparator = SchemaComparator(schema_previous, self.measured_schema)
        else:
            if schema_check_cfg.has_change_validations():
                warning_message = "Skipping schema checks since there is no historic schema metrics!"
                self.logs.warning(warning_message)
                self.add_outcome_reason(outcome_type="notEnoughHistory", message=warning_message, severity="warn")
                return

        self.warn_result = self.get_schema_violations(schema_check_cfg.warn_validations)
        self.fail_result = self.get_schema_violations(schema_check_cfg.fail_validations)

        if self.fail_result:
            self.outcome = CheckOutcome.FAIL
        elif self.warn_result:
            self.outcome = CheckOutcome.WARN
        else:
            self.outcome = CheckOutcome.PASS

    def get_schema_violations(
        self,
        schema_validations: SchemaValidations,
    ) -> SchemaCheckValidationResult | None:
        if schema_validations is None:
            return None

        measured_schema = self.measured_schema

        measured_column_names = [column["name"] for column in measured_schema]

        column_types = {column["name"]: column["type"] for column in measured_schema}

        schema_missing_column_names = []
        schema_present_column_names = []
        schema_column_type_mismatches = {}
        schema_column_index_mismatches = {}

        schema_column_additions = []
        schema_column_deletions = []
        schema_column_type_changes = {}
        schema_column_index_changes = {}

        required_column_names = set()
        if schema_validations.required_column_names:
            required_column_names.update(schema_validations.required_column_names)
        if schema_validations.required_column_types:
            required_column_names.update(schema_validations.required_column_types.keys())

        if required_column_names:
            for required_column_name in required_column_names:
                if required_column_name not in measured_column_names:
                    schema_missing_column_names.append(required_column_name)

        if schema_validations.forbidden_column_names:
            for forbidden_column_name in schema_validations.forbidden_column_names:
                regex = forbidden_column_name.replace("%", ".*").replace("*", ".*")
                forbidden_pattern = re.compile(regex)
                for column_name in measured_column_names:
                    if forbidden_pattern.match(column_name):
                        schema_present_column_names.append(column_name)

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
                        schema_column_type_mismatches[expected_column_name] = {
                            "expected_type": expected_column_type,
                            "actual_type": column_types[expected_column_name],
                        }

        if schema_validations.required_column_indexes:
            for required_column_name in schema_validations.required_column_indexes:
                if required_column_name not in column_types:
                    schema_missing_column_names.append(required_column_name)

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
                        schema_column_index_mismatches[expected_column_name] = {
                            "expected_index": expected_index,
                            "actual_index": actual_index,
                            "column_on_expected_index": column_on_index,
                        }
        if self.schema_comparator:
            if schema_validations.is_column_addition_forbidden:
                schema_column_additions = self.schema_comparator.schema_column_additions

            if schema_validations.is_column_deletion_forbidden:
                schema_column_deletions = self.schema_comparator.schema_column_deletions

            if schema_validations.is_column_type_change_forbidden:
                schema_column_type_changes = self.schema_comparator.schema_column_type_changes

            if schema_validations.is_column_index_change_forbidden:
                schema_column_index_changes = self.schema_comparator.schema_column_index_changes

        validation_results = [
            schema_missing_column_names,
            schema_present_column_names,
            schema_column_type_mismatches,
            schema_column_index_mismatches,
            schema_column_additions,
            schema_column_deletions,
            schema_column_type_changes,
            schema_column_index_changes,
        ]

        if any(validation_results):
            return SchemaCheckValidationResult(
                schema_missing_column_names,
                schema_present_column_names,
                schema_column_type_mismatches,
                schema_column_index_mismatches,
                schema_column_additions,
                schema_column_deletions,
                schema_column_type_changes,
                schema_column_index_changes,
            )

        return None

    def get_cloud_diagnostics_dict(self) -> dict:
        schema_diagnostics = {
            "blocks": [],
        }

        if self.measured_schema:
            columns_str = "\n".join([f'{c["name"]},{c["type"]}' for c in self.measured_schema])
            schema_diagnostics["blocks"].append(
                {
                    "type": "csv",
                    "text": f"Column,Type\n{columns_str}",
                    "title": "Schema",
                }
            )

        fail_change_events: list(dict(str, str)) = (
            self.__build_change_events(self.fail_result) if self.fail_result else []
        )
        warn_change_events: list(dict(str, str)) = (
            self.__build_change_events(self.warn_result) if self.warn_result else []
        )

        event_count = len(fail_change_events) + len(warn_change_events)

        if event_count > 0:
            fail_change_events_text = self.__build_change_events_text(fail_change_events, CheckOutcome.FAIL.name)
            warn_change_events_text = self.__build_change_events_text(warn_change_events, CheckOutcome.WARN.name)
            change_events_text = "\n".join([fail_change_events_text, warn_change_events_text])

            schema_diagnostics["blocks"].append(
                {
                    "type": "csv",
                    "text": f"Column,Event,Details\n{change_events_text}",
                    "title": "Diagnostics",
                }
            )

        schema_diagnostics["preferredChart"] = "bars"
        schema_diagnostics["valueLabel"] = f"{event_count} schema event(s)"
        schema_diagnostics["valueSeries"] = {
            "values": [
                {
                    "label": CheckOutcome.FAIL,
                    "value": len(fail_change_events),
                    "outcome": CheckOutcome.FAIL,
                },
                {
                    "label": CheckOutcome.WARN,
                    "value": len(warn_change_events),
                    "outcome": CheckOutcome.WARN,
                },
                {
                    "label": CheckOutcome.PASS,
                    "value": len(self.measured_schema) - event_count,
                    "outcome": CheckOutcome.PASS,
                },
            ]
        }

        return schema_diagnostics

    def __build_change_events(self, schema_validation_result: SchemaCheckValidationResult) -> list(dict(str, str)):
        change_events: list(dict(str, str)) = []

        for column in schema_validation_result.column_deletions:
            change_events.append(
                {
                    "column": column,
                    "event": "Column Deleted",
                    "details": "",
                }
            )
        for column in schema_validation_result.column_additions:
            change_events.append(
                {
                    "column": column,
                    "event": "Column Added",
                    "details": "",
                }
            )
        for column, index_change in schema_validation_result.column_index_changes.items():
            change_events.append(
                {
                    "column": column,
                    "event": "Index Changed",
                    "details": f"Previous Index: {index_change['previous_index']}; New Index: {index_change['new_index']}",
                }
            )
        for column, type_change in schema_validation_result.column_type_changes.items():
            change_events.append(
                {
                    "column": column,
                    "event": "Type Changed",
                    "details": f"Previous Type: {type_change['previous_type']}; New Type: {type_change['new_type']}",
                }
            )

        # Create diagnostics blocks without previous schema comparison as well.
        # Simply take outcome type and count of all changes.
        for column, index_mismatch in schema_validation_result.column_index_mismatches.items():
            change_events.append(
                {
                    "column": column,
                    "event": "Index Mismatch",
                    "details": f"Expected Index: {index_mismatch['expected_index']}; Actual Index: {index_mismatch['actual_index']}",
                }
            )
        for column, type_mismatch in schema_validation_result.column_type_mismatches.items():
            change_events.append(
                {
                    "column": column,
                    "event": "Type Mismatch",
                    "details": f"Expected Type: {type_mismatch['expected_type']}; Actual Type: {type_mismatch['actual_type']}",
                }
            )
        for column in schema_validation_result.present_column_names:
            change_events.append(
                {
                    "column": column,
                    "event": "Forbidden Column Present",
                    "details": "",
                }
            )
        for column in schema_validation_result.missing_column_names:
            change_events.append(
                {
                    "column": column,
                    "event": "Required Column Missing",
                    "details": "",
                }
            )
        return change_events

    def __build_change_events_text(self, change_events: list, event_type: str) -> str:
        return "\n".join(
            f"{ch['column']},:icon-{event_type.lower()}: {ch['event']}, {ch['details']}" for ch in change_events
        )

    def get_log_diagnostic_lines(self) -> list[str]:
        diagnostics_texts: list[str] = []

        if self.outcome in [CheckOutcome.FAIL, CheckOutcome.WARN]:
            if self.fail_result:
                diagnostics_texts.extend(
                    self.__build_log_diagnostic_lines(self.fail_result, CheckOutcome.FAIL.name.lower())
                )
            if self.warn_result:
                diagnostics_texts.extend(
                    self.__build_log_diagnostic_lines(self.warn_result, CheckOutcome.WARN.name.lower())
                )

        diagnostics_texts.append(f"schema_measured = {self.__schema_diagnostics_texts()}")

        return diagnostics_texts

    def __build_log_diagnostic_lines(
        self, schema_validation_result: SchemaCheckValidationResult, type: str
    ) -> list[str]:
        diagnostics_texts: list[str] = []

        if schema_validation_result.missing_column_names:
            diagnostics_texts.append(
                f"{type}_missing_column_names = {self.__list_of_texts(schema_validation_result.missing_column_names)}"
            )
        if schema_validation_result.present_column_names:
            diagnostics_texts.append(
                f"{type}_forbidden_present_column_names = {self.__list_of_texts(schema_validation_result.present_column_names)}"
            )
        if schema_validation_result.column_type_mismatches:
            for (
                column_name,
                column_type_mismatch_data,
            ) in schema_validation_result.column_type_mismatches.items():
                expected_type = column_type_mismatch_data["expected_type"]
                actual_type = column_type_mismatch_data["actual_type"]
                diagnostics_texts.append(
                    f"{type}_column_type_mismatch[{column_name}] expected({expected_type}) actual({actual_type})"
                )
        if schema_validation_result.column_index_mismatches:
            for (
                column_name,
                column_index_mismatch_data,
            ) in schema_validation_result.column_index_mismatches.items():
                expected_index = column_index_mismatch_data["expected_index"]
                actual_index = column_index_mismatch_data["actual_index"]
                column_on_expected_index = column_index_mismatch_data["column_on_expected_index"]
                column_on_expected_index_text = (
                    f"{type}_column_on_expected_index({column_on_expected_index})" if column_on_expected_index else ""
                )
                diagnostics_texts.append(
                    f"{type}_column_index_mismatch[{column_name}] expected({expected_index}) actual({actual_index}){column_on_expected_index_text}"
                )

        if self.schema_comparator:
            if schema_validation_result.column_additions:
                diagnostics_texts.append(
                    f"{type}_column_additions = {self.__list_of_texts(schema_validation_result.column_additions)}"
                )
            if schema_validation_result.column_deletions:
                diagnostics_texts.append(
                    f"{type}_column_deletions = {self.__list_of_texts(schema_validation_result.column_deletions)}"
                )
            if schema_validation_result.column_type_changes:
                for (
                    column_name,
                    column_type_change_data,
                ) in schema_validation_result.column_type_changes.items():
                    previous_type = column_type_change_data["previous_type"]
                    new_type = column_type_change_data["new_type"]
                    diagnostics_texts.append(
                        f"{type}_column_type_change[{column_name}] new_type({new_type}) previous_type({previous_type})"
                    )
            if schema_validation_result.column_index_changes:
                changes = []
                for (
                    column_name,
                    column_index_change_data,
                ) in schema_validation_result.column_index_changes.items():
                    previous_index = column_index_change_data["previous_index"]
                    new_index = column_index_change_data["new_index"]
                    changes.append(f"{column_name}[{previous_index}->{new_index}]")
                changes_txt = ", ".join(changes)
                diagnostics_texts.append(f"{type}_column_index_changes = {changes_txt}")

        return diagnostics_texts

    def __list_of_texts(self, texts):
        elements = ", ".join(texts)
        return f"[{elements}]"

    def __schema_diagnostics_texts(self) -> str:
        if not self.measured_schema:
            return "[]"

        return self.__list_of_texts((column["name"] + " " + column["type"]) for column in self.measured_schema)
