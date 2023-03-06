from __future__ import annotations

import functools
import logging
import os
import re
from datetime import timedelta
from numbers import Number
from textwrap import dedent

from antlr4 import CommonTokenStream, InputStream
from antlr4.error.ErrorListener import ErrorListener
from soda.common.logs import Logs
from soda.common.parser import Parser
from soda.common.yaml_helper import to_yaml_str
from soda.sodacl.antlr.SodaCLAntlrLexer import SodaCLAntlrLexer
from soda.sodacl.antlr.SodaCLAntlrParser import SodaCLAntlrParser
from soda.sodacl.change_over_time_cfg import ChangeOverTimeCfg
from soda.sodacl.check_cfg import CheckCfg
from soda.sodacl.data_source_check_cfg import (
    AutomatedMonitoringCfg,
    DiscoverTablesCfg,
    ProfileColumnsCfg,
    SampleTablesCfg,
)
from soda.sodacl.distribution_check_cfg import DistributionCheckCfg
from soda.sodacl.for_each_column_cfg import ForEachColumnCfg
from soda.sodacl.for_each_dataset_cfg import ForEachDatasetCfg
from soda.sodacl.freshness_check_cfg import FreshnessCheckCfg
from soda.sodacl.missing_and_valid_cfg import CFG_MISSING_VALID_ALL, MissingAndValidCfg
from soda.sodacl.name_filter import NameFilter
from soda.sodacl.reference_check_cfg import ReferenceCheckCfg
from soda.sodacl.row_count_comparison_check_cfg import RowCountComparisonCheckCfg
from soda.sodacl.schema_check_cfg import SchemaCheckCfg, SchemaValidations
from soda.sodacl.sodacl_cfg import SodaCLCfg
from soda.sodacl.table_cfg import TableCfg
from soda.sodacl.threshold_cfg import ThresholdCfg

logger = logging.getLogger(__name__)

WARN = "warn"
FAIL = "fail"
NAME = "name"
IDENTITY = "identity"
ATTRIBUTES = "attributes"
FAIL_CONDITION = "fail condition"
FAIL_QUERY = "fail query"
SAMPLES_LIMIT = "samples limit"
WHEN_REQUIRED_COLUMN_MISSING = "when required column missing"
WHEN_WRONG_COLUMN_TYPE = "when wrong column type"
WHEN_WRONG_COLUMN_INDEX = "when wrong column index"
WHEN_FORBIDDEN_COLUMN_PRESENT = "when forbidden column present"
WHEN_SCHEMA_CHANGES = "when schema changes"
ALL_SCHEMA_VALIDATIONS = [
    WHEN_REQUIRED_COLUMN_MISSING,
    WHEN_WRONG_COLUMN_TYPE,
    WHEN_WRONG_COLUMN_INDEX,
    WHEN_FORBIDDEN_COLUMN_PRESENT,
    WHEN_SCHEMA_CHANGES,
]


# Generic log messages for SODACL parser
QUOTE_CHAR_ERROR_LOG = """It looks like quote characters are present in one of more of your {dataset_type}
dataset identifiers. This may result in erroneous or no matches as most data sources do not
support such characters in table names.
"""


