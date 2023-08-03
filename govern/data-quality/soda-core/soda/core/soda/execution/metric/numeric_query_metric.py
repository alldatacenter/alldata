from __future__ import annotations

from datetime import datetime
from decimal import Decimal
from importlib.util import find_spec
from numbers import Number

from soda.execution.metric.query_metric import QueryMetric
from soda.execution.query.sample_query import SampleQuery
from soda.sodacl.format_cfg import FormatHelper


class NumericQueryMetric(QueryMetric):
    def __init__(
        self,
        data_source_scan: DataSourceScan,
        partition: Partition | None,
        column: Column | None,
        metric_name: str,
        metric_args: list[object] | None,
        filter: str | None,
        aggregation: str | None,
        check_missing_and_valid_cfg: MissingAndValidCfg,
        column_configurations_cfg: ColumnConfigurationsCfg,
        check: Check,
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

        self.metric_args: list[object] | None = metric_args
        self.filter: str | None = filter
        self.aggregation: str | None = aggregation
        self.missing_and_valid_cfg: MissingAndValidCfg = merged_missing_and_valid_cfg

        # Implementation and other non-identity fields
        self.logs = data_source_scan.scan._logs
        self.column_name = column.column_name if column else None
        self.check = check

    def get_sql_aggregation_expression(self) -> str | None:
        data_source = self.data_source_scan.data_source

        """
        Returns an aggregation SQL expression for the given metric as a str or None if It is not an aggregation metric
        """
        if self.name in ["row_count", "missing_count", "valid_count", "invalid_count"]:
            # These are the conditional count metrics
            condition = None

            if "missing_count" == self.name:
                condition = self.build_missing_condition()

            if "valid_count" == self.name:
                condition = self.build_non_missing_and_valid_condition()

            if "invalid_count" == self.name:
                missing_condition = self.build_missing_condition()
                valid_condition = self.build_valid_condition()
                invalid_condition = self.build_invalid_condition()
                if valid_condition:
                    condition = f"NOT ({missing_condition}) AND NOT ({valid_condition})"
                elif invalid_condition:
                    condition = f"NOT ({missing_condition}) AND ({invalid_condition})"
                else:
                    self.logs.warning(
                        f'Counting invalid without valid or invalid specification does not make sense. ("{self.check.check_cfg.source_line}" @ {self.check.check_cfg.location})'
                    )
                    condition = self.data_source_scan.data_source.expr_false_condition()

            if self.filter:
                condition = f"({self.filter}) AND ({condition})" if condition else self.filter

            if condition:
                return data_source.expr_count_conditional(condition=condition)
            else:
                return data_source.expr_count_all()

        if self.aggregation:
            if self.is_missing_or_validity_configured():
                self.logs.info(
                    f"Missing and validity in {self.column.column_name} will not be applied to custom aggregation expression ({self.aggregation})"
                )
            if self.filter:
                self.logs.error(
                    f"Filter ({self.filter}) can't be applied in combination with a custom metric aggregation expression ({self.aggregation})"
                )
            return self.aggregation

        values_expression = self.column.column_name

        condition_clauses = []
        if self.is_missing_or_validity_configured():
            condition_clauses.append(self.build_non_missing_and_valid_condition())
        if self.filter:
            condition_clauses.append(self.filter)
        if condition_clauses:
            condition = " AND ".join(condition_clauses)
            values_expression = data_source.expr_conditional(condition=condition, expr=self.column_name)

        numeric_format = self.get_numeric_format()
        if numeric_format:
            values_expression = data_source.cast_text_to_number(values_expression, numeric_format)

        return data_source.get_metric_sql_aggregation_expression(self.name, self.metric_args, values_expression)

    def set_value(self, value):
        if value is None or isinstance(value, int):
            self.value = value
        elif self.name in [
            "row_count",
            "missing_count",
            "invalid_count",
            "valid_count",
            "duplicate_count",
        ]:
            self.value = int(value)
        elif isinstance(value, Decimal):
            self.value = float(value)
        # Check if numpy is installed
        elif find_spec("pandas") and find_spec("numpy"):
            # If pandas then numpy is installed already
            import numpy as np
            import pandas as pd

            if isinstance(value, np.datetime64):
                # Convert numpy datetime64 to python datetime
                self.value = datetime.utcfromtimestamp(value.tolist() / 1e9)
            elif isinstance(value, pd.Timestamp):
                self.value = value.to_pydatetime()
            elif isinstance(value, np.floating):
                self.value = float(value)
            elif isinstance(value, np.integer):
                self.value = int(value)
            else:
                self.value = value
        else:
            self.value = value

    def ensure_query(self):
        self.partition.ensure_query_for_metric(self)

    def build_missing_condition(self) -> str:
        from soda.execution.data_source import DataSource

        column_name = self.column_name
        data_source: DataSource = self.data_source_scan.data_source

        def append_missing(missing_and_valid_cfg):
            if missing_and_valid_cfg:
                if missing_and_valid_cfg.missing_values:
                    sql_literal_missing_values = data_source.literal_list(missing_and_valid_cfg.missing_values)
                    validity_clauses.append(f"{column_name} IN {sql_literal_missing_values}")

                missing_format = missing_and_valid_cfg.missing_format
                if missing_format:
                    missing_expression = self.data_source_scan.data_source.get_default_format_expression(
                        column_name, missing_format, missing_and_valid_cfg.missing_format_location
                    )
                    if missing_expression:
                        validity_clauses.append(missing_expression)

                if missing_and_valid_cfg.missing_regex:
                    missing_regex = data_source.escape_regex(missing_and_valid_cfg.missing_regex)
                    validity_clauses.append(data_source.expr_regexp_like(column_name, missing_regex))

        validity_clauses = [f"{column_name} IS NULL"]

        append_missing(self.missing_and_valid_cfg)

        return " OR ".join(validity_clauses)

    def build_valid_condition(self) -> str | None:
        column_name = self.column_name
        data_source = self.data_source_scan.data_source

        def append_valid(missing_and_valid_cfg):
            if missing_and_valid_cfg:
                if missing_and_valid_cfg.valid_values:
                    valid_values_sql = data_source.literal_list(missing_and_valid_cfg.valid_values)
                    in_expr = data_source.expr_in(column_name, valid_values_sql)
                    validity_clauses.append(in_expr)

                valid_format = missing_and_valid_cfg.valid_format
                if valid_format:
                    validity_expression = self.data_source_scan.data_source.get_default_format_expression(
                        column_name, valid_format, missing_and_valid_cfg.valid_format_location
                    )
                    if validity_expression:
                        validity_clauses.append(validity_expression)
                if missing_and_valid_cfg.valid_regex is not None:
                    valid_regex = data_source.escape_regex(missing_and_valid_cfg.valid_regex)
                    regex_like_expr = data_source.expr_regexp_like(column_name, valid_regex)
                    validity_clauses.append(regex_like_expr)
                if missing_and_valid_cfg.valid_length is not None:
                    length_expr = data_source.expr_length(column_name)
                    gte_expr = f"{length_expr} = {missing_and_valid_cfg.valid_length}"
                    validity_clauses.append(gte_expr)
                if missing_and_valid_cfg.valid_min_length is not None:
                    length_expr = data_source.expr_length(column_name)
                    gte_expr = f"{length_expr} >= {missing_and_valid_cfg.valid_min_length}"
                    validity_clauses.append(gte_expr)
                if missing_and_valid_cfg.valid_max_length is not None:
                    length_expr = data_source.expr_length(column_name)
                    lte_expr = f"{length_expr} <= {missing_and_valid_cfg.valid_max_length}"
                    validity_clauses.append(lte_expr)
                if missing_and_valid_cfg.valid_min is not None:
                    gte_expr = get_boundary_expr(missing_and_valid_cfg.valid_min, ">=")
                    validity_clauses.append(gte_expr)
                if missing_and_valid_cfg.valid_max is not None:
                    lte_expr = get_boundary_expr(missing_and_valid_cfg.valid_max, "<=")
                    validity_clauses.append(lte_expr)

        def get_boundary_expr(value, comparator: str):
            valid_format = self.missing_and_valid_cfg.valid_format if self.missing_and_valid_cfg else None
            if valid_format:
                cast_expr = data_source.cast_text_to_number(column_name, valid_format)
                return f"{cast_expr} {comparator} {value}"
            else:
                return f"{column_name} {comparator} {value}"

        validity_clauses = []

        append_valid(self.missing_and_valid_cfg)

        return " AND ".join(validity_clauses) if len(validity_clauses) != 0 else None

    def build_invalid_condition(self) -> str | None:
        column_name = self.column_name
        data_source = self.data_source_scan.data_source

        def append_invalid(missing_and_valid_cfg):
            if missing_and_valid_cfg:
                invalid_format = missing_and_valid_cfg.invalid_format
                if invalid_format:
                    invalidity_expression = self.data_source_scan.data_source.get_default_format_expression(
                        column_name, invalid_format, missing_and_valid_cfg.invalid_format_location
                    )
                    if invalidity_expression:
                        invalidity_clauses.append(invalidity_expression)

                if missing_and_valid_cfg.invalid_values:
                    invalid_values_sql = data_source.literal_list(missing_and_valid_cfg.invalid_values)
                    in_expr = data_source.expr_in(column_name, invalid_values_sql)
                    invalidity_clauses.append(in_expr)

                if missing_and_valid_cfg.invalid_regex is not None:
                    invalid_regex = data_source.escape_regex(missing_and_valid_cfg.invalid_regex)
                    regex_like_expr = data_source.expr_regexp_like(column_name, invalid_regex)
                    invalidity_clauses.append(regex_like_expr)

        invalidity_clauses = []

        append_invalid(self.missing_and_valid_cfg)

        return " OR ".join(invalidity_clauses) if len(invalidity_clauses) != 0 else None

    def build_non_missing_and_valid_condition(self):
        missing_condition = self.build_missing_condition()
        valid_condition = self.build_valid_condition()
        invalid_condition = self.build_invalid_condition()
        if valid_condition:
            return f"NOT ({missing_condition}) AND ({valid_condition})"
        elif invalid_condition:
            return f"NOT ({missing_condition}) AND NOT ({invalid_condition})"
        else:
            return f"NOT ({missing_condition})"

    def get_numeric_format(self) -> str | None:
        if self.missing_and_valid_cfg and FormatHelper.is_numeric(self.missing_and_valid_cfg.valid_format):
            return self.missing_and_valid_cfg.valid_format
        return None

    def is_missing_or_validity_configured(self) -> bool:
        return self.missing_and_valid_cfg is not None

    metric_names_with_failed_rows = ["missing_count", "invalid_count"]

    def create_failed_rows_sample_query(self) -> SampleQuery | None:
        sampler = self.data_source_scan.scan._configuration.sampler
        if (
            sampler
            and self.name in self.metric_names_with_failed_rows
            and isinstance(self.value, Number)
            and self.value > 0
        ):
            where_clauses = []
            passing_where_clauses = []
            partition_filter = self.partition.sql_partition_filter
            if partition_filter:
                resolved_filter = self.data_source_scan.scan.jinja_resolve(definition=partition_filter)
                where_clauses.append(resolved_filter)
                passing_where_clauses.append(resolved_filter)

            if self.name == "missing_count":
                where_clauses.append(self.build_missing_condition())
                passing_where_clauses.append(f"NOT ({self.build_missing_condition()})")
            elif self.name == "invalid_count":
                where_clauses.append(f"NOT ({self.build_missing_condition()})")
                passing_where_clauses.append(f"NOT ({self.build_missing_condition()})")

                valid_condition = self.build_valid_condition()
                if valid_condition:
                    where_clauses.append(f"NOT ({valid_condition})")
                    passing_where_clauses.append(valid_condition)

                invalid_condition = self.build_invalid_condition()
                if invalid_condition:
                    passing_where_clauses.append(f"NOT ({invalid_condition})")
                    where_clauses.append(invalid_condition)

            if self.filter:
                where_clauses.append(self.filter)
                passing_where_clauses.append(self.filter)

            where_sql = " AND ".join(where_clauses)
            passing_where_sql = " AND ".join(passing_where_clauses)

            sql = self.data_source_scan.data_source.sql_select_all(
                self.partition.table.table_name, self.samples_limit, where_sql
            )

            # Passing/failing queries are tied to existence of samples and sample query - this is ok for now as
            # whole Failed Rows Analysis is tied to existence of a failed rows sample file.
            if self.samples_limit > 0:
                sample_query = SampleQuery(self.data_source_scan, self, "failed_rows", sql)

                sample_query.passing_sql = self.data_source_scan.data_source.sql_select_all(
                    self.partition.table.table_name, filter=passing_where_sql
                )
                sample_query.failing_sql = self.data_source_scan.data_source.sql_select_all(
                    self.partition.table.table_name, filter=where_sql
                )

                return sample_query