class SodaCLParser(Parser):
    def __init__(
        self,
        sodacl_cfg: SodaCLCfg,
        logs: Logs,
        file_path: str,
        data_source_name: str,
    ):
        super().__init__(file_path=file_path, logs=logs)
        from soda.sodacl.sodacl_cfg import SodaCLCfg

        self.sodacl_cfg: SodaCLCfg = sodacl_cfg
        self.data_source_name = data_source_name

    def assert_header_content_is_dict(func):
        @functools.wraps(func)
        def handler(self, header_str, header_content):
            if isinstance(header_content, dict):
                return func(self, header_str, header_content)
            else:
                self.logs.error(
                    f'Skipping section "{header_str}" because content is not an object/dict',
                    location=self.location,
                )

        return handler

    def parse_sodacl_yaml_str(self, sodacl_yaml_str: str):
        sodacl_dict = self._parse_yaml_str(sodacl_yaml_str)
        if sodacl_dict is not None:
            self.__parse_headers(sodacl_dict)

    def __check_str_list_has_quotes(self, check_str_list: list[str]):
        return any(x in (" ").join(check_str_list) for x in ['"', "'"])

    def __parse_headers(self, headers_dict: dict) -> None:
        if not headers_dict:
            return

        for header_str, header_content in headers_dict.items():
            # Backwards compatibility warning
            if "for each table" in header_str:
                self.logs.warning(
                    f"Please update 'for each table ...' to 'for each dataset ...'.", location=self.location
                )

            self._push_path_element(header_str, header_content)
            try:
                if "automated monitoring" == header_str:
                    self.__parse_automated_monitoring_section(header_str, header_content)
                elif header_str.startswith("profile columns"):
                    self.__parse_profile_columns_section(header_str, header_content)
                elif header_str.startswith("discover datasets") or header_str.startswith("discover tables"):
                    self.__parse_discover_tables_section(header_str, header_content)
                elif header_str.startswith("sample datasets") or header_str.startswith("sample tables"):
                    self.__parse_sample_datasets_section(header_str, header_content)
                elif "checks" == header_str:
                    self.__parse_data_source_checks_section(header_str, header_content)
                elif "variables" == header_str:
                    self.__parse_header_section(header_str, header_content)
                else:
                    antlr_parser = self.antlr_parse_section_header(header_str)
                    if antlr_parser.is_ok():
                        antlr_section_header = antlr_parser.result

                        if antlr_section_header.table_checks_header():
                            self.__parse_table_checks_section(
                                antlr_section_header.table_checks_header(),
                                self._resolve_jinja(header_str, self.sodacl_cfg.scan._variables),
                                header_content,
                            )
                        elif antlr_section_header.column_configurations_header():
                            self.__parse_column_configurations_section(
                                antlr_section_header.column_configurations_header(),
                                header_str,
                                header_content,
                            )
                        elif antlr_section_header.table_filter_header():
                            self.__parse_table_filter_section(
                                antlr_section_header.table_filter_header(), header_str, header_content
                            )
                        elif antlr_section_header.checks_for_each_dataset_header():
                            self.__parse_antlr_checks_for_each_dataset_section(
                                antlr_section_header.checks_for_each_dataset_header(),
                                header_str,
                                header_content,
                            )
                        elif antlr_section_header.checks_for_each_column_header():
                            self.__parse_antlr_checks_for_each_column_section(
                                antlr_section_header.checks_for_each_column_header(),
                                header_str,
                                header_content,
                            )
                        else:
                            self.logs.error(
                                f'Skipping unknown section header "{header_str}"',
                                location=self.location,
                            )
                    else:
                        self.logs.error(
                            f'Invalid section header "{header_str}"',
                            location=self.location,
                        )

            finally:
                self._pop_path_element()

    def __parse_table_checks_section(self, antlr_table_checks_header, header_str, header_content):
        if isinstance(header_content, list):
            table_name = self.__antlr_parse_identifier_name_from_header(antlr_table_checks_header)
            if table_name is None:
                self.logs.error(
                    f'No table name in section header "{header_str}"',
                    location=self.location,
                )

            data_source_scan_cfg = self.get_data_source_scan_cfgs()
            table_cfg: TableCfg = data_source_scan_cfg.get_or_create_table_cfg(table_name)

            partition_name = self.__antlr_parse_partition_from_header(antlr_table_checks_header)
            partition_cfg = table_cfg.find_partition(self.location.file_path, partition_name)
            if partition_cfg is None:
                self.logs.error(f"Filter not declared: {partition_name}", location=self.location)
                return None

            partition_cfg.locations.append(self.location)

            for check_index, check_list_element in enumerate(header_content):
                self._push_path_element(check_index, check_list_element)

                check_str, check_configurations = self.__parse_check_configuration(check_list_element)

                if check_str is not None:
                    check_cfg = self.__parse_table_check_str(header_str, check_str, check_configurations)

                    if check_cfg:
                        column_name = check_cfg.get_column_name()
                        if column_name:
                            column_checks = partition_cfg.get_or_create_column_checks(column_name)
                            column_checks.add_check_cfg(check_cfg)
                        else:
                            partition_cfg.add_check_cfg(check_cfg)

                self._pop_path_element()
        else:
            self.logs.error(
                f'Skipping section "{header_str}" because content is not a list',
                location=self.location,
            )

    def __parse_data_source_checks_section(self, header_str, header_content):
        if isinstance(header_content, list):
            data_source_scan_cfg = self.get_data_source_scan_cfgs()

            for check_index, check_list_element in enumerate(header_content):
                self._push_path_element(check_index, check_list_element)

                check_str, check_configurations = self.__parse_check_configuration(check_list_element)

                if check_str is not None:
                    check_cfg = self.__parse_data_source_check_str(header_str, check_str, check_configurations)

                    if check_cfg:
                        data_source_scan_cfg.add_check_cfg(check_cfg)
                    else:
                        self.logs.error(
                            f"Could not detect check type of '{check_str}'",
                            location=self.location,
                        )

                self._pop_path_element()
        else:
            self.logs.error(
                f'Skipping section "{header_str}" because content is not a list',
                location=self.location,
            )

    def __parse_table_check_str(self, header_str: str, check_str: str, check_configurations) -> CheckCfg:
        if check_str == "schema":
            return self.__parse_schema_check(header_str, check_str, check_configurations)

        elif check_str == "failed rows":
            return self.parse_user_defined_failed_rows_check_cfg(check_configurations, check_str, header_str)

        else:
            antlr_parser = self.antlr_parse_check(check_str)
            if antlr_parser.is_ok():
                antlr_check = antlr_parser.result
                if antlr_check.metric_check():
                    return self.__parse_metric_check(
                        antlr_check.metric_check(),
                        header_str,
                        check_str,
                        check_configurations,
                    )

                elif antlr_check.row_count_comparison_check():
                    return self.__parse_row_count_comparison_check(
                        antlr_check.row_count_comparison_check(),
                        header_str,
                        check_str,
                        check_configurations,
                    )

                elif antlr_check.reference_check():
                    return self.__parse_reference_check(
                        antlr_check.reference_check(),
                        header_str,
                        check_str,
                        check_configurations,
                    )

                elif antlr_check.freshness_check():
                    return self.__parse_freshness_check(
                        antlr_check.freshness_check(),
                        header_str,
                        check_str,
                        check_configurations,
                    )
            else:
                self.logs.error(f'Invalid check "{check_str}": {antlr_parser.error_message}')

    def __parse_data_source_check_str(
        self,
        header_str: str,
        check_str: str,
        check_configurations: dict | None,
    ) -> CheckCfg:
        if check_str == "failed rows":
            return self.parse_failed_rows_data_source_query_check(header_str, check_str, check_configurations)

        else:
            antlr_parser = self.antlr_parse_check(check_str)
            if antlr_parser.is_ok():
                antlr_check = antlr_parser.result
                if antlr_check.metric_check():
                    return self.__parse_metric_check(
                        antlr_check.metric_check(),
                        header_str,
                        check_str,
                        check_configurations,
                    )
                else:
                    self.logs.error(f'Check type "{check_str}" must be in "checks for TABLENAME:" section.')
            else:
                self.logs.error(f'Invalid check "{check_str}": {antlr_parser.error_message}')

    def __parse_header_section(self, header_str, header_content):
        if isinstance(header_content, dict):
            variables = self.sodacl_cfg.scan._variables
            for variable_name in header_content:
                variable_value = header_content[variable_name]
                if "${" in variable_value:
                    variable_value = self._resolve_jinja(variable_value, variables)
                variables[variable_name] = variable_value
        else:
            self.logs.error(f"Variables content must be a dict.  Was {type(header_content).__name__}")

    def parse_user_defined_failed_rows_check_cfg(self, check_configurations, check_str, header_str):
        if isinstance(check_configurations, dict):
            from soda.sodacl.user_defined_failed_rows_check_cfg import (
                UserDefinedFailedRowsCheckCfg,
            )
            from soda.sodacl.user_defined_failed_rows_expression_check_cfg import (
                UserDefinedFailedRowsExpressionCheckCfg,
            )

            self._push_path_element(check_str, check_configurations)
            try:
                name = self._get_optional(NAME, str)
                for invalid_configuration_key in [
                    key
                    for key in check_configurations
                    if key not in [NAME, WARN, FAIL, FAIL_CONDITION, FAIL_QUERY, SAMPLES_LIMIT, ATTRIBUTES]
                ]:
                    self.logs.error(
                        f'Invalid user defined failed rows check configuration key "{invalid_configuration_key}"',
                        location=self.location,
                    )
                fail_condition_sql_expr = self._get_optional(FAIL_CONDITION, str)
                samples_limit = self._get_optional(SAMPLES_LIMIT, int)
                if fail_condition_sql_expr:
                    return UserDefinedFailedRowsExpressionCheckCfg(
                        source_header=header_str,
                        source_line=check_str,
                        source_configurations=check_configurations,
                        location=self.location,
                        name=name,
                        fail_condition_sql_expr=fail_condition_sql_expr,
                        samples_limit=samples_limit,
                    )
                else:
                    fail_query = self._get_optional(FAIL_QUERY, str)
                    if fail_query:
                        return UserDefinedFailedRowsCheckCfg(
                            source_header=header_str,
                            source_line=check_str,
                            source_configurations=check_configurations,
                            location=self.location,
                            name=name,
                            query=fail_query,
                            samples_limit=samples_limit,
                        )
                    else:
                        self.logs.error(
                            'In a "failed rows" check, either "fail condition" or "fail query" are required as nested configurations.',
                            location=self.location,
                        )

            finally:
                self._pop_path_element()
        else:
            self.logs.error(f'Check "{check_str}" expects a nested object/dict, but was {check_configurations}')

    def __parse_failed_rows_table_expression_check(
        self,
        header_str: str,
        check_str: str,
        check_configurations: dict | None,
    ) -> CheckCfg:
        if isinstance(check_configurations, dict):
            from soda.sodacl.user_defined_failed_rows_expression_check_cfg import (
                UserDefinedFailedRowsExpressionCheckCfg,
            )

            self._push_path_element(check_str, check_configurations)
            try:
                expression = self._get_required("failed rows expression", str)
                name = self._get_optional(NAME, str)
                return UserDefinedFailedRowsExpressionCheckCfg(
                    source_header=header_str,
                    source_line=check_str,
                    source_configurations=check_configurations,
                    location=self.location,
                    name=name,
                    fail_condition_sql_expr=expression,
                )
            finally:
                self._pop_path_element()
        else:
            self.logs.error(f'Check "{check_str}" expects a nested object/dict, but was {check_configurations}')

    def parse_failed_rows_data_source_query_check(
        self,
        header_str: str,
        check_str: str,
        check_configurations: dict | None,
    ) -> CheckCfg:
        from soda.sodacl.user_defined_failed_rows_check_cfg import (
            UserDefinedFailedRowsCheckCfg,
        )

        if isinstance(check_configurations, dict):
            self._push_path_element(check_str, check_configurations)
            try:
                name = self._get_optional(NAME, str)
                query = self._get_required(FAIL_QUERY, str)
                samples_limit = self._get_optional(SAMPLES_LIMIT, int)
                return UserDefinedFailedRowsCheckCfg(
                    source_header=header_str,
                    source_line=check_str,
                    source_configurations=check_configurations,
                    location=self.location,
                    name=name,
                    query=query,
                    samples_limit=samples_limit,
                )
            finally:
                self._pop_path_element()
        else:
            self.logs.error(
                f'"failed rows" check must have configurations',
                location=self.location,
            )

    def __parse_metric_check(
        self,
        antlr_metric_check,
        header_str: str,
        check_str: str,
        check_configurations: dict | None,
    ) -> CheckCfg:
        from soda.sodacl.metric_check_cfg import MetricCheckCfg

        antlr_metric = antlr_metric_check.metric()
        metric_name = antlr_metric.metric_name().getText()
        metric_args = None
        if antlr_metric.metric_args():
            metric_args = [
                self.__parse_metric_arg(metric_arg)
                for metric_arg in antlr_metric.metric_args().getChildren()
                if isinstance(metric_arg, SodaCLAntlrParser.Metric_argContext)
            ]

        antlr_threshold = antlr_metric_check.threshold()
        fail_threshold_cfg = None
        warn_threshold_cfg = None
        if antlr_threshold:
            pass_threshold_cfg = self.__antlr_parse_threshold_condition(antlr_threshold)
            fail_threshold_cfg = pass_threshold_cfg.get_inverse()
        elif isinstance(check_configurations, dict):
            self._push_path_element(check_str, check_configurations)
            fail_threshold_condition_str = self._get_optional(FAIL, str)
            fail_threshold_cfg = self.__parse_configuration_threshold_condition(fail_threshold_condition_str)
            warn_threshold_condition_str = self._get_optional(WARN, str)
            warn_threshold_cfg = self.__parse_configuration_threshold_condition(warn_threshold_condition_str)
            self._pop_path_element()

        # Parse the nested check configuration details
        name = None
        filter = None
        method = None
        missing_and_valid_cfg = None
        condition = None
        metric_expression = None
        metric_query = None
        samples_limit = None

        if isinstance(check_configurations, dict):
            for configuration_key in check_configurations:
                configuration_value = check_configurations[configuration_key]
                self._push_path_element(check_str, check_configurations)
                name = self._get_optional(NAME, str)
                samples_limit = self._get_optional(SAMPLES_LIMIT, int)
                self._push_path_element(configuration_key, configuration_value)
                if "filter" == configuration_key:
                    filter = configuration_value.strip()
                elif "condition" == configuration_key:
                    condition = configuration_value.strip()
                elif "method" == configuration_key:
                    method = configuration_value.strip()
                elif configuration_key.endswith("expression"):
                    metric_expression = configuration_value.strip()
                    configuration_metric_name = (
                        configuration_key[: -len(" expression")]
                        if len(configuration_key) > len(" expression")
                        else None
                    )
                    if configuration_metric_name != metric_name:
                        self.logs.error(
                            f'In configuration "{configuration_key}" the metric name must match exactly the metric name in the check "{metric_name}"',
                            location=self.location,
                        )
                elif configuration_key.endswith("query"):
                    metric_query = dedent(configuration_value).strip()
                    configuration_metric_name = (
                        configuration_key[: -len(" query")] if len(configuration_key) > len(" query") else None
                    )
                    if configuration_metric_name != metric_name:
                        self.logs.error(
                            f'In configuration "{configuration_key}" the metric name must match exactly the metric name in the check "{metric_name}"',
                            location=self.location,
                        )
                elif configuration_key in CFG_MISSING_VALID_ALL:
                    if missing_and_valid_cfg is None:
                        missing_and_valid_cfg = MissingAndValidCfg()
                    self.__parse_missing_and_valid(
                        configuration_key,
                        configuration_value,
                        missing_and_valid_cfg,
                    )
                elif configuration_key not in [NAME, IDENTITY, WARN, FAIL, SAMPLES_LIMIT, ATTRIBUTES]:
                    if metric_name != "distribution_difference":
                        self.logs.error(
                            f"Skipping unsupported check configuration: {configuration_key}",
                            doc="metric_check_configurations",
                            location=self.location,
                        )

                self._pop_path_element()
                self._pop_path_element()

        elif check_configurations is not None:
            self.logs.error(
                f"Check configuration details should be object/dict, but was {type(check_configurations).__name__}: \n{to_yaml_str(check_configurations)}",
                location=self.location,
            )

        if metric_name == "freshness":
            if metric_args:
                column_name = metric_args[0]
                variable_name = metric_args[1] if len(metric_args) > 1 else None

                fail_freshness_threshold = fail_threshold_cfg.get_freshness_threshold() if fail_threshold_cfg else None
                warn_freshness_threshold = warn_threshold_cfg.get_freshness_threshold() if warn_threshold_cfg else None

                return FreshnessCheckCfg(
                    source_header=header_str,
                    source_line=check_str,
                    source_configurations=check_configurations,
                    location=self.location,
                    name=name,
                    column_name=column_name,
                    variable_name=variable_name,
                    fail_freshness_threshold=fail_freshness_threshold,
                    warn_freshness_threshold=warn_freshness_threshold,
                )
            else:
                self.logs.error("Metric freshness must have at least 1 arg", location=self.location)
                return None

        metric_check_cfg_class = MetricCheckCfg

        change_over_time_cfg = None
        if antlr_metric_check.change_over_time():
            from soda.sodacl.change_over_time_metric_check_cfg import (
                ChangeOverTimeMetricCheckCfg,
            )

            metric_check_cfg_class = ChangeOverTimeMetricCheckCfg
            change_over_time_cfg = ChangeOverTimeCfg()
            antlr_change_over_time = antlr_metric_check.change_over_time()
            antlr_change_over_time_config = antlr_change_over_time.change_over_time_config()

            if antlr_change_over_time.percent():
                change_over_time_cfg.percent = True

            if antlr_change_over_time_config:
                if antlr_change_over_time_config.LAST():
                    change_over_time_cfg.last_measurements = int(antlr_change_over_time_config.integer().getText())
                    change_over_time_cfg.last_aggregation = antlr_change_over_time_config.change_aggregation().getText()
                elif antlr_change_over_time_config.same_day_last_week():
                    change_over_time_cfg.same_day_last_week = True
            else:
                change_over_time_cfg.last_measurements = 1
                change_over_time_cfg.last_aggregation = "min"

        if antlr_metric_check.anomaly_score():
            from soda.sodacl.anomaly_metric_check_cfg import AnomalyMetricCheckCfg

            metric_check_cfg_class = AnomalyMetricCheckCfg

            if not antlr_metric_check.default_anomaly_threshold():
                return metric_check_cfg_class(
                    source_header=header_str,
                    source_line=check_str,
                    source_configurations=check_configurations,
                    location=self.location,
                    name=name,
                    metric_name=metric_name,
                    metric_args=metric_args,
                    missing_and_valid_cfg=missing_and_valid_cfg,
                    filter=filter,
                    condition=condition,
                    metric_expression=metric_expression,
                    metric_query=metric_query,
                    change_over_time_cfg=change_over_time_cfg,
                    fail_threshold_cfg=None,
                    warn_threshold_cfg=None,
                )
            if antlr_metric_check.default_anomaly_threshold():
                warn_threshold_cfg = ThresholdCfg(gt=0.9)

            # NOTE: Right now we are not using this parsing as we require '< default' syntax but when that changes
            # this section will come in handy to parse user-provided thresholds for anomaly detection.
            elif antlr_threshold:
                warn_threshold_cfg = fail_threshold_cfg
                fail_threshold_cfg = None

            if (fail_threshold_cfg and not fail_threshold_cfg.is_valid_anomaly_threshold()) or (
                warn_threshold_cfg and not warn_threshold_cfg.is_valid_anomaly_threshold()
            ):
                if antlr_threshold:
                    self.logs.error(
                        'Invalid anomaly threshold.  Only "< default" or "< {threshold-value}" '
                        "are allowed where threshold-value must be between 0 (ok) and 1 (anomaly)",
                        location=self.location,
                    )
                else:
                    self.logs.error(
                        'Invalid anomaly threshold.  Only "when > {threshold-value}" '
                        "is allowed where threshold-value must be between 0 (ok) and 1 (anomaly)",
                        location=self.location,
                    )

        elif antlr_metric_check.default_anomaly_threshold():
            self.logs.error(
                'Threshold "< default" only allowed for anomaly checks that start with: "anomaly score '
                "{metric} < {threshold}",
                location=self.location,
            )
        elif metric_name == "distribution_difference":
            column_name: str = metric_args[0]
            distribution_name: str | None = metric_args[1] if len(metric_args) > 1 else None
            sample_clause = check_configurations.get("sample")

            if check_configurations.get("distribution reference file"):
                reference_file_path: str = os.path.join(
                    os.path.dirname(self.location.file_path), check_configurations.get("distribution reference file")
                )
            else:
                self.logs.error(
                    f"""You did not define a `distribution reference file` key. See the docs for more information:\n"""
                    f"""https://docs.soda.io/soda-cl/distribution.html#define-a-distribution-check""",
                    location=self.location,
                )
            if not fail_threshold_cfg and not warn_threshold_cfg:
                self.logs.error(
                    f"""You did not define a threshold for your distribution check. Please use the following syntax\n"""
                    f"""- distribution_difference(column_name, reference_distribution) > threshold: \n"""
                    f"""    distribution reference file: distribution_reference.yml""",
                    location=self.location,
                )

            return DistributionCheckCfg(
                source_header=header_str,
                source_line=check_str,
                source_configurations=check_configurations,
                location=self.location,
                name=name,
                column_name=column_name,
                distribution_name=distribution_name,
                filter=filter,
                sample_clause=sample_clause,
                method=method,
                reference_file_path=reference_file_path,
                fail_threshold_cfg=fail_threshold_cfg,
                warn_threshold_cfg=warn_threshold_cfg,
            )

        if fail_threshold_cfg is None and warn_threshold_cfg is None:
            self.logs.error(f'No threshold specified for check "{check_str}"', location=self.location)

        # Skip a check if more than one argument is used in a metric check that does not support that.
        # TODO: refactor this method into separate ones for different metric checks for clearer logic and better
        # organization of extra steps like validation that cannot be done on antlr level.
        if (
            metric_check_cfg_class == MetricCheckCfg
            and metric_args
            and len(metric_args) > 1
            and metric_name not in MetricCheckCfg.MULTI_METRIC_CHECK_TYPES
        ):
            self.logs.warning(
                f"Invalid syntax used in '{check_str}'. More than one check attribute is not supported. A check like this will be skipped in future versions of Soda Core"
            )

        return metric_check_cfg_class(
            source_header=header_str,
            source_line=check_str,
            source_configurations=check_configurations,
            location=self.location,
            name=name,
            metric_name=metric_name,
            metric_args=metric_args,
            missing_and_valid_cfg=missing_and_valid_cfg,
            filter=filter,
            condition=condition,
            metric_expression=metric_expression,
            metric_query=metric_query,
            change_over_time_cfg=change_over_time_cfg,
            fail_threshold_cfg=fail_threshold_cfg,
            warn_threshold_cfg=warn_threshold_cfg,
            samples_limit=samples_limit,
        )

    def __parse_configuration_threshold_condition(self, value) -> ThresholdCfg | None:
        if isinstance(value, str):
            if not value.startswith("when "):
                self.logs.error(
                    'Value for fail must be "when {condition}"',
                    location=self.location,
                )
            else:
                threshold_str = value[len("when ") :]
                antlr_parser = self.antlr_parse_threshold(threshold_str)
                if antlr_parser.is_ok():
                    antlr_threshold = antlr_parser.result
                    return self.__antlr_parse_threshold_condition(antlr_threshold)
                else:
                    self.logs.error(f'Invalid threshold "{threshold_str}": {antlr_parser.error_message}')

    def __parse_metric_arg(self, antlr_metric_arg):
        antlr_signed_number = antlr_metric_arg.signed_number()
        if antlr_signed_number:
            return self.__antlr_parse_signed_number(antlr_signed_number)
        return antlr_metric_arg.identifier().getText()

    def __parse_column_configurations_section(self, antlr_configuration_header, header_str, header_content):
        table_name = self.__antlr_parse_identifier_name_from_header(antlr_configuration_header)
        if table_name is None:
            self.logs.error(
                f'Table not specified in "{header_str}"',
                location=self.location,
            )
            return

        if isinstance(header_content, dict):
            data_source_scan_cfg = self.get_data_source_scan_cfgs()
            table_cfg: TableCfg = data_source_scan_cfg.get_or_create_table_cfg(table_name)
            table_cfg.column_configuration_locations.append(self.location)

            for (
                configuration_key,
                configuration_value,
            ) in header_content.items():
                parts = configuration_key.split(" for ")
                if len(parts) != 2:
                    self.logs.error(
                        f"Column configuration key {configuration_key} not in appropriate format {{configuration_type}} for {{column}}",
                        location=self.location,
                    )
                else:
                    configuration_type = parts[0].strip()
                    column_name = parts[1].strip()
                    column_configurations_cfg = table_cfg.get_or_create_column_configurations(column_name)
                    if configuration_type in CFG_MISSING_VALID_ALL:
                        self.__parse_missing_and_valid(
                            configuration_type,
                            configuration_value,
                            column_configurations_cfg,
                        )
                    else:
                        self.logs.error(f'Invalid configuration "{configuration_key}"', location=self.location)
        elif isinstance(header_content, list) and len(header_content) > 0:
            self.logs.error(
                f'Contents of column configurations "{header_str}" is a list, but should be plain configurations.  Remove the "-" before the nested configurations',
                location=self.location,
            )
        else:
            self.logs.error(
                f'Skipping section "{header_str}" because content is not a object/dict',
                location=self.location,
            )

    def __parse_schema_check(self, header_str, check_str, check_configurations) -> SchemaCheckCfg | None:
        if isinstance(check_configurations, dict):
            self._push_path_element(check_str, check_configurations)
            for configuration_key in check_configurations:
                if configuration_key not in [NAME, WARN, FAIL, ATTRIBUTES]:
                    self.logs.error(
                        f'Invalid schema check configuration key "{configuration_key}"', location=self.location
                    )
            name = self._get_optional(NAME, str)
            schema_check_cfg = SchemaCheckCfg(
                source_header=header_str,
                source_line=check_str,
                source_configurations=check_configurations,
                location=self.location,
                name=name,
                warn_validations=self.__parse_schema_validations(WARN),
                fail_validations=self.__parse_schema_validations(FAIL),
            )
            self._pop_path_element()
            return schema_check_cfg
        else:
            self.logs.error(f'Check "{check_str}" expects a nested object/dict, but was {check_configurations}')

    def __parse_schema_validations(self, outcome_text: str):
        validations_dict = self._get_optional(outcome_text, dict)
        if validations_dict:
            self._push_path_element(outcome_text, validations_dict)

            is_column_addition_forbidden = False
            is_column_deletion_forbidden = False
            is_column_type_change_forbidden = False
            is_column_index_change_forbidden = False
            changes_not_allowed = validations_dict.get("when schema changes")
            if changes_not_allowed == "any":
                is_column_addition_forbidden = True
                is_column_deletion_forbidden = True
                is_column_type_change_forbidden = True
                is_column_index_change_forbidden = True
            elif isinstance(changes_not_allowed, list):
                for change_not_allowed in changes_not_allowed:
                    if change_not_allowed == "column add":
                        is_column_addition_forbidden = True
                    elif change_not_allowed == "column delete":
                        is_column_deletion_forbidden = True
                    elif change_not_allowed == "column type change":
                        is_column_type_change_forbidden = True
                    elif change_not_allowed == "column index change":
                        is_column_index_change_forbidden = True
                    else:
                        self.logs.error(f'"when schema changes" has invalid value {change_not_allowed}')
            elif changes_not_allowed is not None:
                self.logs.error(
                    f'Value for "when schema changes" must be either "any" or a list of these optioonal strings: {"column add", "column delete", "column type change", "column index change"}. Was {changes_not_allowed}'
                )

            schema_validations = SchemaValidations(
                required_column_names=self.__parse_schema_validation(WHEN_REQUIRED_COLUMN_MISSING),
                required_column_types=self.__parse_schema_validation(WHEN_WRONG_COLUMN_TYPE),
                required_column_indexes=self.__parse_schema_validation(WHEN_WRONG_COLUMN_INDEX),
                forbidden_column_names=self.__parse_schema_validation(WHEN_FORBIDDEN_COLUMN_PRESENT),
                is_column_addition_forbidden=is_column_addition_forbidden,
                is_column_deletion_forbidden=is_column_deletion_forbidden,
                is_column_type_change_forbidden=is_column_type_change_forbidden,
                is_column_index_change_forbidden=is_column_index_change_forbidden,
            )
            for invalid_schema_validation in [
                v
                for v in validations_dict
                if v
                not in [
                    WHEN_REQUIRED_COLUMN_MISSING,
                    WHEN_WRONG_COLUMN_TYPE,
                    WHEN_WRONG_COLUMN_INDEX,
                    WHEN_FORBIDDEN_COLUMN_PRESENT,
                    WHEN_SCHEMA_CHANGES,
                ]
            ]:
                hint = f"Available schema validations: {ALL_SCHEMA_VALIDATIONS}"
                if invalid_schema_validation == "when schema change":
                    hint = f'Did you mean "when schema changes" (plural)? {hint}'
                elif invalid_schema_validation == "when required columns missing":
                    hint = f'Did you mean "when required column missing"? (column in singular form) {hint}'
                elif invalid_schema_validation == "when forbidden columns present":
                    hint = f'Did you mean "when forbidden column present"? (column in singular form) {hint}'
                self.logs.error(
                    f'Invalid schema validation "{invalid_schema_validation}": {hint}',
                    location=self.location,
                )
            self._pop_path_element()
            return schema_validations

    def __parse_schema_validation(self, validation_type):
        value_type = (
            list
            if validation_type
            in [
                "when required column missing",
                "when forbidden column present",
            ]
            else dict
        )
        configuration_value = self._get_optional(validation_type, value_type)

        if configuration_value:
            if validation_type in [
                "when required column missing",
                "when forbidden column present",
            ]:
                are_values_valid = all(isinstance(c, str) for c in configuration_value)
            elif validation_type == "when wrong column type":
                are_values_valid = all(
                    isinstance(k, str) and isinstance(v, str) for k, v in configuration_value.items()
                )
            else:
                are_values_valid = all(
                    isinstance(k, str) and isinstance(v, int) for k, v in configuration_value.items()
                )
            if are_values_valid:
                return configuration_value
            else:
                self._push_path_element(validation_type, None)
                expected_configuration_type = (
                    "list of strings"
                    if validation_type
                    in [
                        "when required column missing",
                        "when forbidden column present",
                    ]
                    else "dict with strings for keys and values"
                    if validation_type == "when wrong column type"
                    else "dict with strings for keys and ints for values"
                )
                self.logs.error(
                    f'"{validation_type}" must contain {expected_configuration_type}',
                    location=self.location,
                )
                self._pop_path_element()

    def __parse_row_count_comparison_check(
        self,
        antlr_row_count_comparison_check,
        header_str: str,
        check_str: str,
        check_configurations: dict | None,
    ) -> CheckCfg:
        other_table_name = antlr_row_count_comparison_check.identifier(0).getText()
        antlr_identifier2 = antlr_row_count_comparison_check.identifier(1)

        other_partition_name = None
        antlr_partition_name = antlr_row_count_comparison_check.partition_name()
        if antlr_partition_name:
            other_partition_name = self.__antlr_parse_identifier(antlr_partition_name)

        other_data_source_name = antlr_identifier2.getText() if antlr_identifier2 else None

        name = None
        if isinstance(check_configurations, dict):
            self._push_path_element(check_str, check_configurations)
            name = self._get_optional(NAME, str)
            for configuration_key in check_configurations:
                if configuration_key != NAME:
                    self.logs.error(
                        f"Invalid row count comparison configuration key {configuration_key}", location=self.location
                    )
            self._pop_path_element()

        return RowCountComparisonCheckCfg(
            source_header=header_str,
            source_line=check_str,
            source_configurations=check_configurations,
            location=self.location,
            name=name,
            other_table_name=other_table_name,
            other_partition_name=other_partition_name,
            other_data_source_name=other_data_source_name,
        )

    def __parse_reference_check(
        self,
        antlr_reference_check,
        header_str: str,
        check_str: str,
        check_configurations: dict | None,
    ) -> CheckCfg:
        antlr_reference_check: SodaCLAntlrParser.Reference_checkContext = antlr_reference_check

        antlr_source_column_name_arg_list = antlr_reference_check.getTypedRuleContexts(
            SodaCLAntlrParser.Source_column_nameContext
        )
        source_column_names = [arg.getText() for arg in antlr_source_column_name_arg_list]

        target_table_name = antlr_reference_check.identifier().getText()

        antlr_target_column_name_arg_list = antlr_reference_check.getTypedRuleContexts(
            SodaCLAntlrParser.Target_column_nameContext
        )
        target_column_names = [arg.getText() for arg in antlr_target_column_name_arg_list]

        if len(source_column_names) == 0:
            self.logs.error(
                f"No source columns in reference check",
                location=self.location,
            )
        if len(target_column_names) == 0:
            self.logs.error(
                f"No target columns in reference check",
                location=self.location,
            )
        if len(source_column_names) != len(target_column_names):
            self.logs.error(
                f"Number of source and target column names must be equal",
                location=self.location,
            )

        name = None
        samples_limit = None
        if isinstance(check_configurations, dict):
            self._push_path_element(check_str, check_configurations)
            name = self._get_optional(NAME, str)
            samples_limit = self._get_optional(SAMPLES_LIMIT, int)

            for configuration_key in check_configurations:
                if configuration_key not in [NAME, SAMPLES_LIMIT, ATTRIBUTES]:
                    self.logs.error(
                        f"Invalid reference check configuration key {configuration_key}", location=self.location
                    )
            self._pop_path_element()

        return ReferenceCheckCfg(
            source_header=header_str,
            source_line=check_str,
            source_configurations=check_configurations,
            location=self.location,
            name=name,
            source_column_names=source_column_names,
            target_table_name=target_table_name,
            target_column_names=target_column_names,
            samples_limit=samples_limit,
        )

    def __parse_freshness_check(
        self,
        antlr_freshness_check,
        header_str: str,
        check_str: str,
        check_configurations: dict | None,
    ) -> CheckCfg:
        self.logs.warning(
            "Syntax of freshness check has changed and is deprecated.  Use freshness(column_name) < 24h30m  See docs"
        )

        antlr_freshness_check: SodaCLAntlrParser.Freshness_checkContext = antlr_freshness_check

        column_name = self.__antlr_parse_identifier(antlr_freshness_check.identifier())
        variable_name = None
        if antlr_freshness_check.freshness_variable():
            variable_name = self.__antlr_parse_identifier(antlr_freshness_check.freshness_variable().identifier())

        antlr_freshness_threshold = antlr_freshness_check.freshness_threshold_value()
        warn_freshness_threshold = None
        name = None
        if antlr_freshness_threshold:
            fail_freshness_threshold = self.parse_freshness_threshold(antlr_freshness_threshold.getText())
        else:
            self._push_path_element(check_str, check_configurations)
            fail_staleness_threshold_text = self._get_optional(FAIL, str)
            fail_freshness_threshold = self.parse_staleness_threshold_text(fail_staleness_threshold_text)
            warn_freshness_threshold_text = self._get_optional(WARN, str)
            warn_freshness_threshold = self.parse_staleness_threshold_text(warn_freshness_threshold_text)

            name = self._get_optional(NAME, str)
            for configuration_key in check_configurations:
                if configuration_key not in [NAME, WARN, FAIL, ATTRIBUTES]:
                    self.logs.error(f"Invalid freshness configuration key {configuration_key}", location=self.location)

            self._pop_path_element()

        return FreshnessCheckCfg(
            source_header=header_str,
            source_line=check_str,
            source_configurations=check_configurations,
            location=self.location,
            name=name,
            column_name=column_name,
            variable_name=variable_name,
            fail_freshness_threshold=fail_freshness_threshold,
            warn_freshness_threshold=warn_freshness_threshold,
        )

    def parse_staleness_threshold_text(self, staleness_threshold_text):
        if isinstance(staleness_threshold_text, str):
            if staleness_threshold_text.startswith("when > "):
                return self.parse_freshness_threshold(staleness_threshold_text[len("when > ") :])
            else:
                self.logs.error(
                    f'Invalid staleness threshold "{staleness_threshold_text}"',
                    location=self.location,
                )

    def parse_freshness_threshold(self, freshness_threshold_text: str) -> timedelta | None:
        try:
            days = 0
            hours = 0
            minutes = 0
            seconds = 0
            previous_unit = None
            match = re.match(r"(\d+[dhms])+(\d+)?", freshness_threshold_text)
            for group in match.groups():
                if isinstance(group, str):
                    if group.isdigit():
                        unit = previous_unit
                    else:
                        unit = group[-1:]

                    value = int(group[:-1])
                    if unit == "d":
                        days += value
                    elif unit == "h":
                        hours += value
                    elif unit == "m":
                        minutes += value
                    elif unit == "s":
                        seconds += value

                    previous_unit = unit

                return timedelta(days=days, hours=hours, minutes=minutes, seconds=seconds)
        except Exception as e:
            self.logs.error(
                f'Problem parsing freshness threshold "{freshness_threshold_text}"', location=self.location, exception=e
            )

    def __antlr_parse_threshold_condition(self, antlr_threshold) -> ThresholdCfg:
        antlr_comparator_threshold = antlr_threshold.comparator_threshold()
        if antlr_comparator_threshold:
            comparator = antlr_comparator_threshold.comparator().getText()
            antlr_threshold_value = antlr_comparator_threshold.threshold_value()
            threshold_value = self.__antlr_threshold_value(antlr_threshold_value)

            if comparator == "<":
                return ThresholdCfg(lt=threshold_value)
            if comparator == "<=":
                return ThresholdCfg(lte=threshold_value)
            if comparator == ">":
                return ThresholdCfg(gt=threshold_value)
            if comparator == ">=":
                return ThresholdCfg(gte=threshold_value)
            if comparator == "=":
                return ThresholdCfg(lte=threshold_value, gte=threshold_value)
            if comparator in ["!=", "<>"]:
                return ThresholdCfg(lt=threshold_value, gt=threshold_value, is_split_zone=True)
            self.logs.error(f"Unsupported comparator {comparator}", location=self.location)
            return None

        antlr_between_threshold: SodaCLAntlrParser.Between_thresholdContext = antlr_threshold.between_threshold()
        if antlr_between_threshold:
            lower_included = antlr_between_threshold.ROUND_LEFT() is None
            antlr_lower_value = antlr_between_threshold.threshold_value(0)
            lower_bound = self.__antlr_threshold_value(antlr_lower_value)
            upper_included = antlr_between_threshold.ROUND_RIGHT() is None
            antlr_upper_value = antlr_between_threshold.threshold_value(1)
            upper_bound = self.__antlr_threshold_value(antlr_upper_value)
            if lower_bound > upper_bound:
                self.logs.error(
                    f"Left lower bound should be less than the upper bound on the right {antlr_between_threshold.getText()}",
                    location=self.location,
                )
            threshold_condition_cfg = ThresholdCfg(
                lt=upper_bound if not upper_included else None,
                lte=upper_bound if upper_included else None,
                gt=lower_bound if not lower_included else None,
                gte=lower_bound if lower_included else None,
            )

            if antlr_between_threshold.NOT():
                threshold_condition_cfg = threshold_condition_cfg.get_inverse()

            return threshold_condition_cfg

        if antlr_threshold.anomaly_threshold():
            return AnomalyThresholdCfg()

        self.logs.error(f'Unknown threshold "{antlr_threshold.getText()}"', location=self.location)

    def __antlr_threshold_value(self, antlr_threshold_value: SodaCLAntlrParser.Threshold_valueContext):
        if antlr_threshold_value.signed_number():
            return self.__antlr_parse_signed_number(antlr_threshold_value.signed_number())
        if antlr_threshold_value.freshness_threshold_value():
            freshness_threshold = antlr_threshold_value.freshness_threshold_value().getText()
            return self.parse_freshness_threshold(freshness_threshold)
        if antlr_threshold_value.IDENTIFIER_UNQUOTED():
            return antlr_threshold_value.IDENTIFIER_UNQUOTED().getText()

    def __antlr_parse_signed_number(self, antlr_signed_number):
        signed_number_str = antlr_signed_number.getText()
        if signed_number_str.startswith("+"):
            signed_number_str = signed_number_str[1:]
        return float(signed_number_str)

    def __parse_missing_and_valid(
        self,
        configuration_type: str,
        configuration_value,
        missing_and_valid_cfg: MissingAndValidCfg,
    ) -> None:
        def set_configuration_value(value):
            field_name = configuration_type.replace(" ", "_")
            location_field_name = f"{field_name}_location"
            previous_location = getattr(missing_and_valid_cfg, location_field_name)
            if previous_location:
                self.logs.warning(
                    f"Overwriting {configuration_type} from {previous_location}",
                    location=self.location,
                )
            setattr(missing_and_valid_cfg, field_name, value)
            setattr(missing_and_valid_cfg, location_field_name, self.location)

        if configuration_type in ["valid values", "invalid values", "missing values"]:
            if all(isinstance(v, str) for v in configuration_value) or all(
                isinstance(v, Number) for v in configuration_value
            ):
                set_configuration_value(configuration_value)
            else:
                self.logs.error(
                    f"Only strings or only numbers allowed in {configuration_type}: {configuration_value}",
                    location=self.location,
                )

        elif configuration_type in [
            "valid min length",
            "valid max length",
            "valid length",
        ]:
            try:
                set_configuration_value(int(configuration_value))
            except ValueError:
                self.logs.error(
                    f"{configuration_type} must be an integer, but was {configuration_value}",
                    location=self.location,
                )

        elif configuration_type in ["valid min", "valid max"]:
            try:
                set_configuration_value(float(configuration_value))
            except ValueError:
                self.logs.error(
                    f"{configuration_type} must be an number (float), but was '{configuration_value}'",
                    location=self.location,
                )

        elif configuration_type in [
            "missing format",
            "missing regex",
            "valid regex",
            "valid format",
            "invalid regex",
            "invalid format",
        ]:
            if isinstance(configuration_value, str):
                set_configuration_value(configuration_value)
            else:
                self.logs.error(
                    f"{configuration_type} must be a string, but was {type(configuration_value).__name__}",
                    location=self.location,
                )

    def __parse_table_filter_section(self, antlr_table_filter_header, header_str, header_content):
        from soda.sodacl.partition_cfg import PartitionCfg

        if isinstance(header_content, dict):
            table_name = self.__antlr_parse_identifier_name_from_header(antlr_table_filter_header)
            data_source_scan_cfg = self.get_data_source_scan_cfgs()
            table_cfg: TableCfg = data_source_scan_cfg.get_or_create_table_cfg(table_name)

            partition_name = self.__antlr_parse_partition_from_header(antlr_table_filter_header)
            partition_cfg: PartitionCfg = table_cfg.create_partition(self.location.file_path, partition_name)
            partition_filter = header_content.get("where")
            if partition_filter is None:
                self.logs.error(
                    f'Expecting "where" with a SQL expression as a configuration under {header_str}',
                    location=self.location,
                )
            else:
                partition_cfg.sql_partition_filter = partition_filter

        else:
            self.logs.error(
                f'Skipping section "{header_str}" because content is not an object/dict',
                location=self.location,
            )

    def __parse_tables(self, header_content, data_source_check_cfg):
        data_source_check_cfg.data_source_name = header_content.get("data_source")
        datasets = header_content.get("datasets")
        if datasets is None:
            datasets = header_content.get("tables")
        if isinstance(datasets, list):
            for table in datasets:
                if table.startswith("exclude "):
                    exclude_table_expression = table[len("exclude ") :]
                    data_source_check_cfg.exclude_tables.append(exclude_table_expression)
                else:
                    if table.startswith("include "):
                        include_table_expression = table[len("include ") :]
                    else:
                        include_table_expression = table
                    data_source_check_cfg.include_tables.append(include_table_expression)
            if self.__check_str_list_has_quotes(data_source_check_cfg.include_tables):
                self.logs.error(
                    QUOTE_CHAR_ERROR_LOG.format(dataset_type="included"),
                    location=self.location,
                )
            if self.__check_str_list_has_quotes(data_source_check_cfg.exclude_tables):
                self.logs.error(
                    QUOTE_CHAR_ERROR_LOG.format(dataset_type="excluded"),
                    location=self.location,
                )
        else:
            self.logs.error(
                'Content of "datasets" must be a list of include and/or exclude expressions', location=self.location
            )

    @assert_header_content_is_dict
    def __parse_automated_monitoring_section(self, header_str, header_content):
        automated_monitoring_cfg = AutomatedMonitoringCfg(self.data_source_name, self.location)
        self.__parse_tables(header_content, automated_monitoring_cfg)
        self.get_data_source_scan_cfgs().add_data_source_cfg(automated_monitoring_cfg)

    @assert_header_content_is_dict
    def __parse_discover_tables_section(self, header_str, header_content):
        discover_dataset_cfg = DiscoverTablesCfg(self.data_source_name, self.location)
        self.__parse_tables(header_content, discover_dataset_cfg)
        self.get_data_source_scan_cfgs().add_data_source_cfg(discover_dataset_cfg)

    @assert_header_content_is_dict
    def __parse_profile_columns_section(self, header_str, header_content):
        profile_columns_cfg = ProfileColumnsCfg(self.data_source_name, self.location)
        data_source_scan_cfg = self.get_data_source_scan_cfgs()
        data_source_scan_cfg.add_data_source_cfg(profile_columns_cfg)

        columns = header_content.get("columns")
        if isinstance(columns, list):
            for column_expression in columns:
                if "." not in column_expression:
                    self.logs.error(
                        "Invalid column expression: {column_expression} - must be in the form of table.column".format(
                            column_expression=column_expression
                        ),
                        location=profile_columns_cfg.location,
                    )
                    continue

                if column_expression.startswith("exclude "):
                    exclude_column_expression = column_expression[len("exclude ") :]
                    profile_columns_cfg.exclude_columns.append(exclude_column_expression)
                else:
                    if column_expression.startswith("include "):
                        include_column_expression = column_expression[len("include ") :]
                    else:
                        include_column_expression = column_expression
                    profile_columns_cfg.include_columns.append(include_column_expression)
            if self.__check_str_list_has_quotes(profile_columns_cfg.include_columns):
                self.logs.error(
                    QUOTE_CHAR_ERROR_LOG.format(dataset_type="included"),
                    location=self.location,
                )
            if self.__check_str_list_has_quotes(profile_columns_cfg.exclude_columns):
                self.logs.error(
                    QUOTE_CHAR_ERROR_LOG.format(dataset_type="excluded"),
                    location=self.location,
                )
        elif columns is None:
            self.logs.error('Configuration key "columns" is required in profile columns', location=self.location)
        else:
            self.logs.error('Content of "columns" must be a list of column expressions', location=self.location)

    @assert_header_content_is_dict
    def __parse_sample_datasets_section(self, header_str, header_content):
        sample_tables_cfg = SampleTablesCfg(self.data_source_name, self.location)
        self.__parse_tables(header_content, sample_tables_cfg)
        self.get_data_source_scan_cfgs().add_data_source_cfg(sample_tables_cfg)

    def __parse_nameset_list(self, header_content, for_each_cfg):
        for name_filter_index, name_filter_str in enumerate(header_content):
            if isinstance(name_filter_str, str):
                is_include = True
                name_filter_pieces_str = name_filter_str
                if name_filter_pieces_str.startswith("include "):
                    name_filter_pieces_str = name_filter_pieces_str[len("include ") :]
                elif name_filter_pieces_str.startswith("exclude "):
                    name_filter_pieces_str = name_filter_pieces_str[len("exclude ") :]
                    is_include = False

                data_source_name_filter = None
                table_name_filter = None
                column_name_filter = None

                filter_pieces_list = re.split(r"\.", name_filter_pieces_str)
                if isinstance(for_each_cfg, ForEachDatasetCfg):
                    if len(filter_pieces_list) == 1:
                        data_source_name_filter = self.data_source_name
                        table_name_filter = filter_pieces_list[0]
                    elif len(filter_pieces_list) == 2:
                        data_source_name_filter = filter_pieces_list[0]
                        if data_source_name_filter != self.data_source_name:
                            self.logs.error(
                                f"Cross data source table sets not yet supported. {name_filter_pieces_str} "
                                f"refers to non default data source  {data_source_name_filter}. "
                                f"Default data source is {self.data_source_name}"
                            )
                            data_source_name_filter = None
                            table_name_filter = None
                        else:
                            table_name_filter = filter_pieces_list[1]
                    else:
                        self.logs.error(
                            f'Table name filter "{name_filter_str}" should exist out of 1 or 2 pieces separated by a dot.  Pieces count: {len(filter_pieces_list)}',
                            location=self.location,
                        )

                if isinstance(for_each_cfg, ForEachColumnCfg):
                    if len(filter_pieces_list) == 2:
                        data_source_name_filter = self.data_source_name
                        table_name_filter = filter_pieces_list[0]
                        column_name_filter = filter_pieces_list[1]
                    elif len(filter_pieces_list) == 3:
                        data_source_name_filter = filter_pieces_list[0]
                        if data_source_name_filter != self.data_source_name:
                            self.logs.error(
                                f"Cross data source column sets not yet supported. {name_filter_pieces_str} "
                                f"refers to non default data source  {data_source_name_filter}. "
                                f"Default data source is {self.data_source_name}"
                            )
                            data_source_name_filter = None
                            table_name_filter = None
                            column_name_filter = None
                        else:
                            table_name_filter = filter_pieces_list[1]
                            column_name_filter = filter_pieces_list[2]
                    else:
                        self.logs.error(
                            f'Column name filter "{name_filter_str}" should exist out of 2 or 3 pieces separated by a dot.  Pieces count: {len(filter_pieces_list)}',
                            location=self.location,
                        )

                if data_source_name_filter is not None:
                    name_filter = NameFilter(
                        data_source_name_filter=data_source_name_filter,
                        table_name_filter=table_name_filter,
                        partition_name_filter=None,
                        column_name_filter=column_name_filter,
                    )
                    if is_include:
                        for_each_cfg.includes.append(name_filter)

                    else:
                        for_each_cfg.excludes.append(name_filter)
            else:
                self.logs.error(
                    f'Name filter "{name_filter_str}" is not a string',
                    location=self.location,
                )
        if self.__check_str_list_has_quotes([x.table_name_filter for x in for_each_cfg.includes]):
            self.logs.error(
                QUOTE_CHAR_ERROR_LOG.format(dataset_type="included"),
                location=self.location,
            )
        if self.__check_str_list_has_quotes([x.table_name_filter for x in for_each_cfg.excludes]):
            self.logs.error(
                QUOTE_CHAR_ERROR_LOG.format(dataset_type="included"),
                location=self.location,
            )

    def __antlr_parse_identifier_name_from_header(self, antlr_header, identifier_index: int = 0):
        antlr_identifier = antlr_header.getTypedRuleContext(SodaCLAntlrParser.IdentifierContext, identifier_index)
        return self.__antlr_parse_identifier(antlr_identifier) if antlr_identifier else None

    def __antlr_parse_partition_from_header(self, antlr_header):
        if antlr_header.partition_name():
            return self.__antlr_parse_identifier(antlr_header.partition_name().identifier())

    def __antlr_parse_identifier(self, antlr_identifier) -> str:
        return self._resolve_jinja(antlr_identifier.getText(), self.sodacl_cfg.scan._variables)
        # TODO consider resolving escape chars from a quoted strings:
        # identifier = re.sub(r'\\(.)', '\g<1>', unquoted_identifier)

    def __parse_antlr_checks_for_each_dataset_section(
        self, antlr_checks_for_each_dataset_header, header_str, header_content
    ):
        for_each_dataset_cfg = ForEachDatasetCfg()
        for_each_dataset_cfg.table_alias_name = self.__antlr_parse_identifier_name_from_header(
            antlr_checks_for_each_dataset_header
        )
        datasets = self._get_optional("datasets", list)
        if datasets is None:
            datasets = self._get_optional("tables", list)
            self._push_path_element("tables", datasets)
        else:
            self._push_path_element("datasets", datasets)
        if datasets:
            # moved couple of lines above for backwards compatibility with tables
            # self._push_path_element("datasets", datasets)
            self.__parse_nameset_list(datasets, for_each_dataset_cfg)
            self._pop_path_element()
        check_cfgs = self._get_required("checks", list)
        if check_cfgs:
            self._push_path_element("checks", check_cfgs)
            for_each_dataset_cfg.check_cfgs = self.__parse_checks_in_for_each_section(header_str, check_cfgs)
            self._pop_path_element()
        for_each_dataset_cfg.location = self.location
        self.sodacl_cfg.for_each_dataset_cfgs.append(for_each_dataset_cfg)

    def __parse_antlr_checks_for_each_column_section(
        self, antlr_checks_for_each_column_header, header_str, header_content
    ):
        # TODO atm, It is not necessary that the column variable names in the metrics match.  So they are discarded for now.
        #  That's because *all* column references in the checks will be replaced anyways.
        for_each_column_cfg = ForEachColumnCfg()
        self.__parse_nameset_list(header_content, for_each_column_cfg)
        for_each_column_cfg.table_alias_name = self.__antlr_parse_identifier_name_from_header(
            antlr_checks_for_each_column_header
        )
        check_cfgs = self._get_required("checks", dict)
        for_each_column_cfg.check_cfgs = self.__parse_checks_in_for_each_section(header_str, check_cfgs)
        for_each_column_cfg.location = self.location
        self.sodacl_cfg.for_each_column_cfgs.append(for_each_column_cfg)

    def __parse_checks_in_for_each_section(self, header_str, header_content):
        check_cfgs: list[CheckCfg] = []
        if isinstance(header_content, list):
            for check_index, check_list_element in enumerate(header_content):
                self._push_path_element(check_index, check_list_element)

                check_str, check_configurations = self.__parse_check_configuration(check_list_element)

                if check_str is not None:
                    check_cfg = self.__parse_table_check_str(header_str, check_str, check_configurations)
                    check_cfgs.append(check_cfg)

                self._pop_path_element()
        else:
            self.logs.error(
                f'Skipping section "{header_str}" because content is not a list',
                location=self.location,
            )
        return check_cfgs

    def __parse_check_configuration(self, check_list_element) -> tuple:
        check_str: str = None
        check_configurations = None

        if isinstance(check_list_element, str):
            check_str = check_list_element
        elif isinstance(check_list_element, dict):
            check_str = next(iter(check_list_element))
            check_configurations = check_list_element[check_str]
            ignored_config_keys = [k for k in check_configurations if k != check_str]

            if len(check_list_element) > 1:
                self.logs.info(
                    f"Check '{check_str}' contains same-level configuration keys {ignored_config_keys} that will be ignored. Is your indentation correct?"
                )
        else:
            self.logs.error(
                f"Skipping unsupported check definition: {to_yaml_str(check_list_element)}",
                location=self.location,
            )

        return check_str, check_configurations

    def antlr_parse_check(self, text: str) -> AntlrParser:
        return AntlrParser(text, lambda p: p.check())

    def antlr_parse_section_header(self, text: str) -> AntlrParser:
        return AntlrParser(text, lambda p: p.section_header())

    def antlr_parse_column_configuration(self, text: str) -> AntlrParser:
        return AntlrParser(text, lambda p: p.configuration())

    def antlr_parse_threshold(self, text: str) -> AntlrParser:
        return AntlrParser(text, lambda p: p.threshold())

    def get_data_source_scan_cfgs(self):
        return self.sodacl_cfg.get_or_create_data_source_scan_cfgs(self.data_source_name)


class AntlrParser(ErrorListener):
    def __init__(self, text: str, parser_function):
        self.text = text
        self.error_message = None
        self.exception = None

        input_stream = InputStream(text)
        lexer = SodaCLAntlrLexer(input_stream)
        lexer.removeErrorListeners()
        lexer.addErrorListener(self)
        stream = CommonTokenStream(lexer)
        parser = SodaCLAntlrParser(stream)
        parser.removeErrorListeners()
        parser.addErrorListener(self)

        self.result = parser_function(parser)

    def is_ok(self):
        return self.result is not None and self.error_message is None and self.exception is None

    def get_error_message(self):
        return self.error_message

    def syntaxError(self, recognizer, offendingSymbol, line, column, msg, e):
        self.error_message = msg
        self.exception = e
